/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package biz.paluch.sgreadypi;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import biz.paluch.sgreadypi.measure.Percent;
import biz.paluch.sgreadypi.measure.Watt;
import biz.paluch.sgreadypi.output.SgReadyStateConsumer;
import biz.paluch.sgreadypi.provider.Statistics;
import biz.paluch.sgreadypi.provider.SunnyHomeManagerService;

import java.time.Clock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link SgReadyControlLoop}.
 *
 * @author Mark Paluch
 */
@MockitoSettings(strictness = Strictness.LENIENT)
class SgReadyControlLoopUnitTests {

	@Mock PowerGeneratorService inverters;

	@Mock SunnyHomeManagerService powerMeter;

	@Mock SgReadyStateConsumer stateConsumer;

	SgReadyProperties properties = new SgReadyProperties();

	SgReadyControlLoop controller;

	@BeforeEach
	void setUp() {

		properties.setHeatPumpPowerConsumption(Watt.of(100));
		properties.setWeather(new SgReadyProperties.Weather());

		controller = new SgReadyControlLoop(inverters, powerMeter, stateConsumer, properties, null,
				Clock.systemDefaultZone());

		when(inverters.hasData()).thenReturn(true);
		when(powerMeter.hasData()).thenReturn(true);
		when(inverters.getGeneratorPower()).thenReturn(Statistics.just(Watt.zero()));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Percent.zero());
		when(powerMeter.getIngress()).thenReturn(Statistics.just(Watt.zero()));
	}

	@Test
	void shouldDriveStateFromReadingsAndNotifyConsumer() {

		when(inverters.getGeneratorPower()).thenReturn(Statistics.just(Watt.of(100)));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Percent.of(80));

		controller.control();

		assertThat(controller.getState()).isEqualTo(SgReadyState.EXCESS_PV);
		verify(stateConsumer).onState(SgReadyState.EXCESS_PV);
	}

	@Test
	void shouldSkipIterationWhenNoData() {

		when(inverters.hasData()).thenReturn(false);

		controller.control();

		assertThat(controller.getState()).isEqualTo(SgReadyState.NORMAL);
		verifyNoInteractions(stateConsumer);
	}

	@Test // ADR-0005
	void shouldFallBackToNormalWhenOutOfService() {

		when(inverters.getGeneratorPower()).thenReturn(Statistics.just(Watt.of(100)));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Percent.of(80));
		when(powerMeter.isOutOfService()).thenReturn(true);

		controller.control();

		assertThat(controller.getState()).isEqualTo(SgReadyState.NORMAL);
		verify(stateConsumer).onState(SgReadyState.NORMAL);
	}

	@Test
	void shouldFeedCurrentStateBackIntoPolicy() {

		when(inverters.getGeneratorPower()).thenReturn(Statistics.just(Watt.of(100)));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Percent.of(80));

		controller.control();
		assertThat(controller.getState()).isEqualTo(SgReadyState.EXCESS_PV);

		// drop into the hysteresis band: EXCESS_PV is retained because the prior state is fed back in
		when(inverters.getBatteryStateOfCharge()).thenReturn(Percent.of(60));

		controller.control();
		assertThat(controller.getState()).isEqualTo(SgReadyState.EXCESS_PV);
	}

}

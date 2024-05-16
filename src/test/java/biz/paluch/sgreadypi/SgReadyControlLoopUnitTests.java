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
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

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

	SgReadyProperties properties = new SgReadyProperties();

	SgReadyControlLoop controller;

	@BeforeEach
	void setUp() {

		properties.setHeatPumpPowerConsumption(Watt.of(100));
		properties.setWeather(new SgReadyProperties.Weather());

		controller = new SgReadyControlLoop(inverters, powerMeter, mock(SgReadyStateConsumer.class), properties, null,
				Clock.systemDefaultZone());
		when(inverters.hasData()).thenReturn(true);
		when(powerMeter.hasData()).thenReturn(true);
	}

	@Test
	void shouldLeaveStateNormalOnNoPower() {

		when(inverters.getGeneratorPower()).thenReturn(Statistics.just(Watt.zero()));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Percent.zero());
		when(powerMeter.getIngress()).thenReturn(Statistics.just(Watt.zero()));

		assertThat(controller.createState()).isEqualTo(SgReadyState.NORMAL);
	}

	@Test
	void shouldEnableHeatPumpNormalOnAvailablePower() {

		when(inverters.getGeneratorPower()).thenReturn(Statistics.just(Watt.of(100)));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Percent.of(20));
		when(powerMeter.getIngress()).thenReturn(Statistics.just(Watt.zero()));

		assertThat(controller.createState()).isEqualTo(SgReadyState.AVAILABLE_PV);
	}

	@Test
	void shouldTurnOnHeatPumpOnExcessPower() {

		when(inverters.getGeneratorPower()).thenReturn(Statistics.just(Watt.of(100)));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Percent.of(80));
		when(powerMeter.getIngress()).thenReturn(Statistics.just(Watt.zero()));

		assertThat(controller.createState()).isEqualTo(SgReadyState.EXCESS_PV);
	}

	@Test
	void shouldRetainHeatPumpForcedOnExcessPower() {

		when(inverters.getGeneratorPower()).thenReturn(Statistics.just(Watt.of(100)));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Percent.of(80));
		when(powerMeter.getIngress()).thenReturn(Statistics.just(Watt.zero()));

		controller.control();
		assertThat(controller.getState()).isEqualTo(SgReadyState.EXCESS_PV);

		when(inverters.getBatteryStateOfCharge()).thenReturn(Quantities.getQuantity(60, Units.PERCENT));
		assertThat(controller.createState()).isEqualTo(SgReadyState.EXCESS_PV);
	}

	@Test
	void shouldDowngradeToAvailablePowerFromExcess() {

		when(inverters.getGeneratorPower()).thenReturn(Statistics.just(Watt.of(100)));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Percent.of(80));
		when(powerMeter.getIngress()).thenReturn(Statistics.just(Watt.zero()));

		controller.control();
		assertThat(controller.getState()).isEqualTo(SgReadyState.EXCESS_PV);

		when(inverters.getBatteryStateOfCharge()).thenReturn(Percent.of(50));
		assertThat(controller.createState()).isEqualTo(SgReadyState.AVAILABLE_PV);
	}

	@Test
	void shouldLimitExcessToTimeNotBefore() {

		LocalDateTime ldt = LocalDateTime.parse("2007-12-03T10:15:30");

		Clock fixed = Clock.fixed(ldt.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

		controller = new SgReadyControlLoop(inverters, powerMeter, mock(SgReadyStateConsumer.class), properties, null,
				fixed);

		when(inverters.getGeneratorPower()).thenReturn(Statistics.just(Watt.of(100)));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Percent.of(80));
		when(powerMeter.getIngress()).thenReturn(Statistics.just(Watt.zero()));

		properties.setExcessNotBefore(LocalTime.of(14, 0));
		controller.control();
		assertThat(controller.getState()).isEqualTo(SgReadyState.AVAILABLE_PV);

		properties.setExcessNotBefore(LocalTime.of(10, 0));
		controller.control();
		assertThat(controller.getState()).isEqualTo(SgReadyState.EXCESS_PV);

		properties.setExcessNotBefore(LocalTime.of(14, 0));

		controller.control();
		assertThat(controller.getState()).isEqualTo(SgReadyState.AVAILABLE_PV);

		properties.setExcessNotBefore(null);

		controller.control();
		assertThat(controller.getState()).isEqualTo(SgReadyState.EXCESS_PV);
	}

	@Test
	void shouldLimitExcessToTimeNotAfter() {

		LocalDateTime ldt = LocalDateTime.parse("2007-12-03T10:15:30");

		Clock fixed = Clock.fixed(ldt.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

		controller = new SgReadyControlLoop(inverters, powerMeter, mock(SgReadyStateConsumer.class), properties, null,
				fixed);

		when(inverters.getGeneratorPower()).thenReturn(Statistics.just(Watt.of(100)));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Percent.of(80));
		when(powerMeter.getIngress()).thenReturn(Statistics.just(Watt.zero()));

		properties.setExcessNotAfter(LocalTime.of(14, 0));
		controller.control();
		assertThat(controller.getState()).isEqualTo(SgReadyState.EXCESS_PV);

		properties.setExcessNotAfter(LocalTime.of(9, 40));
		controller.control();
		assertThat(controller.getState()).isEqualTo(SgReadyState.AVAILABLE_PV);

		properties.setExcessNotBefore(LocalTime.of(9, 0));
		properties.setExcessNotAfter(LocalTime.of(11, 0));

		controller.control();
		assertThat(controller.getState()).isEqualTo(SgReadyState.EXCESS_PV);
	}

	@Test
	void shouldDowngradeToNormalOnLowerPowerGeneration() {

		when(inverters.getGeneratorPower()).thenReturn(Statistics.just(Watt.of(100)));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Percent.of(80));
		when(powerMeter.getIngress()).thenReturn(Statistics.just(Watt.zero()));

		controller.control();
		assertThat(controller.getState()).isEqualTo(SgReadyState.EXCESS_PV);

		when(inverters.getGeneratorPower()).thenReturn(Statistics.just(Watt.of(10)));
		assertThat(controller.createState()).isEqualTo(SgReadyState.NORMAL);
	}

	@Test
	void shouldEnableHeatPumpHeatBelowExcessPower() {

		properties.setHeatPumpPowerConsumption(Watt.of(100));
		properties.setBattery(new SgReadyProperties.Levels(Percent.of(10), Percent.of(80), Percent.of(70)));

		when(inverters.getGeneratorPower()).thenReturn(Statistics.just(Watt.of(100)));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Percent.of(10));
		when(powerMeter.getIngress()).thenReturn(Statistics.just(Watt.zero()));

		assertThat(controller.createState()).isEqualTo(SgReadyState.AVAILABLE_PV);
	}

}

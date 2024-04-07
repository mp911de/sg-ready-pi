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

import biz.paluch.sgreadypi.output.SgReadyStateConsumer;
import biz.paluch.sgreadypi.provider.Statistics;
import biz.paluch.sgreadypi.provider.SunnyHomeManagerService;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

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

		properties.setHeatPumpPowerConsumption(Quantities.getQuantity(100, Units.WATT));

		controller = new SgReadyControlLoop(inverters, powerMeter, mock(SgReadyStateConsumer.class), properties);
		when(inverters.hasData()).thenReturn(true);
		when(powerMeter.hasData()).thenReturn(true);
	}

	@Test
	void shouldLeaveStateNormalOnNoPower() {

		when(inverters.getGeneratorPower()).thenReturn(Statistics.ofValue(Quantities.getQuantity(0, Units.WATT)));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Quantities.getQuantity(0, Units.PERCENT));
		when(powerMeter.getIngress()).thenReturn(Statistics.ofValue(Quantities.getQuantity(0, Units.WATT)));

		assertThat(controller.createState()).isEqualTo(SgReadyState.NORMAL);
	}

	@Test
	void shouldEnableHeatPumpNormalOnAvailablePower() {

		when(inverters.getGeneratorPower()).thenReturn(Statistics.ofValue(Quantities.getQuantity(100, Units.WATT)));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Quantities.getQuantity(20, Units.PERCENT));
		when(powerMeter.getIngress()).thenReturn(Statistics.ofValue(Quantities.getQuantity(0, Units.WATT)));

		assertThat(controller.createState()).isEqualTo(SgReadyState.AVAILABLE_PV);
	}

	@Test
	void shouldTurnOnHeatPumpOnExcessPower() {

		when(inverters.getGeneratorPower()).thenReturn(Statistics.ofValue(Quantities.getQuantity(100, Units.WATT)));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Quantities.getQuantity(80, Units.PERCENT));
		when(powerMeter.getIngress()).thenReturn(Statistics.ofValue(Quantities.getQuantity(0, Units.WATT)));

		assertThat(controller.createState()).isEqualTo(SgReadyState.EXCESS_PV);
	}

	@Test
	void shouldRetainHeatPumpForcedOnExcessPower() {

		when(inverters.getGeneratorPower()).thenReturn(Statistics.ofValue(Quantities.getQuantity(100, Units.WATT)));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Quantities.getQuantity(80, Units.PERCENT));
		when(powerMeter.getIngress()).thenReturn(Statistics.ofValue(Quantities.getQuantity(0, Units.WATT)));

		controller.control();
		assertThat(controller.getState()).isEqualTo(SgReadyState.EXCESS_PV);

		when(inverters.getBatteryStateOfCharge()).thenReturn(Quantities.getQuantity(60, Units.PERCENT));
		assertThat(controller.createState()).isEqualTo(SgReadyState.EXCESS_PV);
	}

	@Test
	void shouldDowngradeToAvailablePowerFromExcess() {

		when(inverters.getGeneratorPower()).thenReturn(Statistics.ofValue(Quantities.getQuantity(100, Units.WATT)));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Quantities.getQuantity(80, Units.PERCENT));
		when(powerMeter.getIngress()).thenReturn(Statistics.ofValue(Quantities.getQuantity(0, Units.WATT)));

		controller.control();
		assertThat(controller.getState()).isEqualTo(SgReadyState.EXCESS_PV);

		when(inverters.getBatteryStateOfCharge()).thenReturn(Quantities.getQuantity(50, Units.PERCENT));
		assertThat(controller.createState()).isEqualTo(SgReadyState.AVAILABLE_PV);
	}

	@Test
	void shouldDowngradeToNormalOnLowerPowerGeneration() {

		when(inverters.getGeneratorPower()).thenReturn(Statistics.ofValue(Quantities.getQuantity(100, Units.WATT)));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Quantities.getQuantity(80, Units.PERCENT));
		when(powerMeter.getIngress()).thenReturn(Statistics.ofValue(Quantities.getQuantity(0, Units.WATT)));

		controller.control();
		assertThat(controller.getState()).isEqualTo(SgReadyState.EXCESS_PV);

		when(inverters.getGeneratorPower()).thenReturn(Statistics.ofValue(Quantities.getQuantity(10, Units.WATT)));
		assertThat(controller.createState()).isEqualTo(SgReadyState.NORMAL);
	}

	@Test
	void shouldEnableHeatPumpHeatPumpBelowExcessPower() {

		properties.setHeatPumpPowerConsumption(Quantities.getQuantity(100, Units.WATT));

		when(inverters.getGeneratorPower()).thenReturn(Statistics.ofValue(Quantities.getQuantity(100, Units.WATT)));
		when(inverters.getBatteryStateOfCharge()).thenReturn(Quantities.getQuantity(60, Units.PERCENT));
		when(powerMeter.getIngress()).thenReturn(Statistics.ofValue(Quantities.getQuantity(0, Units.WATT)));

		assertThat(controller.createState()).isEqualTo(SgReadyState.AVAILABLE_PV);
	}

}

/*
 * Copyright 2026 the original author or authors.
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
package biz.paluch.sgreadypi.provider;

import static org.assertj.core.api.Assertions.*;

import biz.paluch.sgreadypi.SgReadyProperties;
import biz.paluch.sgreadypi.measure.Watt;
import tech.units.indriya.unit.Units;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import javax.measure.quantity.Power;

import org.junit.jupiter.api.Test;

import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link SmaPowerGeneratorService}.
 *
 * @author Mark Paluch
 */
class SmaPowerGeneratorServiceUnitTests {

	@Test
	@SuppressWarnings("unchecked")
	void shouldAverageBatteryStateOfChargeAcrossBatteryBackedInverters() {

		SmaPowerGeneratorService service = new SmaPowerGeneratorService(properties(), new ConcurrentTaskScheduler());
		Map<String, SmaPowerGeneratorService.InverterState> stateMap = (Map<String, SmaPowerGeneratorService.InverterState>) ReflectionTestUtils
				.getField(service, "stateMap");

		Instant now = Instant.parse("2026-05-24T00:00:00Z");
		stateMap.put("a", new SmaPowerGeneratorService.InverterState(0, true, 0, 0, 50, now));
		stateMap.put("b", new SmaPowerGeneratorService.InverterState(0, true, 0, 0, 70, now));
		stateMap.put("c", new SmaPowerGeneratorService.InverterState(0, false, 0, 0, 100, now));

		assertThat(service.getBatteryStateOfCharge().to(Units.PERCENT).getValue().doubleValue()).isEqualTo(60);
	}

	@Test // ADR-0004
	void shouldDeriveSolarPowerAndBatteryDischargeFromReadings() {

		Instant now = Instant.parse("2026-05-24T00:00:00Z");
		SmaPowerGeneratorService.InverterState discharging = new SmaPowerGeneratorService.InverterState(1000, true, 200,
				500, 50, now);
		SmaPowerGeneratorService.InverterState charging = new SmaPowerGeneratorService.InverterState(1000, true, 500, 0, 50,
				now);

		assertThat(discharging.getSolarPower().getValue().intValue()).isEqualTo(700);
		assertThat(discharging.getBatteryDischarge().getValue().intValue()).isEqualTo(300);

		assertThat(charging.getSolarPower().getValue().intValue()).isEqualTo(1500);
		assertThat(charging.getBatteryDischarge().getValue().intValue()).isEqualTo(-500);
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldSumBatteryDischargeAcrossInverters() {

		SmaPowerGeneratorService service = new SmaPowerGeneratorService(properties(), new ConcurrentTaskScheduler());
		Map<String, MutableStatistics<Power>> dischargeStats = (Map<String, MutableStatistics<Power>>) ReflectionTestUtils
				.getField(service, "dischargeStats");

		dischargeStats.put("a", statistics(300));
		dischargeStats.put("b", statistics(-100));

		assertThat(service.getBatteryDischarge().getMostRecent().getValue().intValue()).isEqualTo(200);
	}

	private static MutableStatistics<Power> statistics(int watt) {

		MutableStatistics<Power> statistics = MutableStatistics.create(Duration.ofMinutes(5), Units.WATT);
		statistics.update(Watt.of(watt));
		return statistics;
	}

	private static SgReadyProperties properties() {

		SgReadyProperties properties = new SgReadyProperties();
		properties.setInverterHosts(List.of());
		return properties;
	}

}

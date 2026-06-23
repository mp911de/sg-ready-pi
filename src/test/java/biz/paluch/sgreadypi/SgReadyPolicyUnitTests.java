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
package biz.paluch.sgreadypi;

import static org.assertj.core.api.Assertions.*;

import biz.paluch.sgreadypi.measure.Percent;
import biz.paluch.sgreadypi.measure.Watt;
import biz.paluch.sgreadypi.weather.WeatherService;
import tech.units.indriya.AbstractUnit;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.measure.MetricPrefix;
import javax.measure.Quantity;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Power;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SgReadyPolicy}.
 *
 * @author Mark Paluch
 */
class SgReadyPolicyUnitTests {

	SgReadyProperties properties = new SgReadyProperties();

	LocalDateTime now = LocalDateTime.parse("2007-12-03T10:15:30");

	SgReadyPolicy policy = new SgReadyPolicy(properties);

	@BeforeEach
	void setUp() {
		properties.setHeatPumpPowerConsumption(Watt.of(100));
		properties.setHeatElementPowerConsumption(Watt.of(100));
		properties.setWeather(new SgReadyProperties.Weather());
	}

	@Test
	void shouldLeaveStateNormalOnNoPower() {

		Decision decision = decide(SgReadyState.NORMAL, conditions(0, 0, 0));

		assertThat(decision.state()).isEqualTo(SgReadyState.NORMAL);
	}

	@Test
	void shouldRecommendAvailablePvAboveAvailableSoC() {

		Decision decision = decide(SgReadyState.NORMAL, powered(20));

		assertThat(decision.state()).isEqualTo(SgReadyState.AVAILABLE_PV);
	}

	@Test
	void shouldStayNormalBelowAvailableSoC() {

		Decision decision = decide(SgReadyState.NORMAL, powered(19));

		assertThat(decision.state()).isEqualTo(SgReadyState.NORMAL);
	}

	@Test
	void shouldForceExcessPvAboveExcessOnSoC() {

		Decision decision = decide(SgReadyState.NORMAL, powered(80));

		assertThat(decision.state()).isEqualTo(SgReadyState.EXCESS_PV);
	}

	@Test
	void shouldRetainExcessPvWithinHysteresis() {

		Decision decision = decide(SgReadyState.EXCESS_PV, powered(60));

		assertThat(decision.state()).isEqualTo(SgReadyState.EXCESS_PV);
	}

	@Test
	void shouldDowngradeFromExcessToAvailable() {

		Decision decision = decide(SgReadyState.EXCESS_PV, powered(50));

		assertThat(decision.state()).isEqualTo(SgReadyState.AVAILABLE_PV);
	}

	@Test
	void shouldStayNormalBelowHeatPumpConsumption() {

		Decision decision = decide(SgReadyState.EXCESS_PV, conditions(0, 10, 80));

		assertThat(decision.state()).isEqualTo(SgReadyState.NORMAL);
	}

	@Test
	void shouldForceNormalWhenIngressExceedsLimit() {

		Decision decision = decide(SgReadyState.EXCESS_PV, conditions(500, 100, 80));

		assertThat(decision.state()).isEqualTo(SgReadyState.NORMAL);
	}

	@Test
	void shouldEnableHeatPumpHeatBelowExcessPower() {

		properties.setBattery(new SgReadyProperties.Levels(Percent.of(10), Percent.of(80), Percent.of(70)));

		Decision decision = decide(SgReadyState.NORMAL, powered(10));

		assertThat(decision.state()).isEqualTo(SgReadyState.AVAILABLE_PV);
	}

	@Test
	void shouldWithholdExcessBeforeNotBeforeTime() {

		properties.setExcessNotBefore(LocalTime.of(14, 0));

		Decision decision = decide(SgReadyState.NORMAL, powered(80));

		assertThat(decision.state()).isEqualTo(SgReadyState.AVAILABLE_PV);
	}

	@Test
	void shouldAllowExcessAfterNotBeforeTime() {

		properties.setExcessNotBefore(LocalTime.of(10, 0));

		Decision decision = decide(SgReadyState.NORMAL, powered(80));

		assertThat(decision.state()).isEqualTo(SgReadyState.EXCESS_PV);
	}

	@Test
	void shouldWithholdExcessAfterNotAfterTime() {

		properties.setExcessNotAfter(LocalTime.of(9, 40));

		Decision decision = decide(SgReadyState.NORMAL, powered(80));

		assertThat(decision.state()).isEqualTo(SgReadyState.AVAILABLE_PV);
	}

	@Test
	void shouldAllowExcessBeforeNotAfterTime() {

		properties.setExcessNotAfter(LocalTime.of(14, 0));

		Decision decision = decide(SgReadyState.NORMAL, powered(80));

		assertThat(decision.state()).isEqualTo(SgReadyState.EXCESS_PV);
	}

	@Test
	void shouldExplainExcessTimeGates() {

		properties.setExcessNotBefore(LocalTime.of(10, 0));
		properties.setExcessNotAfter(LocalTime.of(14, 0));

		Decision decision = decide(SgReadyState.NORMAL, powered(80));

		assertThat(decision.conditionOutcome().explain()).anySatisfy((entry) -> assertThat(entry).contains("not-before"))
				.anySatisfy((entry) -> assertThat(entry).contains("not-after"))
				.anySatisfy((entry) -> assertThat(entry).contains("excess PV stop threshold"));
	}

	@Test // ADR-0005
	void shouldFallBackToNormalWhenOutOfService() {

		Conditions outOfService = new Conditions(Watt.zero(), Watt.of(100), Percent.of(80), true);

		Decision decision = decide(SgReadyState.EXCESS_PV, outOfService);

		assertThat(decision.state()).isEqualTo(SgReadyState.NORMAL);
		assertThat(decision.conditionOutcome().getMessage()).isEqualTo("Inverters or power meter out of service");
	}

	@Test // ADR-0003
	void shouldDeferExcessWhenEnoughSunRemains() {

		Decision decision = policy.decide(SgReadyState.NORMAL, powered(80), range(true, false, false), now);

		assertThat(decision.state()).isEqualTo(SgReadyState.AVAILABLE_PV);
	}

	@Test // ADR-0003
	void shouldStartExcessWhenSunRunningOut() {

		Decision decision = policy.decide(SgReadyState.NORMAL, powered(80), range(false, false, false), now);

		assertThat(decision.state()).isEqualTo(SgReadyState.EXCESS_PV);
	}

	@Test // ADR-0003
	void shouldWithholdExcessAfterSunset() {

		Decision decision = policy.decide(SgReadyState.NORMAL, powered(80), range(false, true, true), now);

		assertThat(decision.state()).isEqualTo(SgReadyState.AVAILABLE_PV);
	}

	@Test
	void shouldComparePowerQuantitiesInCommonUnits() {

		Quantity<Power> twoKw = Quantities.getQuantity(2, MetricPrefix.KILO(Units.WATT));

		assertThat(SgReadyPolicy.gte(Watt.of(1000), twoKw)).isFalse();
		assertThat(SgReadyPolicy.gte(Watt.of(2000), twoKw)).isTrue();
	}

	@Test
	void shouldCompareDimensionlessQuantitiesInCommonUnits() {

		Quantity<Dimensionless> eightyPercent = Quantities.getQuantity(0.8, AbstractUnit.ONE);
		Quantity<Dimensionless> seventyNinePercent = Quantities.getQuantity(0.79, AbstractUnit.ONE);

		assertThat(SgReadyPolicy.gte(eightyPercent, Percent.of(80))).isTrue();
		assertThat(SgReadyPolicy.gte(seventyNinePercent, Percent.of(80))).isFalse();
	}

	@Test
	void shouldStayOnCompressorWhenGeneratorBelowHeatElement() {

		properties.setHeatPumpPowerConsumption(Watt.of(1000));
		properties.setHeatElementPowerConsumption(Watt.of(4000));

		Decision decision = decide(SgReadyState.NORMAL, conditions(0, 2000, 80));

		assertThat(decision.state()).isEqualTo(SgReadyState.AVAILABLE_PV);
	}

	@Test
	void shouldForceExcessWhenGeneratorCoversHeatElement() {

		properties.setHeatPumpPowerConsumption(Watt.of(1000));
		properties.setHeatElementPowerConsumption(Watt.of(4000));

		Decision decision = decide(SgReadyState.NORMAL, conditions(0, 5000, 80));

		assertThat(decision.state()).isEqualTo(SgReadyState.EXCESS_PV);
	}

	@Test
	void shouldDowngradeExcessWhenGeneratorDropsBelowHeatElement() {

		properties.setHeatPumpPowerConsumption(Watt.of(1000));
		properties.setHeatElementPowerConsumption(Watt.of(4000));

		Decision decision = decide(SgReadyState.EXCESS_PV, conditions(0, 2000, 70));

		assertThat(decision.state()).isEqualTo(SgReadyState.AVAILABLE_PV);
	}

	@Test
	void shouldApplyGeneratorPowerHysteresis() {

		properties.setHeatPumpPowerConsumption(Watt.of(1000));

		// 800 W is below the 1000 W on-threshold but above the 700 W off-threshold
		assertThat(decide(SgReadyState.AVAILABLE_PV, conditions(0, 800, 30)).state()).isEqualTo(SgReadyState.AVAILABLE_PV);
		assertThat(decide(SgReadyState.NORMAL, conditions(0, 800, 30)).state()).isEqualTo(SgReadyState.NORMAL);
	}

	@Test
	void shouldApplyAvailableStateOfChargeHysteresis() {

		// SoC 17 % is below the 20 % on-threshold but above the 15 % off-threshold
		assertThat(decide(SgReadyState.AVAILABLE_PV, conditions(0, 100, 17)).state()).isEqualTo(SgReadyState.AVAILABLE_PV);
		assertThat(decide(SgReadyState.NORMAL, conditions(0, 100, 17)).state()).isEqualTo(SgReadyState.NORMAL);
	}

	@Test
	void shouldUseConfiguredHysteresisProperties() {

		properties.setHeatPumpPowerConsumption(Watt.of(1000));
		properties.setGeneratorPowerOffRatio(0.5); // off-threshold becomes 500 W instead of the 700 W default

		assertThat(decide(SgReadyState.AVAILABLE_PV, conditions(0, 600, 30)).state()).isEqualTo(SgReadyState.AVAILABLE_PV);
		assertThat(decide(SgReadyState.AVAILABLE_PV, conditions(0, 400, 30)).state()).isEqualTo(SgReadyState.NORMAL);
	}

	private Decision decide(SgReadyState currentState, Conditions conditions) {
		return policy.decide(currentState, conditions, null, now);
	}

	private static Conditions conditions(int ingressWatt, int generatorWatt, int soc) {
		return new Conditions(Watt.of(ingressWatt), Watt.of(generatorWatt), Percent.of(soc), false);
	}

	private static Conditions powered(int soc) {
		return conditions(0, 100, soc);
	}

	private static WeatherService.Range range(boolean enoughSun, boolean afterSunset, boolean afterSunsetLimit) {

		LocalDateTime base = LocalDateTime.parse("2007-12-03T16:00:00");
		return new WeatherService.Range(base, base.plusHours(1), base.plusHours(2), afterSunset, afterSunsetLimit,
				enoughSun, Duration.ofHours(2));
	}
}

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

import biz.paluch.sgreadypi.weather.WeatherService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import javax.measure.Quantity;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Power;

import org.jspecify.annotations.Nullable;

/**
 * SG Ready policy function.
 * <p>
 * {@code SgReadyPolicy} turns the current {@link Conditions}, the previously signalled {@link SgReadyState}, optional
 * weather timing, and {@link SgReadyProperties} into a {@link Decision}. It contains the rule ordering for conservative
 * fallback, generator-power gating, battery state-of-charge thresholds, hysteresis, local time gates, and weather
 * deferral. It performs no I/O and does not apply debounce; callers such as {@link SgReadyControlLoop} are responsible
 * for obtaining readings, resolving a {@link WeatherService.Range}, and applying the resulting state.
 * <p>
 * The policy evaluates safety gates first. Out-of-service input data or ingress above the configured limit returns
 * {@link SgReadyState#NORMAL}. Only when generator power is at least the configured heat pump power consumption does
 * the policy consider {@link SgReadyState#AVAILABLE_PV} or {@link SgReadyState#EXCESS_PV}.
 *
 * <pre class="code">
 * Decision decision = policy.decide(currentState, conditions, weatherRange, now);
 * </pre>
 *
 * @author Mark Paluch
 * @see SgReadyControlLoop
 * @see Conditions
 * @see Decision
 * @see SgReadyProperties.Levels
 */
public class SgReadyPolicy {

	private static final DateTimeFormatter DURATION = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.HOUR_OF_DAY, 2) //
			.appendLiteral(':') //
			.appendValue(ChronoField.MINUTE_OF_HOUR, 2) //
			.appendLiteral(':') //
			.appendValue(ChronoField.SECOND_OF_MINUTE, 2) //
			.toFormatter();

	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("HH:mm (yyyy-MM-dd)");

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

	private final SgReadyProperties properties;

	public SgReadyPolicy(SgReadyProperties properties) {
		this.properties = properties;
	}

	/**
	 * Decide the {@link SgReadyState} for the given control-loop snapshot.
	 * <p>
	 * The method is deterministic for the supplied arguments. It does not read the system clock, query services, or
	 * retain state between calls. Quantity values are expected to use compatible units supplied by the application
	 * providers and configuration.
	 * <p>
	 * A non-{@literal null} {@code weatherRange} can defer or withhold {@link SgReadyState#EXCESS_PV} when enough sunny
	 * time remains, after sunset, or after the sunset limit. It does not enable a PV state on its own; generator power,
	 * ingress, state of charge, and configured time gates still decide the final state.
	 *
	 * @param currentState the state currently signalled; must not be {@literal null}. Used to retain the current state
	 *          while battery state of charge remains within the excess-PV hysteresis band.
	 * @param conditions the sensed input snapshot; must not be {@literal null}.
	 * @param weatherRange the usable time range derived from the weather forecast, or {@literal null} when weather
	 *          optimisation is disabled or unavailable.
	 * @param currentTime the current local date-time used for configured time gates and reasoning messages; must not be
	 *          {@literal null}.
	 * @return the selected state together with the reasoning trail; guaranteed to be not {@literal null}.
	 */
	public Decision decide(SgReadyState currentState, Conditions conditions, WeatherService.@Nullable Range weatherRange,
			LocalDateTime currentTime) {

		if (conditions.outOfService()) {
			return Decision.normal(ConditionOutcome.noMatch("Inverters or power meter out of service"));
		}

		Quantity<Power> ingress = conditions.ingress();
		Quantity<Power> generatorPower = conditions.generatorPower();
		Quantity<Dimensionless> soc = conditions.soc();

		if (gte(ingress, properties.getIngressLimit())) {
			return Decision.normal(
					ConditionOutcome.match("Ingress %s exceeds limit %s".formatted(ingress, properties.getIngressLimit())));
		}

		boolean consuming = currentState != SgReadyState.NORMAL;
		Quantity<Power> generatorOn = properties.getHeatPumpPowerConsumption();
		Quantity<Power> generatorOff = generatorOn.multiply(properties.getGeneratorPowerOffRatio());

		if (Hysteresis.active(consuming, generatorPower, generatorOn, generatorOff)) {

			ConditionOutcome match = ConditionOutcome
					.match("Generator power %s above heat pump consumption %s (hysteresis off %s)".formatted(generatorPower,
							generatorOn, generatorOff));

			SgReadyProperties.Levels battery = properties.getBattery();
			ConditionOutcome qualifiesForExcessPower = match.nested(qualifiesForExcessPower(battery,
					properties.getExcessNotBefore(), properties.getExcessNotAfter(), soc, currentTime));
			boolean excess = qualifiesForExcessPower.isMatch();
			boolean weather = true;

			if (weatherRange != null) {

				String sunset = TIME.format(weatherRange.sunset());
				if (weatherRange.enoughRemainingSunHours()) {
					weather = false;
					qualifiesForExcessPower = qualifiesForExcessPower
							.nestedNoMatch("Enough remaining sunny time %s (Sunset: %s), starting at %s until %s".formatted(
									format(weatherRange.remainingSunDuration()), sunset, TIMESTAMP.format(weatherRange.from()),
									TIMESTAMP.format(weatherRange.to())));
				} else {

					if (weatherRange.afterSunset()) {
						weather = false;
						qualifiesForExcessPower = qualifiesForExcessPower
								.nestedNoMatch("After sunset (Sunset: %s)".formatted(sunset));
					} else if (weatherRange.afterSunsetLimit()) {
						weather = false;
						qualifiesForExcessPower = qualifiesForExcessPower
								.nestedNoMatch("After sunset limit (Sunset: %s)".formatted(sunset));
					} else {
						qualifiesForExcessPower = qualifiesForExcessPower.nestedMatch(
								String.format("Using remaining %s sunny time", format(weatherRange.remainingSunDuration())));
					}
				}
			}

			Quantity<Power> elementOn = properties.getHeatElementPowerConsumption();
			Quantity<Power> elementOff = elementOn.multiply(properties.getGeneratorPowerOffRatio());
			boolean canRunElement = Hysteresis.active(currentState == SgReadyState.EXCESS_PV, generatorPower, elementOn,
					elementOff);

			if (excess && weather) {

				if (gte(soc, battery.pvExcessOn())) {

					if (canRunElement) {
						return Decision.excessPv(qualifiesForExcessPower.nestedMatch(
								"Battery SoC %s above excess PV start threshold %s %% and generator power %s covers heat element %s"
										.formatted(soc, battery.pvExcessOn(), generatorPower, elementOn)));
					}

					return Decision.availablePv(qualifiesForExcessPower.nestedNoMatch(
							"Battery SoC %s above excess PV start threshold %s %% but generator power %s below heat element draw %s, staying on compressor"
									.formatted(soc, battery.pvExcessOn(), generatorPower, elementOn)));
				}

				if (currentState == SgReadyState.NORMAL) {
					return Decision.availablePv(qualifiesForExcessPower.nestedNoMatch(
							"Battery SoC %s below excess PV start threshold %s %%, switching from normal to available"
									.formatted(soc, battery.pvExcessOn())));
				}

				if (currentState == SgReadyState.EXCESS_PV && !canRunElement) {
					return Decision.availablePv(qualifiesForExcessPower.nestedNoMatch(
							"Retaining within hysteresis but generator power %s below heat element draw %s, downgrading to compressor"
									.formatted(generatorPower, elementOn)));
				}

				return new Decision(currentState,
						qualifiesForExcessPower.nestedMatch("Battery SoC %s retaining %s".formatted(soc, currentState)));
			} else if (Hysteresis.active(consuming, soc, battery.pvAvailable(),
					battery.pvAvailable().subtract(properties.getAvailableSocOffMargin()))) {
				return Decision.availablePv(qualifiesForExcessPower
						.nestedMatch("Battery SoC %s above required SoC threshold %s %%".formatted(soc, battery.pvAvailable())));
			} else {
				return Decision.normal(qualifiesForExcessPower
						.nestedNoMatch("Battery SoC %s below required SoC threshold %s %%".formatted(soc, battery.pvAvailable())));
			}
		}

		return Decision.normal(ConditionOutcome.noMatch("Generator power below heat pump consumption"));
	}

	private static ConditionOutcome qualifiesForExcessPower(SgReadyProperties.Levels battery,
			@Nullable LocalTime excessNotBefore, @Nullable LocalTime excessNotAfter, Quantity<Dimensionless> soc,
			LocalDateTime now) {

		ConditionOutcome outcome = null;

		if (excessNotBefore != null) {

			if (now.toLocalTime().isBefore(excessNotBefore)) {
				return ConditionOutcome.noMatch("Current time %s before excess power is allowed %s (not-before)"
						.formatted(now.toLocalTime(), excessNotBefore));
			} else {
				outcome = ConditionOutcome.match("Current time %s after excess power is allowed %s (not-before)"
						.formatted(now.toLocalTime(), excessNotBefore));
			}
		}

		if (excessNotAfter != null) {

			ConditionOutcome notAfter;
			if (now.toLocalTime().isAfter(excessNotAfter)) {
				notAfter = ConditionOutcome.noMatch("Current time %s after excess power is allowed %s (not-after)"
						.formatted(now.toLocalTime(), excessNotAfter));
			} else {
				notAfter = ConditionOutcome.match("Current time %s after excess power is allowed %s (not-after)"
						.formatted(now.toLocalTime(), excessNotAfter));
			}

			outcome = outcome == null ? notAfter : outcome.nested(notAfter);
			if (!notAfter.isMatch()) {
				return outcome;
			}
		}

		ConditionOutcome socBelowPvExcessOff = gte(soc, battery.pvExcessOff())
				? ConditionOutcome
						.match("Battery SoC %s above SoC for excess PV stop threshold %s %%".formatted(soc, battery.pvExcessOff()))
				: ConditionOutcome.noMatch(
						"Battery SoC %s below SoC for excess PV stop threshold %s %%".formatted(soc, battery.pvExcessOff()));

		return outcome == null ? socBelowPvExcessOff : outcome.nested(socBelowPvExcessOff);
	}

	static String format(Duration duration) {
		return DURATION.format(duration.addTo(LocalTime.of(0, 0)));
	}

	/**
	 * Compare two quantities after converting the left-hand value to the right-hand unit.
	 *
	 * @param <Q> the quantity dimension being compared.
	 * @param a the left-hand quantity; must not be {@literal null}.
	 * @param b the right-hand quantity; must not be {@literal null}.
	 * @return {@literal true} if {@code a} is greater than or equal to {@code b}; {@literal false} otherwise.
	 */
	static <Q extends Quantity<Q>> boolean gte(Quantity<Q> a, Quantity<Q> b) {
		return a.to(b.getUnit()).getValue().doubleValue() >= b.getValue().doubleValue();
	}

}

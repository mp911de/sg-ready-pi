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

import biz.paluch.sgreadypi.output.SgReadyStateConsumer;
import biz.paluch.sgreadypi.provider.SunnyHomeManagerService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import javax.measure.Quantity;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Power;

import org.slf4j.event.Level;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Controller to determine the SG ready state. State is determined by power availability using
 * {@link SgReadyState#AVAILABLE_PV}. Once the available power reaches the battery charging limits then the state
 * switches to {@link SgReadyState#EXCESS_PV} to ensure consumption by the heater.
 *
 * @author Mark Paluch
 */
@Slf4j
public class SgReadyControlLoop {

	private final PowerGeneratorService inverters;

	private final SunnyHomeManagerService powerMeter;

	private final SgReadyStateConsumer stateConsumer;

	private final SgReadyProperties properties;

	private final Clock clock;

	@Getter private volatile SgReadyState state = SgReadyState.NORMAL;
	@Getter private volatile Decision decision;

	public SgReadyControlLoop(PowerGeneratorService inverters, SunnyHomeManagerService powerMeter,
			SgReadyStateConsumer stateConsumer, SgReadyProperties properties, Clock clock) {
		this.inverters = inverters;
		this.powerMeter = powerMeter;
		this.stateConsumer = stateConsumer;
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * Control loop.
	 */
	@Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
	public void control() {

		doWithPower((ingress, generatorPower, soc) -> {

			if (!inverters.hasData() || !powerMeter.hasData()) {
				log.warn("Skipping control loop iteration. No data available.");
				return;
			}
			Decision decision = createState(this.state, ingress, generatorPower, soc);
			boolean changed = this.state != decision.state();

			this.state = decision.state();
			this.decision = decision;

			logState(this.state, ingress, generatorPower, soc, changed);
			stateConsumer.onState(state);
		});
	}

	SgReadyState createState() {
		return decide().state();
	}

	Decision decide() {

		Quantity<Power> generatorPower = inverters.getGeneratorPower().getAverage();
		Quantity<Dimensionless> soc = inverters.getBatteryStateOfCharge();
		Quantity<Power> ingress = powerMeter.getIngress().getAverage();

		return createState(this.state, ingress, generatorPower, soc);
	}

	private Decision createState(SgReadyState currentState, Quantity<Power> ingress, Quantity<Power> generatorPower,
			Quantity<Dimensionless> soc) {

		if (gte(ingress, properties.getIngressLimit())) {
			// apply state
			return new Decision(SgReadyState.NORMAL,
					ConditionOutcome.match("Ingress %s exceeds limit %s".formatted(ingress, properties.getIngressLimit())));
		}

		if (gte(generatorPower, properties.getHeatPumpPowerConsumption())) {

			ConditionOutcome match = ConditionOutcome.match("Generator power %s above heat pump consumption %s"
					.formatted(generatorPower, properties.getHeatPumpPowerConsumption()));

			SgReadyProperties.Levels battery = properties.getBattery();
			ConditionOutcome qualifiesForExcessPower = match
					.nested(qualifiesForExcessPower(battery, properties.getExcessNotBefore(), soc));

			if (qualifiesForExcessPower.isMatch()) {

				if (gte(soc, battery.pvExcessOn())) {
					return new Decision(SgReadyState.EXCESS_PV, qualifiesForExcessPower.nestedMatch(
							"Battery SoC %s above SoC for excess PV start threshold %s %%".formatted(soc, battery.pvExcessOn())));
				}

				if (currentState == SgReadyState.NORMAL) {
					return new Decision(SgReadyState.AVAILABLE_PV,
							qualifiesForExcessPower.nestedNoMatch(
									"Battery SoC %s below SoC for excess PV start threshold %s %%, switching from normal to available"
											.formatted(soc, battery.pvExcessOn())));
				}

				return new Decision(currentState, qualifiesForExcessPower.nestedNoMatch("Retaining state"));
			} else if (gte(soc, battery.pvAvailable())) {
				return new Decision(SgReadyState.AVAILABLE_PV, qualifiesForExcessPower
						.nestedMatch("Battery SoC %s of charge above required SoC %s %%".formatted(soc, battery.pvAvailable())));
			}

			return new Decision(currentState, match.nestedNoMatch("Retaining current state"));
		}

		return new Decision(SgReadyState.NORMAL, ConditionOutcome.noMatch("Generator power below heat pump consumption"));
	}

	private ConditionOutcome qualifiesForExcessPower(SgReadyProperties.Levels battery,
			@Nullable LocalTime excessNotBefore, Quantity<Dimensionless> soc) {

		ConditionOutcome outcome = null;
		if (excessNotBefore != null) {

			LocalDateTime now = LocalDateTime.now(clock);

			if (now.toLocalTime().isBefore(excessNotBefore)) {
				return ConditionOutcome
						.noMatch("Current time %s before excess power is allowed %s".formatted(now.toLocalTime(), excessNotBefore));
			} else {
				outcome = ConditionOutcome
						.match("Current time %s after excess power is allowed %s".formatted(now.toLocalTime(), excessNotBefore));
			}
		}

		ConditionOutcome socBelowPvExcessOff = gte(soc, battery.pvExcessOff())
				? ConditionOutcome
						.match("Battery SoC %s above SoC for excess PV stop threshold %s %%".formatted(soc, battery.pvExcessOff()))
				: ConditionOutcome.noMatch(
						"Battery SoC %s below SoC for excess PV stop threshold %s %%".formatted(soc, battery.pvExcessOff()));

		return outcome == null ? socBelowPvExcessOff : outcome.nested(socBelowPvExcessOff);
	}

	/**
	 * Notify {@link PowerConsumer} with current power state readings and run the function it defines.
	 *
	 * @param consumer
	 */
	private void doWithPower(PowerConsumer consumer) {

		Quantity<Power> generatorPower = inverters.getGeneratorPower().getAverage();
		Quantity<Power> ingress = powerMeter.getIngress().getAverage();
		Quantity<Dimensionless> soc = inverters.getBatteryStateOfCharge();

		consumer.apply(ingress, generatorPower, soc);
	}

	/**
	 * Log state.
	 *
	 * @param state
	 * @param ingress
	 * @param pv
	 * @param soc
	 * @param changed
	 */
	private void logState(SgReadyState state, Quantity<Power> ingress, Quantity<Power> pv, Quantity<Dimensionless> soc,
			boolean changed) {

		Level level = changed ? Level.INFO : Level.DEBUG;
		if (log.isEnabledForLevel(level)) {
			log.atLevel(level).log(String.format("SG Ready: %s, Ingress %s, PV %s, SoC %s", state, ingress, pv, soc));
		}
	}

	/**
	 * Greater than/equals comparison.
	 *
	 * @param a
	 * @param b
	 * @return
	 * @param <Q>
	 */
	static <Q extends Quantity<Q>> boolean gte(Quantity<Q> a, Quantity<Q> b) {
		return a.getValue().doubleValue() >= b.getValue().doubleValue();
	}

	/**
	 * Consumer for power values.
	 */
	interface PowerConsumer {

		void apply(Quantity<Power> ingress, Quantity<Power> generatorPower, Quantity<Dimensionless> soc);
	}

	/**
	 * Outcome for a condition match, including log message.
	 */
	static class ConditionOutcome {

		private final @Nullable ConditionOutcome parent;

		private final boolean match;

		private final String message;

		private ConditionOutcome(@Nullable ConditionOutcome parent, boolean match, String message) {
			this.parent = parent;
			this.match = match;
			this.message = message;
		}

		/**
		 * Create a {@link ConditionOutcome} for a matching condition including a {@code message}.
		 *
		 * @param message
		 * @return
		 */
		public static ConditionOutcome match(String message) {
			return new ConditionOutcome(null, true, message);
		}

		/**
		 * Create a {@link ConditionOutcome} for a not matching condition including a {@code message}.
		 *
		 * @param message
		 * @return
		 */
		public static ConditionOutcome noMatch(String message) {
			return new ConditionOutcome(null, false, message);
		}

		/**
		 * Create a nested {@link ConditionOutcome} for a matching condition including a {@code message}.
		 *
		 * @param message
		 * @return
		 */
		public ConditionOutcome nestedMatch(String message) {
			return new ConditionOutcome(this, true, message);
		}

		/**
		 * Create a nested {@link ConditionOutcome} for a not matching condition including a {@code message}.
		 *
		 * @param message
		 * @return
		 */
		public ConditionOutcome nestedNoMatch(String message) {
			return new ConditionOutcome(this, false, message);
		}

		/**
		 * Create a nested {@link ConditionOutcome} for a nested {@link ConditionOutcome}.
		 *
		 * @param message
		 * @return
		 */
		public ConditionOutcome nested(ConditionOutcome nested) {
			return new ConditionOutcome(this, nested.isMatch(), nested.message);
		}

		@Nullable
		public ConditionOutcome getParent() {
			return parent;
		}

		public boolean isMatch() {
			return match;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public String toString() {

			StringBuilder builder = new StringBuilder();

			if (parent != null) {
				builder.append(parent).append(" -> ");
			}

			builder.append(isMatch() ? "Match" : "No match").append(": ").append(getMessage());

			return builder.toString();
		}
	}

	record Decision(SgReadyState state, ConditionOutcome conditionOutcome) {

	}

}

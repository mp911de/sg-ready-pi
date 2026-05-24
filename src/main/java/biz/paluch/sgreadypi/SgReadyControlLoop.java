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
import biz.paluch.sgreadypi.weather.WeatherService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * Control loop that drives the SG Ready state. It resolves the current {@link Conditions} from the power generator and
 * power meter, fetches the weather {@link WeatherService.Range Range} when weather optimisation is enabled, and hands
 * everything to the pure {@link SgReadyPolicy} which returns the {@link Decision}. The loop is the I/O shell; all
 * state-selection rules live in the policy.
 *
 * @author Mark Paluch
 */
public class SgReadyControlLoop {

	private static final Logger log = LoggerFactory.getLogger(SgReadyControlLoop.class);

	private final PowerGeneratorService inverters;

	private final SunnyHomeManagerService powerMeter;

	private final SgReadyStateConsumer stateConsumer;

	private final SgReadyProperties properties;

	private final WeatherService weatherService;

	private final Clock clock;

	private final SgReadyPolicy policy;

	private volatile SgReadyState state = SgReadyState.NORMAL;

	private volatile @Nullable Decision decision;

	public SgReadyControlLoop(PowerGeneratorService inverters, SunnyHomeManagerService powerMeter,
			SgReadyStateConsumer stateConsumer, SgReadyProperties properties, WeatherService weatherService, Clock clock) {
		this.inverters = inverters;
		this.powerMeter = powerMeter;
		this.stateConsumer = stateConsumer;
		this.properties = properties;
		this.weatherService = weatherService;
		this.clock = clock;
		this.policy = new SgReadyPolicy(properties);
	}

	public SgReadyState getState() {
		return state;
	}

	public @Nullable Decision getDecision() {
		return decision;
	}

	/**
	 * Control loop.
	 */
	@Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
	public void control() {

		Conditions conditions = readConditions();

		if (conditions.outOfService()) {
			log.warn("Out of service, returning to normal state.");
		} else if (!inverters.hasData() || !powerMeter.hasData()) {
			log.warn("Skipping control loop iteration. No data available.");
			return;
		}

		applyDecision(decide(conditions), conditions);
	}

	private void applyDecision(Decision decision, Conditions conditions) {

		boolean changed = this.state != decision.state();

		this.state = decision.state();
		this.decision = decision;

		logState(this.state, conditions, changed);
		this.stateConsumer.onState(this.state);
	}

	private Decision decide(Conditions conditions) {

		SgReadyProperties.Weather weather = properties.getWeather();
		WeatherService.Range weatherRange = weather != null && weather.isEnabled() && !conditions.outOfService()
				? weatherService.getUsableTimeRange()
				: null;

		return policy.decide(this.state, conditions, weatherRange, LocalDateTime.now(clock));
	}

	/**
	 * Read the current {@link Conditions} from the power generator and power meter.
	 */
	private Conditions readConditions() {
		return new Conditions(powerMeter.getIngress().getAverage(), inverters.getGeneratorPower().getAverage(),
				inverters.getBatteryStateOfCharge(), inverters.isOutOfService() || powerMeter.isOutOfService());
	}

	/**
	 * Log state.
	 */
	private void logState(SgReadyState state, Conditions conditions, boolean changed) {

		Level level = changed ? Level.INFO : Level.DEBUG;
		if (log.isEnabledForLevel(level)) {
			log.atLevel(level).log(String.format("SG Ready: %s, Ingress %s, PV %s, SoC %s", state, conditions.ingress(),
					conditions.generatorPower(), conditions.soc()));
		}
	}

}

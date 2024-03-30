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

import java.util.concurrent.TimeUnit;

import javax.measure.Quantity;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Power;

import org.slf4j.event.Level;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Controller to determine the SG ready state. State is determined by power availability using
 * {@link SgReadyState#AVAILABLE_PV}. Once the available power reaches the battery charging limits then the state
 * switches to {@link SgReadyState#EXCESS_PV} to ensure consumption by the heater.
 *
 * @author Mark Paluch
 */
@Slf4j
@Component
public class SgReadyControlLoop {

	private final PowerGeneratorService inverters;

	private final SunnyHomeManagerService powerMeter;

	private final SgReadyStateConsumer stateConsumer;
	private final SgReadyProperties properties;

	@Getter private volatile SgReadyState state = SgReadyState.NORMAL;

	public SgReadyControlLoop(PowerGeneratorService inverters, SunnyHomeManagerService powerMeter,
			SgReadyStateConsumer stateConsumer, SgReadyProperties properties) {
		this.inverters = inverters;
		this.powerMeter = powerMeter;
		this.stateConsumer = stateConsumer;
		this.properties = properties;
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
			SgReadyState state = createState(ingress, generatorPower, soc);
			boolean changed = this.state != state;

			this.state = state;

			logState(this.state, ingress, generatorPower, soc, changed);
			stateConsumer.onState(state);
		});
	}

	SgReadyState createState() {

		Quantity<Power> generatorPower = inverters.getGeneratorPower();
		Quantity<Dimensionless> soc = inverters.getBatteryStateOfCharge();
		Quantity<Power> ingress = powerMeter.getIngress();

		return createState(ingress, generatorPower, soc);
	}

	private SgReadyState createState(Quantity<Power> ingress, Quantity<Power> generatorPower,
			Quantity<Dimensionless> soc) {

		if (gte(ingress, properties.getIngressLimit())) {
			// apply state
			return SgReadyState.NORMAL;
		}

		if (gte(generatorPower, properties.getHeatPumpPowerConsumption())) {

			if (gte(soc, properties.getBattery().pvExcessOff())) {

				if (gte(soc, properties.getBattery().pvExcessOn())) {
					return SgReadyState.EXCESS_PV;
				}

				if (state == SgReadyState.NORMAL) {
					return SgReadyState.AVAILABLE_PV;
				}
			} else if (gte(soc, properties.getBattery().pvAvailable())) {
				return SgReadyState.AVAILABLE_PV;
			}
		}

		return this.state;
	}

	/**
	 * Apply SG Ready state.
	 *
	 * @param state
	 */
	private void applyState(SgReadyState state) {
		this.state = state;
	}

	/**
	 * Notify {@link PowerConsumer} with current power state readings and run the function it defines.
	 *
	 * @param consumer
	 */
	private void doWithPower(PowerConsumer consumer) {

		Quantity<Power> generatorPower = inverters.getGeneratorPower();
		Quantity<Power> ingress = powerMeter.getIngress();
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
			log.atLevel(level).log(String.format("SG-Ready: %s, Ingress %s, PV %s, SoC %s", state, ingress, pv, soc));
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

}

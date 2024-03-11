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
package com.sample.sgready.provider;

import com.sample.sgready.PowerMeter;
import com.sample.sgready.SgReadyProperties;
import io.github.joblo2213.sma.speedwire.Speedwire;
import io.github.joblo2213.sma.speedwire.protocol.measuringChannels.EnergyMeterChannels;
import io.github.joblo2213.sma.speedwire.protocol.telegrams.EnergyMeterTelegram;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import java.io.IOException;
import java.time.Instant;

import javax.measure.Quantity;
import javax.measure.quantity.Power;

import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

/**
 * @author Mark Paluch
 */
@Service
@Slf4j
public class SunnyHomeManagerService implements SmartLifecycle, PowerMeter {

	private Speedwire speedwire;
	private final SgReadyProperties properties;

	private volatile int ingress;
	@Getter protected volatile Instant reading = Instant.MIN;

	public SunnyHomeManagerService(SgReadyProperties properties) {
		this.properties = properties;
	}

	@Override
	public Quantity<Power> getIngress() {
		return Quantities.getQuantity(ingress, Units.WATT);
	}

	@Override
	public void start() {

		log.info("Starting SunnyHomeManagerService");

		try {
			this.speedwire = new Speedwire();
			this.speedwire.onError(e -> log.error("Speedwire error", e));
			this.speedwire.onTimeout(() -> log.warn("Speedwire timeout"));

			speedwire.onData(data -> {
				if (data instanceof EnergyMeterTelegram em) {

					// device information
					long powerMeterId = em.getSerNo().longValueExact();

					if (powerMeterId == properties.getPowerMeterId()) {

						Quantity<Power> w = em.getData(EnergyMeterChannels.TOTAL_P_IN).to(Units.WATT);
						ingress = w.getValue().intValue();
						reading = Instant.now();
					}
				}
			});

			this.speedwire.start();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void stop() {
		if (this.speedwire != null) {
			this.speedwire.shutdown();
			this.speedwire = null;
		}
	}

	@Override
	public boolean isRunning() {

		if (this.speedwire == null) {
			return false;
		}

		return this.speedwire.getState() != Thread.State.TERMINATED;
	}

	@Override
	public boolean hasData() {
		return isRunning() && reading.isAfter(Instant.now().minusSeconds(60));
	}
}

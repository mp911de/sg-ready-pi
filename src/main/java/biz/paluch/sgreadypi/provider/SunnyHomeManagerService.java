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
package biz.paluch.sgreadypi.provider;

import io.github.joblo2213.sma.speedwire.Speedwire;
import io.github.joblo2213.sma.speedwire.protocol.measuringChannels.EnergyMeterChannels;
import io.github.joblo2213.sma.speedwire.protocol.telegrams.DiscoveryResponse;
import io.github.joblo2213.sma.speedwire.protocol.telegrams.EnergyMeterTelegram;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.measure.Quantity;
import javax.measure.quantity.Power;

import org.springframework.context.SmartLifecycle;

import biz.paluch.sgreadypi.PowerMeter;

import com.codahale.metrics.SlidingTimeWindowMovingAverages;

/**
 * {@link PowerMeter} implementation for Sunny Home Manager 2.0.
 *
 * @author Mark Paluch
 * @link <a href="https://www.sma.de/produkte/monitoring-control/sunny-home-manager">Sunny Home Manager 2.0</a>
 */
@Slf4j
public class SunnyHomeManagerService implements SmartLifecycle, PowerMeter {

	private static final Duration TIMEOUT = Duration.ofMinutes(1);

	private Speedwire speedwire;
	private final long powerMeterId;

	private volatile int ingress;
	private volatile int egress;

	private final Average averagesIn = new Average(Duration.ofMinutes(1));
	private final Average averagesOut = new Average(Duration.ofMinutes(1));

	@Getter protected volatile Instant reading = Instant.MIN;

	public SunnyHomeManagerService(long powerMeterId) {
		this.powerMeterId = powerMeterId;
	}

	@Override
	public Quantity<Power> getIngress() {
		return Quantities.getQuantity(Math.round(averagesIn.getAverage()), Units.WATT);
	}

	@Override
	public Quantity<Power> getMomentaryIngress() {
		return Quantities.getQuantity(ingress, Units.WATT);
	}

	@Override
	public Quantity<Power> getEgress() {
		return Quantities.getQuantity(Math.round(averagesOut.getAverage()), Units.WATT);
	}

	@Override
	public Quantity<Power> getMomentaryEgress() {
		return Quantities.getQuantity(egress, Units.WATT);
	}

	@Override
	public void start() {

		log.info("Starting SunnyHomeManagerService");

		try {
			this.speedwire = new Speedwire();
			this.speedwire.onError(e -> log.error("Speedwire error", e));
			this.speedwire.onTimeout(() -> log.warn("Speedwire timeout"));

			speedwire.onData(data -> {

				if (data instanceof DiscoveryResponse dr) {
					log.info("Discovered Power Meter " + dr.getOrigin().getHostAddress());
				}

				if (data instanceof EnergyMeterTelegram em) {

					// device information
					long powerMeterId = em.getSerNo().longValueExact();

					if (powerMeterId == this.powerMeterId) {

						Quantity<Power> in = em.getData(EnergyMeterChannels.TOTAL_P_IN).to(Units.WATT);
						ingress = in.getValue().intValue();
						averagesIn.add(ingress);

						Quantity<Power> out = em.getData(EnergyMeterChannels.TOTAL_P_OUT).to(Units.WATT);
						egress = out.getValue().intValue();
						averagesOut.add(egress);
						reading = Instant.now();
					}
				}
			});

			this.speedwire.start();
			this.speedwire.sendDiscoveryRequest();
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
		return isRunning() && reading.isAfter(Instant.now().minus(TIMEOUT));
	}

}

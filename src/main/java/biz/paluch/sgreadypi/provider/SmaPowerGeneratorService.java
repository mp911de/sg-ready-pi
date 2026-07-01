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

import biz.paluch.sgreadypi.PowerGeneratorService;
import biz.paluch.sgreadypi.RecencyTracker;
import biz.paluch.sgreadypi.SgReadyProperties;
import biz.paluch.sgreadypi.measure.Percent;
import biz.paluch.sgreadypi.measure.Watt;
import cat.joanpujol.smasolar.modbus.ModbusRegister;
import cat.joanpujol.smasolar.modbus.SmaModbusClient;
import cat.joanpujol.smasolar.modbus.SmaModbusRequest;
import cat.joanpujol.smasolar.modbus.SmaModbusResponse;
import tech.units.indriya.unit.Units;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import javax.measure.Quantity;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Power;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.TaskScheduler;

/**
 * {@link PowerGeneratorService} implementation based on Modbus communication supporting Sunny Tripower inverters.
 *
 * @author Mark Paluch
 */
public class SmaPowerGeneratorService implements SmartLifecycle, PowerGeneratorService {

	private static final Logger log = org.slf4j.LoggerFactory.getLogger(SmaPowerGeneratorService.class);
	private final AtomicBoolean running = new AtomicBoolean(false);

	private final Map<String, SmaModbusClient> clients = new LinkedHashMap<>();
	private final Map<String, InverterState> stateMap = new ConcurrentHashMap<>();

	private final Map<String, MutableStatistics<Power>> solarStats = new ConcurrentHashMap<>();
	private final Map<String, MutableStatistics<Power>> dischargeStats = new ConcurrentHashMap<>();

	private final SgReadyProperties properties;
	private final TaskScheduler executorService;
	private volatile @Nullable ScheduledFuture<?> schedule;

	public SmaPowerGeneratorService(SgReadyProperties properties, TaskScheduler executorService) {
		this.properties = properties;
		this.executorService = executorService;

		for (String inverterHost : properties.getInverterHosts()) {
			clients.put(inverterHost, new SmaModbusClient(inverterHost, properties.getInverterPort(), registerReader -> 3));
		}
	}

	@Override
	public void start() {
		if (running.compareAndSet(false, true)) {

			clients.forEach((host, client) -> client.connect()
					.doOnError(err -> log.error("InverterService failed to connect to " + host, err)).subscribe());
			readInverters();

			schedule = executorService.scheduleAtFixedRate(this::readInverters, properties.getQueryInterval());
		}
	}

	private void readInverters() {

		clients.forEach((host, client) -> client.read(createRequest())

				.doOnError(err -> log.error("InverterService failed to read from " + host, err)).subscribe(response -> {

					int currentActivePower = getIntRegister(response, ModbusRegister.CURRENT_ACTIVE_POWER);
					int batteryCharging = getIntRegister(response, ModbusRegister.BATTERY_CURRENT_CHARGING);
					int batteryDischarging = getIntRegister(response, ModbusRegister.BATTERY_CURRENT_DISCHARGING);
					int stateOfCharge = getIntRegister(response, ModbusRegister.CURRENT_BATTERY_STATE_OF_CHARGE);
					Number capacity = response.getRegisterValue(ModbusRegister.CURRENT_BATTERY_CAPACITY);

					InverterState state = new InverterState(currentActivePower, capacity != null && capacity.intValue() > 0,
							batteryCharging, batteryDischarging, stateOfCharge, Instant.now());

					log.debug("Inverter at {} state {}", host, state);

					stateMap.put(host, state);
					statistics(solarStats, host).update(state.getSolarPower());
					statistics(dischargeStats, host).update(state.getBatteryDischarge());
				}));
	}

	private static SmaModbusRequest createRequest() {
		return new SmaModbusRequest.Builder(SmaModbusRequest.Type.READ).addRegister(ModbusRegister.CURRENT_ACTIVE_POWER)
				.addRegister(ModbusRegister.BATTERY_CURRENT_DISCHARGING).addRegister(ModbusRegister.BATTERY_CURRENT_CHARGING)
				.addRegister(ModbusRegister.CURRENT_BATTERY_STATE_OF_CHARGE)
				.addRegister(ModbusRegister.CURRENT_BATTERY_CAPACITY).build();
	}

	private MutableStatistics<Power> statistics(Map<String, MutableStatistics<Power>> stats, String host) {
		return stats.computeIfAbsent(host, it -> MutableStatistics.create(properties.getAveraging(), Units.WATT));
	}

	private static int getIntRegister(SmaModbusResponse response, ModbusRegister<Number> register) {
		Number value = response.getRegisterValue(register);

		if (value == null) {
			return 0;
		}

		return value.intValue();
	}

	@Override
	public void stop() {
		if (running.compareAndSet(true, false)) {

			ScheduledFuture<?> schedule = this.schedule;
			this.schedule = null;
			if (schedule != null) {
				schedule.cancel(false);
			}

			clients.forEach((host, client) -> client.disconnect().subscribe());
		}
	}

	@Override
	public boolean isRunning() {
		return running.get();
	}

	@Override
	public Quantity<Dimensionless> getBatteryStateOfCharge() {
		double average = stateMap.values().stream().filter(InverterState::hasBattery).mapToInt(InverterState::stateOfCharge)
				.average().orElse(0);
		return Percent.of(average);
	}

	@Override
	public Statistics<Power> getGeneratorPower() {
		return aggregate(solarStats);
	}

	@Override
	public Statistics<Power> getBatteryDischarge() {
		return aggregate(dischargeStats);
	}

	private static Statistics<Power> aggregate(Map<String, MutableStatistics<Power>> stats) {
		return new Statistics<>() {
			@Override
			public Quantity<Power> getAverage() {
				return sum(stats, Statistics::getAverage);
			}

			@Override
			public Quantity<Power> getMostRecent() {
				return sum(stats, Statistics::getMostRecent);
			}
		};
	}

	private static Quantity<Power> sum(Map<String, MutableStatistics<Power>> stats,
			Function<Statistics<Power>, Quantity<Power>> value) {
		return Watt.of(stats.values().stream().mapToInt(it -> value.apply(it).getValue().intValue()).sum());
	}

	public Map<String, InverterState> getStateMap() {
		return new LinkedHashMap<>(stateMap);
	}

	@Override
	public boolean hasData() {
		return !stateMap.isEmpty();
	}

	@Override
	public boolean isOutOfService() {

		for (InverterState value : stateMap.values()) {
			if (value.getHealthState().isOutOfService()) {
				return true;
			}
		}

		return false;
	}

	public record InverterState(int currentActivePower, boolean hasBattery, int batteryCharging, int batteryDischarging,
			int stateOfCharge, Instant timestamp) implements RecencyTracker {

		/**
		 * The usable solar surplus: active power minus net battery discharge, so the figure reflects sun-derived power
		 * rather than power pulled out of the battery (ADR-0004).
		 */
		public Quantity<Power> getSolarPower() {
			return Watt.of(currentActivePower - dischargeWatts());
		}

		/**
		 * Net battery discharge: power drawn from the battery minus power charging it. Negative while charging dominates.
		 */
		public Quantity<Power> getBatteryDischarge() {
			return Watt.of(dischargeWatts());
		}

		private int dischargeWatts() {
			return Math.abs(batteryDischarging) - batteryCharging;
		}

		@Override
		public Duration dataAge() {
			return Duration.between(timestamp, Instant.now());
		}
	}

}

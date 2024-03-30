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

import cat.joanpujol.smasolar.modbus.ModbusRegister;
import cat.joanpujol.smasolar.modbus.SmaModbusClient;
import cat.joanpujol.smasolar.modbus.SmaModbusRequest;
import cat.joanpujol.smasolar.modbus.SmaModbusResponse;
import biz.paluch.sgreadypi.PowerGeneratorService;
import biz.paluch.sgreadypi.SgReadyProperties;
import lombok.extern.slf4j.Slf4j;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.measure.Quantity;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Power;

import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.TaskScheduler;

/**
 * @author Mark Paluch
 */
@Slf4j
public class SmaPowerGeneratorService implements SmartLifecycle, PowerGeneratorService {

	private final AtomicBoolean running = new AtomicBoolean(false);

	private final Map<String, SmaModbusClient> clients = new LinkedHashMap<>();
	private final Map<String, InverterState> stateMap = new ConcurrentHashMap<>();

	private final SgReadyProperties properties;
	private final TaskScheduler executorService;
	private volatile ScheduledFuture<?> schedule;

	private final Supplier<SmaModbusRequest> requestFactory = () -> new SmaModbusRequest.Builder(
			SmaModbusRequest.Type.READ).addRegister(ModbusRegister.CURRENT_ACTIVE_POWER)
			.addRegister(ModbusRegister.BATTERY_CURRENT_DISCHARGING).addRegister(ModbusRegister.BATTERY_CURRENT_CHARGING)
			.addRegister(ModbusRegister.CURRENT_BATTERY_STATE_OF_CHARGE).addRegister(ModbusRegister.CURRENT_BATTERY_CAPACITY)
			.build();

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

		clients.forEach((host, client) -> client.read(requestFactory.get())

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
				}));
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
		return Quantities.getQuantity(
				stateMap.values().stream().filter(InverterState::hasBattery).mapToInt(InverterState::stateOfCharge).sum(),
				Units.PERCENT);
	}

	@Override
	public Quantity<Power> getGeneratorPower() {
		return Quantities.getQuantity(
				stateMap.values().stream()
						.mapToInt(it -> it.currentActivePower() + it.batteryCharging() - it.batteryDischarging()).sum(),
				Units.WATT);
	}

	public Map<String, InverterState> getStateMap() {
		return new LinkedHashMap<>(stateMap);
	}

	@Override
	public boolean hasData() {
		return !stateMap.isEmpty();
	}

	public record InverterState(int currentActivePower, boolean hasBattery, int batteryCharging, int batteryDischarging,
			int stateOfCharge, Instant timestamp) {
	}

}

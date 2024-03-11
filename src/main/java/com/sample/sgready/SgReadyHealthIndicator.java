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
package com.sample.sgready;

import com.sample.sgready.provider.SmaPowerGeneratorService;
import com.sample.sgready.provider.SunnyHomeManagerService;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

import javax.measure.Quantity;
import javax.measure.quantity.Power;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator displaying the SG ready state and underlying values.
 *
 * @author Mark Paluch
 */
@Component
@Value
public class SgReadyHealthIndicator implements HealthIndicator {

	SmaPowerGeneratorService inverterService;
	SgReadyControlLoop controller;
	SunnyHomeManagerService homeManagerService;

	@Override
	public Health health() {

		Instant powerMeterReading = homeManagerService.getReading();
		Quantity<Power> ingress = homeManagerService.getIngress();

		Map<String, SmaPowerGeneratorService.InverterState> stateMap = inverterService.getStateMap();

		Health.Builder builder = newHealthBuilder(powerMeterReading, stateMap);

		builder.withDetail("ingress", ingress.toString());

		builder.withDetail("power-meter-reading", powerMeterReading.toString());

		builder.withDetail("generator-power", inverterService.getGeneratorPower().toString());
		builder.withDetail("battery-soc", inverterService.getBatteryStateOfCharge().toString());

		stateMap.forEach((inverter, inverterState) -> {
			builder.withDetail("inverter-" + inverter, inverterState);
		});

		builder.withDetail("sg-ready", controller.getState());

		return builder.build();
	}

	private Health.Builder newHealthBuilder(Instant powerMeterReading,
			Map<String, SmaPowerGeneratorService.InverterState> stateMap) {

		Instant limit = Instant.now().minusSeconds(60);

		if (powerMeterReading.isBefore(limit)) {
			return Health.down();
		}

		if (stateMap.isEmpty()) {
			return Health.down();
		}

		for (SmaPowerGeneratorService.InverterState value : stateMap.values()) {
			if (value.timestamp().isBefore(limit)) {
				return Health.down();
			}
		}

		return Health.up();
	}
}

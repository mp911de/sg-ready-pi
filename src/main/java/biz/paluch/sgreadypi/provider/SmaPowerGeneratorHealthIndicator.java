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

import biz.paluch.sgreadypi.RecencyTracker;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.measure.quantity.Power;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.format.annotation.DurationFormat;
import org.springframework.format.datetime.standard.DurationFormatterUtils;
import org.springframework.stereotype.Component;

/**
 * Health Contributor for {@link SmaPowerGeneratorService}.
 *
 * @author Mark Paluch
 */
@Component
record SmaPowerGeneratorHealthIndicator(SmaPowerGeneratorService powerGenerator) implements HealthIndicator {

	@Override
	public Health health() {
		Health.Builder builder = Health.up();
		contribute(builder);
		return builder.build();
	}

	private void contribute(Health.Builder builder) {

		Map<String, SmaPowerGeneratorService.InverterState> stateMap = powerGenerator.getStateMap();
		Map<String, Object> inverters = new LinkedHashMap<>();

		for (SmaPowerGeneratorService.InverterState value : stateMap.values()) {
			RecencyTracker.HealthState healthState = value.getHealthState();

			if (!healthState.isHealthy()) {

				Duration dataAge = value.dataAge();
				String message = "Reading timeout (" + DurationFormatterUtils.print(dataAge, DurationFormat.Style.COMPOSITE)
						+ ")";

				if (healthState.isDegraded()) {
					builder.outOfService().withDetail("inverter-down-reason", message);
				} else {
					builder.down().withDetail("inverter-down-reason", message);
				}
			}
		}

		if (stateMap.isEmpty()) {
			builder.outOfService().withDetail("inverter-down-reason", "No readings");
		}

		Statistics<Power> generatorPower = powerGenerator.getGeneratorPower();
		builder.withDetail("generator-power", generatorPower.getAverage().toString());
		builder.withDetail("generator-power-momentary", generatorPower.getMostRecent().toString());
		builder.withDetail("battery-soc", powerGenerator.getBatteryStateOfCharge().toString());

		stateMap.forEach((inverter, inverterState) -> {
			inverters.put("inverter-" + inverter, inverterState);
		});

		builder.withDetail("inverters", inverters);
	}

}

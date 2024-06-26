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

import lombok.Value;

import javax.measure.quantity.Power;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health Contributor for {@link SmaPowerGeneratorService}.
 *
 * @author Mark Paluch
 */
@Component
@Value
class SmaPowerGeneratorHealthIndicator implements HealthIndicator {

	SmaPowerGeneratorService powerGenerator;

	@Override
	public Health health() {
		Health.Builder builder = Health.up();
		contribute(builder);
		return builder.build();
	}

	private void contribute(Health.Builder builder) {

		Instant limit = Instant.now().minusSeconds(60);
		Map<String, SmaPowerGeneratorService.InverterState> stateMap = powerGenerator.getStateMap();
		Map<String, Object> inverters = new LinkedHashMap<>();

		for (SmaPowerGeneratorService.InverterState value : stateMap.values()) {
			if (value.timestamp().isBefore(limit)) {
				builder.down().withDetail("inverter-down-reason", "Reading timeout");
			}
		}
		if (stateMap.isEmpty()) {
			builder.down().withDetail("inverter-down-reason", "No readings");
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

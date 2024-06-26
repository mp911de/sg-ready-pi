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

import javax.measure.Quantity;
import javax.measure.quantity.Power;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health Contributor for {@link SunnyHomeManagerService}.
 *
 * @author Mark Paluch
 */
@Component
@Value
class SunnyHomeManagerHealthIndicator implements HealthIndicator {

	SunnyHomeManagerService service;

	@Override
	public Health health() {
		Health.Builder builder = Health.up();
		contribute(builder);
		return builder.build();
	}

	private void contribute(Health.Builder builder) {

		Instant powerMeterReading = service.getReading();
		Instant limit = Instant.now().minusSeconds(60);

		if (powerMeterReading.isBefore(limit)) {
			builder.down().withDetail("power-meter-down-reason", "Reading timeout");
		}

		Statistics<Power> ingress = service.getIngress();
		Statistics<Power> egress = service.getEgress();

		builder.withDetail("last-update", powerMeterReading);
		builder.withDetail("ingress", ingress.getAverage().toString());
		builder.withDetail("ingress-momentary", ingress.getMostRecent().toString());
		builder.withDetail("egress", egress.getAverage().toString());
		builder.withDetail("egress-momentary", egress.getMostRecent().toString());
	}

}

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
package biz.paluch.sgreadypi.weather;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health Contributor for {@link WeatherClient} and {@link SunPositionCalculator}.
 *
 * @author Mark Paluch
 */
@Component
@ConditionalOnProperty(value = "sg.weather.enabled", havingValue = "true")
record WeatherHealthIndicator(WeatherService weatherService) implements HealthIndicator {

	@Override
	public Health health() {
		Health.Builder builder = Health.up();
		contribute(builder);
		return builder.build();
	}

	private void contribute(Health.Builder builder) {

		WeatherState weather = weatherService.getWeatherState();
		LocalDateTime sunset = weatherService.getSunset();
		WeatherService.Range timeRange = weatherService.getUsableTimeRange();

		builder.withDetail("weather", weather);
		builder.withDetail("sunset", sunset);

		Map<String, ? extends Comparable<? extends Comparable<?>>> weatherDetail = Map.of("from", timeRange.from(),
				"afterSunset", timeRange.afterSunset(), "afterSunsetLimit", timeRange.afterSunsetLimit(), "enoughSunHours",
				timeRange.enoughRemainingSunHours());

		builder.withDetail("sunset", sunset);
		builder.withDetail("detail", weatherDetail);
	}

}

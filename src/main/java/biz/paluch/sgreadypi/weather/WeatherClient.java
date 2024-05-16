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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

/**
 * API Client for <a href="https://open-meteo.com/">Open Meteo</a>.
 *
 * @author Mark Paluch
 */
class WeatherClient {

	private static final String API_URL = "https://api.open-meteo.com/v1/forecast?latitude={latitude}&longitude={longitude}&current=temperature,weather_code,pressure_msl,surface_pressure&hourly=temperature_2m,weather_code,cloud_cover_low,cloud_cover_mid&forecast_days=1";
	private final Duration UPDATE_DELAY = Duration.ofHours(1);

	private final RestTemplate restTemplate;
	private final Clock clock;

	private volatile WeatherState cached;
	private volatile Instant lastUpdate;

	public WeatherClient(RestTemplateBuilder builder, Clock clock) {
		this.restTemplate = builder.build();
		this.clock = clock;
	}

	/**
	 * Obtain the weather state for a given {@link GeoPosition}.
	 *
	 * @param position
	 * @return
	 */
	public WeatherState getWeatherState(GeoPosition position) {

		WeatherState cached = this.cached;
		Instant lastUpdate = this.lastUpdate;

		if (cached == null || lastUpdate == null || lastUpdate.plus(UPDATE_DELAY).isBefore(clock.instant())) {
			cached = doGetWeatherState(position);
			lastUpdate = clock.instant();

			this.cached = cached;
			this.lastUpdate = lastUpdate;
		}

		return cached;
	}

	private WeatherState doGetWeatherState(GeoPosition position) {

		Map<String, Double> uriVariables = Map.of("latitude", position.latitude(), "longitude", position.longitude());
		WeatherResponse response = restTemplate.getForObject(API_URL, WeatherResponse.class, uriVariables);

		ZoneId zone = clock.getZone();
		ZoneId responseZone = TimeZone.getTimeZone(response.getTimeZone()).toZoneId();

		WeatherResponse.HourlyWeather hourly = response.getHourly();
		List<WeatherState.CloudCoverage> coverages = new ArrayList<>(hourly.time.size());
		for (int i = 0; i < hourly.time.size(); i++) {

			LocalDateTime localResponse = hourly.time.get(i);
			LocalDateTime here = localResponse.atZone(responseZone).toInstant().atZone(zone).toLocalDateTime();
			int mid = hourly.cloudMid.get(i);
			int low = hourly.cloudLow.get(i);

			coverages.add(new WeatherState.CloudCoverage(here, (int) Math.max(low, mid * 0.8)));
		}

		return new WeatherState((int) response.current.temperature, (int) response.current.pressure, coverages);
	}

}

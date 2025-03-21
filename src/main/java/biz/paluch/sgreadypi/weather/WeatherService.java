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

import biz.paluch.sgreadypi.SgReadyProperties;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

/**
 * Service to determine how much good weather we can utilize based on sunset and weather forecast.
 *
 * @author Mark Paluch
 */
@Component
public class WeatherService {

	// cloud coverage in percent
	private static final int MAX_ACCEPTABLE_CLOUD_COVERAGE = 60;

	private final SgReadyProperties.Weather properties;
	private final Clock clock;
	private final GeoPosition position;
	private final WeatherClient client;
	private final SunPositionCalculator calculator;

	@Autowired
	public WeatherService(SgReadyProperties properties, RestTemplateBuilder builder, Clock clock) {
		this(properties.getWeather(), new WeatherClient(builder, clock), clock);
	}

	WeatherService(SgReadyProperties.Weather properties, WeatherClient weatherClient, Clock clock) {
		this.properties = properties;
		this.clock = clock;
		this.position = this.properties != null ? this.properties.getGeoPosition() : null;
		this.client = weatherClient;
		this.calculator = new SunPositionCalculator(clock);
	}

	/**
	 * @return the time range that can be used to consume the sun.
	 */
	public Range getUsableTimeRange() {

		WeatherState weatherState = getWeatherState();
		LocalDateTime sunset = getSunset();
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime beforeSunsetLimit = sunset.minus(properties.getNotBeforeSunset());

		boolean afterSunset = now.isAfter(sunset);
		boolean afterSunsetLimit = now.isAfter(beforeSunsetLimit);

		Duration remainingSun = getRemainingSunDuration(weatherState, now, beforeSunsetLimit);
		boolean enoughRemainingSunHours = remainingSun.compareTo(properties.getDesiredExcessDuration()) > 0;

		LocalDateTime from = enoughRemainingSunHours ? beforeSunsetLimit.minus(properties.getDesiredExcessDuration())
				: now.minusMinutes(1);
		return new Range(from, beforeSunsetLimit, afterSunset, afterSunsetLimit, enoughRemainingSunHours, remainingSun);
	}

	private static Duration getRemainingSunDuration(WeatherState weatherState, LocalDateTime now,
			LocalDateTime beforeSunsetLimit) {

		List<WeatherState.CloudCoverage> cloudCoverages = weatherState.cloudCoverage();
		List<Duration> sunnyTime = new ArrayList<>();

		LocalDateTime currentTime = now;
		for (int i = 0; i < cloudCoverages.size(); i++) {

			WeatherState.CloudCoverage coverage = cloudCoverages.get(i);
			WeatherState.CloudCoverage next;
			LocalDateTime from = currentTime;

			if (cloudCoverages.size() > i + 1) {
				next = cloudCoverages.get(i + 1);
			} else {
				next = null;
			}

			if (coverage.isBeforeOrEqual(from) && next != null && next.isBeforeOrEqual(from)) {
				continue;
			}

			double cov;

			if (next != null) {

				double thisPart = Duration.between(coverage.time(), from).toSeconds();
				double nextPart = Duration.between(from, next.time()).toSeconds();

				cov = (coverage.coverage() * (thisPart / 3600)) + (next.coverage() * (nextPart / 3600));
				currentTime = next.time();
			} else {
				cov = coverage.coverage();
				currentTime = beforeSunsetLimit;
			}

			LocalDateTime endBoundary = next != null ? next.time() : beforeSunsetLimit;
			boolean endReached = false;

			if (currentTime.isEqual(beforeSunsetLimit) || currentTime.isAfter(beforeSunsetLimit)
					|| endBoundary.isEqual(beforeSunsetLimit)) {
				endBoundary = beforeSunsetLimit;
				endReached = true;
			}

			if (cov < MAX_ACCEPTABLE_CLOUD_COVERAGE) {
				sunnyTime.add(Duration.between(from, endBoundary));
			}

			if (endReached) {
				break;
			}
		}

		Duration result = Duration.ZERO;

		for (Duration toAdd : sunnyTime) {
			result = result.plus(toAdd);
		}

		return result;
	}

	public WeatherState getWeatherState() {
		return client.getWeatherState(position);
	}

	public LocalDateTime getSunset() {
		return calculator.getSunset(position);
	}

	public record Range(LocalDateTime from, LocalDateTime to, boolean afterSunset, boolean afterSunsetLimit,
			boolean enoughRemainingSunHours, Duration remainingSunDuration) {

	}
}

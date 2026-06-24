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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Service to determine how much good weather we can utilize based on sunset and weather forecast.
 *
 * @author Mark Paluch
 */
@Component
public class WeatherService {

	private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

	// cloud coverage in percent
	private static final int MAX_ACCEPTABLE_CLOUD_COVERAGE = 60;

	// forecast is refreshed in the background once the cached state is older than this window
	private static final Duration CACHE_WINDOW = Duration.ofHours(4);

	private final SgReadyProperties.@Nullable Weather properties;
	private final Clock clock;
	private final @Nullable GeoPosition position;
	private final WeatherClient client;
	private final SunPositionCalculator calculator;

	private final Object refreshLock = new Object();
	private volatile @Nullable WeatherState weatherState;
	private volatile @Nullable Instant lastUpdate;

	@Autowired
	public WeatherService(SgReadyProperties properties, RestTemplateBuilder builder, Clock clock) {
		this(properties.getWeather(), new WeatherClient(builder, clock), clock);
	}

	WeatherService(SgReadyProperties.@Nullable Weather properties, WeatherClient weatherClient, Clock clock) {
		this.properties = properties;
		this.clock = clock;
		this.position = this.properties != null ? this.properties.getGeoPosition() : null;
		this.client = weatherClient;
		this.calculator = new SunPositionCalculator(clock);
	}

	/**
	 * Return the current weather state for the configured position.
	 * <p>
	 * Served from the cache without blocking once the forecast has been loaded. The very first access (cold cache) passes
	 * the request straight through to the {@link WeatherClient} rather than waiting for the background refresh, fetching
	 * under a lock so concurrent callers do not issue duplicate requests.
	 *
	 * @return the weather state; never {@literal null}.
	 * @throws RuntimeException if the forecast has never been loaded and the pass-through fetch fails.
	 */
	public WeatherState getWeatherState() {

		WeatherState weatherState = this.weatherState;
		if (weatherState != null) {
			return weatherState;
		}

		synchronized (refreshLock) {
			weatherState = this.weatherState;
			if (weatherState != null) {
				return weatherState;
			}
			return fetchAndCache(getRequiredPosition());
		}
	}

	/**
	 * Refresh the cached forecast in the background once it falls outside the {@link #CACHE_WINDOW}. Runs on the shared
	 * task scheduler; on failure the last-known forecast is retained so the control loop keeps deferring on the most
	 * recent data instead of losing weather optimisation.
	 */
	@Scheduled(fixedDelay = 4, timeUnit = TimeUnit.HOURS)
	void refreshWeather() {

		GeoPosition position = this.position;
		SgReadyProperties.Weather properties = this.properties;
		if (position == null || properties == null || !properties.isEnabled()) {
			return;
		}

		Instant lastUpdate = this.lastUpdate;
		if (lastUpdate != null && clock.instant().isBefore(lastUpdate.plus(CACHE_WINDOW))) {
			return;
		}

		try {
			synchronized (refreshLock) {
				fetchAndCache(position);
			}
		} catch (RuntimeException ex) {
			log.warn("Weather forecast refresh failed; continuing to serve the last-known forecast", ex);
		}
	}

	private WeatherState fetchAndCache(GeoPosition position) {

		WeatherState weatherState = client.getWeatherState(position);
		this.weatherState = weatherState;
		this.lastUpdate = clock.instant();
		return weatherState;
	}

	/**
	 * Return the sunset time for the configured position.
	 *
	 * @return the local sunset date-time; never {@literal null}.
	 */
	public LocalDateTime getSunset() {
		return calculator.getSunset(getRequiredPosition());
	}

	private SgReadyProperties.Weather getRequiredProperties() {

		SgReadyProperties.Weather properties = this.properties;
		if (properties == null) {
			throw new IllegalStateException("Weather optimization is not configured (requires sg.weather)");
		}
		return properties;
	}

	private GeoPosition getRequiredPosition() {

		GeoPosition position = this.position;
		if (position == null) {
			throw new IllegalStateException(
					"Weather optimization is not configured (requires sg.weather.latitude/longitude)");
		}
		return position;
	}

	/**
	 * Determine the usable time range during which excess solar power should be consumed.
	 *
	 * @return the usable time range, or {@literal null} when no forecast has been loaded yet. A {@literal null} result
	 *         lets the policy decide on state of charge alone, without weather deferral.
	 */
	public @Nullable Range getUsableTimeRange() {

		SgReadyProperties.Weather properties = getRequiredProperties();

		WeatherState weatherState;
		try {
			weatherState = getWeatherState();
		} catch (RuntimeException ex) {
			log.warn("Weather forecast unavailable; proceeding without weather deferral", ex);
			return null;
		}

		LocalDateTime sunset = getSunset();
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime usableLimit = sunset.minus(properties.getNotBeforeSunset());

		// additionally cap the window when the sun drops too low to cover the configured power (clear-sky geometry);
		// the window ends at whichever comes first, the not-before-sunset limit or the elevation cutoff.
		double minSunElevation = properties.getMinSunElevation();
		if (minSunElevation > 0) {
			LocalDateTime elevationCutoff = calculator.getDescendingElevationTime(getRequiredPosition(), minSunElevation);
			if (elevationCutoff.isBefore(usableLimit)) {
				usableLimit = elevationCutoff;
			}
		}

		boolean afterSunset = now.isAfter(sunset);
		boolean afterSunsetLimit = now.isAfter(usableLimit);

		Duration remainingSun = getRemainingSunDuration(weatherState, now, usableLimit);
		boolean enoughRemainingSunHours = remainingSun.compareTo(properties.getDesiredExcessDuration()) > 0;

		LocalDateTime from = enoughRemainingSunHours ? usableLimit.minus(properties.getDesiredExcessDuration())
				: now.minusMinutes(1);
		return new Range(from, usableLimit, sunset, afterSunset, afterSunsetLimit, enoughRemainingSunHours, remainingSun);
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
				// average cloud cover across the whole hour rather than weighting by where "now" falls within
				// it; assumes the hourly forecast resolution produced by WeatherClient.
				cov = (coverage.coverage() + next.coverage()) / 2.0;
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

			// guard against a negative interval once "now" has passed the sunset limit
			if (cov < MAX_ACCEPTABLE_CLOUD_COVERAGE && endBoundary.isAfter(from)) {
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

	/**
	 * The time window in which excess solar power may be consumed, with the sun-related boundaries and flags used to
	 * reach the decision.
	 *
	 * @param from the start of the usable window.
	 * @param to the end of the usable window: whichever comes first, the not-before-sunset limit or the sun-elevation
	 *          cutoff.
	 * @param sunset the calculated sunset time.
	 * @param afterSunset whether the current time is already past sunset.
	 * @param afterSunsetLimit whether the current time is already past the not-before-sunset limit.
	 * @param enoughRemainingSunHours whether enough sunny time remains to cover the desired excess duration.
	 * @param remainingSunDuration the forecast sunny time remaining until the sunset limit.
	 */
	public record Range(LocalDateTime from, LocalDateTime to, LocalDateTime sunset, boolean afterSunset,
			boolean afterSunsetLimit, boolean enoughRemainingSunHours, Duration remainingSunDuration) {

	}
}

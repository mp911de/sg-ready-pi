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

import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;
import net.e175.klaus.solarpositioning.SolarPosition;
import net.e175.klaus.solarpositioning.SunriseResult;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

/**
 * Calculates sunset times for a {@link GeoPosition} using the SPA algorithm.
 *
 * @author Mark Paluch
 */
class SunPositionCalculator {

	private final Clock clock;

	public SunPositionCalculator(Clock clock) {
		this.clock = clock;
	}

	/**
	 * Obtain the local time for the sunset at the given position.
	 *
	 * @param geoPosition the geographic position to calculate sunset for.
	 * @return the local sunset date-time, or the solar transit time when the sun does not set.
	 */
	public LocalDateTime getSunset(GeoPosition geoPosition) {

		var dateTime = clock.instant();

		double deltaT = DeltaT.estimate(dateTime.atZone(clock.getZone()).toLocalDate());
		SunriseResult result = SPA.calculateSunriseTransitSet(dateTime.atZone(clock.getZone()), geoPosition.latitude(),
				geoPosition.longitude(), deltaT, SPA.Horizon.SUNRISE_SUNSET);

		if (result instanceof SunriseResult.RegularDay rd) {
			return rd.sunset().toLocalDateTime();
		}

		return result.transit().toLocalDateTime();
	}

	/**
	 * Time at which the descending afternoon sun passes below {@code elevationDegrees} for the current day. Beyond this
	 * time the sun is too low to expect the corresponding plane-of-array power. Returns solar transit (noon) when the sun
	 * never climbs above the requested elevation that day (for example in winter), and the transit fallback on polar days
	 * without a regular sunset.
	 *
	 * @param geoPosition the position to evaluate; must not be {@literal null}.
	 * @param elevationDegrees the sun elevation threshold in degrees above the horizon.
	 * @return the local cutoff date-time; never {@literal null}.
	 */
	public LocalDateTime getDescendingElevationTime(GeoPosition geoPosition, double elevationDegrees) {

		ZonedDateTime now = clock.instant().atZone(clock.getZone());
		double deltaT = DeltaT.estimate(now.toLocalDate());

		SunriseResult result = SPA.calculateSunriseTransitSet(now, geoPosition.latitude(), geoPosition.longitude(), deltaT,
				SPA.Horizon.SUNRISE_SUNSET);

		if (!(result instanceof SunriseResult.RegularDay regularDay)) {
			return result.transit().toLocalDateTime();
		}

		ZonedDateTime transit = regularDay.transit();
		ZonedDateTime sunset = regularDay.sunset();

		if (elevationDegrees <= 0) {
			return sunset.toLocalDateTime();
		}
		if (elevationDegrees >= elevation(geoPosition, transit, deltaT)) {
			// the sun never reaches the threshold this day; treat the whole afternoon as below it
			return transit.toLocalDateTime();
		}

		// elevation decreases monotonically from transit to sunset; bisect for the crossing
		ZonedDateTime low = transit, high = sunset;
		for (int i = 0; i < 40; i++) {
			ZonedDateTime mid = low.plus(Duration.between(low, high).dividedBy(2));
			if (elevation(geoPosition, mid, deltaT) > elevationDegrees) {
				low = mid;
			} else {
				high = mid;
			}
		}
		return low.toLocalDateTime();
	}

	private static double elevation(GeoPosition geoPosition, ZonedDateTime time, double deltaT) {
		SolarPosition position = SPA.calculateSolarPosition(time, geoPosition.latitude(), geoPosition.longitude(), 0,
				deltaT);
		return 90 - position.zenithAngle();
	}
}

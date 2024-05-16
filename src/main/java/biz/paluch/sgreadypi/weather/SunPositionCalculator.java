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
import net.e175.klaus.solarpositioning.SunriseResult;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

/**
 * @author Mark Paluch
 */
class SunPositionCalculator {

	private final Clock clock;

	public SunPositionCalculator(Clock clock) {
		this.clock = clock;
	}

	/**
	 * Obtain the local time for the sunset.
	 *
	 * @param geoPosition
	 * @return
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
}

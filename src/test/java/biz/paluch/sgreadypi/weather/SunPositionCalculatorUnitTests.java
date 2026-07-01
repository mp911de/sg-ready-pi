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

import static org.assertj.core.api.Assertions.*;

import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;
import net.e175.klaus.solarpositioning.SolarPosition;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 */
class SunPositionCalculatorUnitTests {

	GeoPosition position = new GeoPosition(53.4138213, 7.1646956);

	@Test
	void shouldCalculateSunset() {
		new SunPositionCalculator(Clock.systemDefaultZone()).getSunset(position);
	}

	@Test
	void sunPositionMatchesLibraryComputation() {

		ZoneId zone = ZoneId.of("Europe/Berlin");
		Instant instant = Instant.parse("2026-06-21T10:00:00.00Z");
		Clock clock = Clock.fixed(instant, zone);

		SunPosition sunPosition = new SunPositionCalculator(clock).getSunPosition(position);

		double deltaT = DeltaT.estimate(instant.atZone(zone).toLocalDate());
		SolarPosition expected = SPA.calculateSolarPosition(instant.atZone(zone), position.latitude(), position.longitude(),
				0, deltaT);

		assertThat(sunPosition.azimuth()).isCloseTo(expected.azimuth(), within(1.0));
		assertThat(sunPosition.elevation()).isCloseTo(90 - expected.zenithAngle(), within(1.0));
		// midmorning midsummer sun at 53 N is well above the horizon
		assertThat(sunPosition.elevation()).isGreaterThan(0);
	}

	@Test
	void descendingElevationTimeMatchesRequestedElevation() {

		ZoneId zone = ZoneId.of("Europe/Berlin");
		Clock clock = Clock.fixed(Instant.parse("2026-06-21T10:00:00.00Z"), zone);
		SunPositionCalculator calculator = new SunPositionCalculator(clock);

		LocalDateTime cutoff = calculator.getDescendingElevationTime(position, 30);

		assertThat(cutoff).isBefore(calculator.getSunset(position));
		assertThat(cutoff.toLocalTime()).isAfter(LocalTime.NOON);

		double deltaT = DeltaT.estimate(cutoff.toLocalDate());
		SolarPosition at = SPA.calculateSolarPosition(cutoff.atZone(zone), position.latitude(), position.longitude(), 0,
				deltaT);
		assertThat(90 - at.zenithAngle()).isCloseTo(30, within(0.5));
	}

	@Test
	void returnsTransitWhenSunNeverReachesElevation() {

		ZoneId zone = ZoneId.of("Europe/Berlin");
		Clock clock = Clock.fixed(Instant.parse("2026-12-21T10:00:00.00Z"), zone);
		SunPositionCalculator calculator = new SunPositionCalculator(clock);

		// midwinter noon sun at 53 N stays around ~13 deg, well below 30 deg
		LocalDateTime cutoff = calculator.getDescendingElevationTime(position, 30);

		assertThat(cutoff.toLocalTime()).isBetween(LocalTime.of(11, 0), LocalTime.of(13, 30));
	}
}

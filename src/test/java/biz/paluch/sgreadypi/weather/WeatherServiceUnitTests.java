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
import static org.mockito.Mockito.*;

import biz.paluch.sgreadypi.SgReadyProperties;
import biz.paluch.sgreadypi.weather.WeatherState.CloudCoverage;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link WeatherService}.
 *
 * @author Mark Paluch
 */
@MockitoSettings(strictness = Strictness.LENIENT)
class WeatherServiceUnitTests {

	@Mock WeatherClient client;

	SgReadyProperties.Weather properties = new SgReadyProperties.Weather();

	@BeforeEach
	void setUp() {
		properties.setEnabled(true);
		properties.setDesiredExcessDuration(Duration.ofMinutes(180));
		properties.setNotBeforeSunset(Duration.ofHours(3));
	}

	@Test
	void shouldConsiderAllTimeSunny() {

		Clock clock = Clock.fixed(Instant.parse("2007-12-03T09:15:30.00Z"), ZoneId.of("Europe/Paris"));
		WeatherService service = new WeatherService(properties, client, clock);

		WeatherState state = new WeatherState(0, 0,
				List.of(new CloudCoverage(LocalDateTime.parse("2007-12-03T10:00:00.00"), 0),
						new CloudCoverage(LocalDateTime.parse("2007-12-03T11:00:00.00"), 0)));

		LocalDateTime sunset = service.getSunset().minusHours(3);

		Duration duration = Duration.between(LocalDateTime.now(clock), sunset);
		when(client.getWeatherState(any())).thenReturn(state);

		WeatherService.Range usableTimeRange = service.getUsableTimeRange();
		assertThat(usableTimeRange.remainingSunDuration()).isEqualTo(duration);
	}

	@Test
	void shouldConsiderPartTimeSunny() {

		Clock clock = Clock.fixed(Instant.parse("2007-12-03T09:15:30.00Z"), ZoneId.of("Europe/Paris"));
		WeatherService service = new WeatherService(properties, client, clock);

		WeatherState state = new WeatherState(0, 0,
				List.of(new CloudCoverage(LocalDateTime.parse("2007-12-03T10:00:00.00"), 0),
						new CloudCoverage(LocalDateTime.parse("2007-12-03T11:00:00.00"), 100),
						new CloudCoverage(LocalDateTime.parse("2007-12-03T12:00:00.00"), 0)));

		when(client.getWeatherState(any())).thenReturn(state);

		WeatherService.Range usableTimeRange = service.getUsableTimeRange();
		assertThat(usableTimeRange.remainingSunDuration().toString()).contains("4H53M");
		assertThat(usableTimeRange.enoughRemainingSunHours()).isTrue();
	}

	@Test
	void shouldConsiderMostlyCloudy() {

		Clock clock = Clock.fixed(Instant.parse("2007-12-03T12:15:30.00Z"), ZoneId.of("Europe/Paris"));
		WeatherService service = new WeatherService(properties, client, clock);

		WeatherState state = new WeatherState(0, 0,
				List.of(new CloudCoverage(LocalDateTime.parse("2007-12-03T12:00:00.00"), 0),
						new CloudCoverage(LocalDateTime.parse("2007-12-03T13:00:00.00"), 100),
						new CloudCoverage(LocalDateTime.parse("2007-12-03T14:00:00.00"), 0)));

		when(client.getWeatherState(any())).thenReturn(state);

		WeatherService.Range usableTimeRange = service.getUsableTimeRange();
		assertThat(usableTimeRange.remainingSunDuration().toString()).contains("2H37M");
		assertThat(usableTimeRange.enoughRemainingSunHours()).isFalse();
	}
}

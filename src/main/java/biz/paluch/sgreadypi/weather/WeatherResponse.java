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

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Mark Paluch
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
class WeatherResponse {

	@JsonProperty("timezone_abbreviation") String timeZone;

	CurrentWeather current;

	HourlyWeather hourly;

	@JsonIgnoreProperties(ignoreUnknown = true)
	static class CurrentWeather {

		@JsonProperty("pressure_msl") double pressure;

		@JsonProperty("temperature") double temperature;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	static class HourlyWeather {

		@JsonProperty("time") List<LocalDateTime> time;

		@JsonProperty("cloud_cover_low") List<Integer> cloudLow;
		@JsonProperty("cloud_cover_mid") List<Integer> cloudMid;

	}
}

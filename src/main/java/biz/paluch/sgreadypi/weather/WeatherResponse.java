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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson binding for the Open-Meteo forecast response.
 *
 * @author Mark Paluch
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("NullAway.Init") // fields are populated by Jackson deserialization
class WeatherResponse {

	@JsonProperty("timezone_abbreviation") String timeZone;

	CurrentWeather current;

	HourlyWeather hourly;

	public WeatherResponse() {}

	public String getTimeZone() {
		return this.timeZone;
	}

	public CurrentWeather getCurrent() {
		return this.current;
	}

	public HourlyWeather getHourly() {
		return this.hourly;
	}

	@JsonProperty("timezone_abbreviation")
	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public void setCurrent(CurrentWeather current) {
		this.current = current;
	}

	public void setHourly(HourlyWeather hourly) {
		this.hourly = hourly;
	}

	public String toString() {
		return "WeatherResponse(timeZone=" + this.getTimeZone() + ", current=" + this.getCurrent() + ", hourly="
				+ this.getHourly() + ")";
	}

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

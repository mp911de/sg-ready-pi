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

/**
 * Snapshot of weather relevant to PV yield: current temperature and pressure plus an hourly cloud-coverage forecast.
 *
 * @param temperature the current temperature in degrees Celsius.
 * @param pressure the current pressure in hPa.
 * @param cloudCoverage the hourly cloud-coverage forecast.
 * @author Mark Paluch
 */
public record WeatherState(int temperature, int pressure, List<CloudCoverage> cloudCoverage) {

	record CloudCoverage(LocalDateTime time, int coverage) {

		public boolean isBeforeOrEqual(LocalDateTime compareTo) {
			return time.isBefore(compareTo) || time.isEqual(compareTo);
		}
	}
}

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

import org.junit.jupiter.api.Test;

import org.springframework.boot.web.client.RestTemplateBuilder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mark Paluch
 */
class WeatherClientIntegrationTests {

	@Test
	void apiCall() {

		WeatherClient client = new WeatherClient(new RestTemplateBuilder(), Clock.systemDefaultZone());
		WeatherState weatherState = client.getWeatherState(new GeoPosition(53.4138213, 7.1646956));

		System.out.println(weatherState);
	}

}

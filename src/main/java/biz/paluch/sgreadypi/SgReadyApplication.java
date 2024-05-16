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
package biz.paluch.sgreadypi;

import biz.paluch.sgreadypi.output.SgReadyStateConsumer;
import biz.paluch.sgreadypi.provider.SunnyHomeManagerService;
import biz.paluch.sgreadypi.weather.WeatherService;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entry point.
 */
@SpringBootApplication
@EnableConfigurationProperties(SgReadyProperties.class)
@EnableScheduling
public class SgReadyApplication {

	public static void main(String[] args) {

		SpringApplication application = new SpringApplication(SgReadyApplication.class);

		if (System.getProperties().containsKey("PIDFILE")) {
			application.addListeners(new ApplicationPidFileWriter());
		}

		application.run(args);
	}

	@Bean
	Clock clock() {
		return Clock.systemDefaultZone();
	}

	@Bean
	public SgReadyControlLoop controlLoop(PowerGeneratorService inverters, SunnyHomeManagerService powerMeter,
			SgReadyStateConsumer stateConsumer, SgReadyProperties properties, WeatherService weatherService, Clock clock) {
		return new SgReadyControlLoop(inverters, powerMeter, stateConsumer, properties, weatherService, clock);
	}

}

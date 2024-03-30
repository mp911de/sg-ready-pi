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
package biz.paluch.sgreadypi.output.gpio;

import biz.paluch.sgreadypi.SgReadyProperties;
import biz.paluch.sgreadypi.output.DebounceStateConsumer;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;

/**
 * GPIO configuration.
 *
 * @author Mark Paluch
 */
@Configuration(proxyBeanMethods = false)
public class GpioConfiguration {

	@Bean
	@ConditionalOnProperty("sg.gpio.rpi3-ch.pin-a")
	PiRelHat3Ch piRelHat3Ch(Context context, SgReadyProperties properties) {

		GpioProperties.Rpi3Ch rpi3Ch = properties.getGpio().rpi3Ch();

		return new PiRelHat3Ch(context, rpi3Ch.pinA(), rpi3Ch.pinB());
	}
	@Bean
	@Primary
	DebounceStateConsumer debounce(PiRelHat3Ch relay, TaskScheduler scheduler) {
		return new DebounceStateConsumer(relay, scheduler, Duration.ofMinutes(1));
	}

	@Bean(destroyMethod = "shutdown")
	Context context(SgReadyProperties properties) {
		return Pi4J.newAutoContextAllowMocks();
	}
}

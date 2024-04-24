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
import biz.paluch.sgreadypi.output.CompositeSgReadyStateConsumer;
import biz.paluch.sgreadypi.output.ConditionalOnRaspberryPi;
import biz.paluch.sgreadypi.output.DebounceStateConsumer;
import biz.paluch.sgreadypi.output.SgReadyStateConsumer;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
@Slf4j
@Import({ GpioConfiguration.RaspberryPi.class })
public class GpioConfiguration {

	@ConditionalOnRaspberryPi
	@Bean(destroyMethod = "shutdown")
	Context context(SgReadyProperties properties) {
		return Pi4J.newContext();
	}

	@Conditional(NotOnRaspberryPi.class)
	@Bean(destroyMethod = "shutdown")
	Context mockContext(SgReadyProperties properties) {
		return Pi4J.newAutoContextAllowMocks();
	}

	/**
	 * Debouncer to avoid relay wear due to hysteresis.
	 */
	@Bean
	@Primary
	DebounceStateConsumer debounce(ObjectProvider<SgReadyStateConsumer> stateConsumers, TaskScheduler scheduler,
			SgReadyProperties properties) {

		List<SgReadyStateConsumer> list = stateConsumers.stream().toList();

		return new DebounceStateConsumer(new CompositeSgReadyStateConsumer(list), scheduler, properties.getDebounce());
	}

	@Configuration(proxyBeanMethods = false)
	static class RaspberryPi {

		@Bean
		@ConditionalOnProperty("sg.gpio.rpi3-ch.pin-a")
		PiRelHat3Ch piRelHat3Ch(MeterRegistry meterRegistry, Context context, SgReadyProperties properties) {

			GpioProperties.Rpi3Ch rpi3Ch = properties.getGpio().rpi3Ch();

			return new PiRelHat3Ch(meterRegistry, context, rpi3Ch.pinA(), rpi3Ch.pinB(), rpi3Ch.pinC());
		}

		@Bean
		RelayHealthIndicator relayHealthIndicator(PiRelHat3Ch relay) {
			return new RelayHealthIndicator(relay);
		}

		@Bean
		RelayController relayController(PiRelHat3Ch piRelHat3Ch) {
			return new RelayController(piRelHat3Ch);
		}

	}

	static class NotOnRaspberryPi extends NoneNestedConditions {

		public NotOnRaspberryPi() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnRaspberryPi
		static class OnRaspberryPi {

		}
	}

}

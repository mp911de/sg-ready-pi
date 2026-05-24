/*
 * Copyright 2026 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import biz.paluch.sgreadypi.SgReadyProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

/**
 * Unit tests for {@link GpioConfiguration}.
 *
 * @author Mark Paluch
 */
class GpioConfigurationUnitTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class);

	@Test
	void shouldUseNoOpRelayWhenGpioPinsAreMissing() {

		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(Relay.class);
			assertThat(context).hasSingleBean(NoOpRelay.class);
			assertThat(context).doesNotHaveBean(PiRelHat3Ch.class);
			assertThat(context).doesNotHaveBean(RelayController.class);
			assertThat(context).doesNotHaveBean(RelayHealthIndicator.class);
		});
	}

	@Test
	void shouldUseNoOpRelayWhenGpioPinsArePartial() {

		contextRunner.withPropertyValues("sg.gpio.rpi3-ch.pin-a=1").run(context -> {
			assertThat(context).hasSingleBean(Relay.class);
			assertThat(context).hasSingleBean(NoOpRelay.class);
			assertThat(context).doesNotHaveBean(PiRelHat3Ch.class);
			assertThat(context).doesNotHaveBean(RelayController.class);
			assertThat(context).doesNotHaveBean(RelayHealthIndicator.class);
		});
	}

	@Test
	void shouldConfigureRelayGraphWhenAllGpioPinsArePresent() {

		contextRunner.withPropertyValues("sg.gpio.rpi3-ch.pin-a=1", "sg.gpio.rpi3-ch.pin-b=2", "sg.gpio.rpi3-ch.pin-c=3")
				.run(context -> {
					assertThat(context).hasSingleBean(Relay.class);
					assertThat(context).hasSingleBean(PiRelHat3Ch.class);
					assertThat(context).hasSingleBean(RelayController.class);
					assertThat(context).hasSingleBean(RelayHealthIndicator.class);
					assertThat(context).doesNotHaveBean(NoOpRelay.class);
				});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(SgReadyProperties.class)
	@Import(GpioConfiguration.class)
	static class TestConfiguration {

		@Bean
		TaskScheduler taskScheduler() {
			return new ConcurrentTaskScheduler();
		}

		@Bean
		MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

	}

}

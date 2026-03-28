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

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Health.Builder;
import org.springframework.boot.health.contributor.HealthIndicator;

import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;

/**
 * @author Mark Paluch
 */
record RelayHealthIndicator(PiRelHat3Ch relay) implements HealthIndicator {

	@Override
	public Health health() {

		Builder builder = Health.up();

		builder.withDetail("ch1", toString(relay.getCh1()));
		builder.withDetail("ch2", toString(relay.getCh2()));
		builder.withDetail("ch3", toString(relay.getCh3()));

		return builder.build();
	}

	private static String toString(DigitalOutput relay) {
		return relay.state() == DigitalState.LOW ? "closed" : "open";
	}
}

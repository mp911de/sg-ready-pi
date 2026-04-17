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
package biz.paluch.sgreadypi;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RecencyTracker}.
 *
 * @author Mark Paluch
 */
class RecencyTrackerUnitTests {

	@Test
	void shouldReportOutOfServiceWithoutData() {

		SampleRecencyTracker sample = new SampleRecencyTracker(Duration.ZERO, false);

		RecencyTracker.HealthState healthState = sample.getHealthState();
		assertThat(healthState).isEqualTo(RecencyTracker.HealthState.OUT_OF_SERVICE);
		assertThat(healthState.isHealthy()).isFalse();
		assertThat(healthState.isDegraded()).isFalse();
		assertThat(healthState.isOutOfService()).isTrue();
	}

	@Test
	void shouldReportHealthyAtHealthyThreshold() {

		SampleRecencyTracker sample = new SampleRecencyTracker(RecencyTracker.MAX_HEALTHY, true);

		RecencyTracker.HealthState healthState = sample.getHealthState();
		assertThat(healthState).isEqualTo(RecencyTracker.HealthState.HEALTHY);
		assertThat(healthState.isHealthy()).isTrue();
	}

	@Test
	void shouldReportDegradedBetweenThresholds() {

		SampleRecencyTracker sample = new SampleRecencyTracker(Duration.ofMinutes(5), true);

		RecencyTracker.HealthState healthState = sample.getHealthState();
		assertThat(healthState).isEqualTo(RecencyTracker.HealthState.DEGRADED);
		assertThat(healthState.isDegraded()).isTrue();
		assertThat(healthState.isHealthy()).isFalse();
		assertThat(healthState.isOutOfService()).isFalse();
	}

	@Test
	void shouldReportOutOfServiceAtOutOfServiceThreshold() {

		SampleRecencyTracker sample = new SampleRecencyTracker(RecencyTracker.OUT_OF_SERVICE, true);

		RecencyTracker.HealthState healthState = sample.getHealthState();
		assertThat(healthState).isEqualTo(RecencyTracker.HealthState.OUT_OF_SERVICE);
	}

	@Test
	void shouldUseCustomThresholds() {

		SampleRecencyTracker sample = new SampleRecencyTracker(Duration.ofMinutes(4), true, Duration.ofMinutes(2),
				Duration.ofMinutes(5));

		assertThat(sample.getHealthState()).isEqualTo(RecencyTracker.HealthState.DEGRADED);

		sample = new SampleRecencyTracker(Duration.ofMinutes(1), true, Duration.ofMinutes(2), Duration.ofMinutes(5));
		assertThat(sample.getHealthState()).isEqualTo(RecencyTracker.HealthState.HEALTHY);

		sample = new SampleRecencyTracker(Duration.ofMinutes(5), true, Duration.ofMinutes(2), Duration.ofMinutes(5));
		assertThat(sample.getHealthState()).isEqualTo(RecencyTracker.HealthState.OUT_OF_SERVICE);
	}

	@Test
	void shouldRejectInvalidThresholdConfiguration() {

		SampleRecencyTracker sample = new SampleRecencyTracker(Duration.ofMinutes(3), true, Duration.ofMinutes(5),
				Duration.ofMinutes(2));

		assertThatIllegalStateException().isThrownBy(sample::getHealthState).withMessageContaining("Healthy threshold");
	}

	record SampleRecencyTracker(Duration dataAge, boolean hasData, Duration healthy,
			Duration outOfService) implements RecencyTracker {

		SampleRecencyTracker(Duration dataAge, boolean hasData) {
			this(dataAge, hasData, MAX_HEALTHY, OUT_OF_SERVICE);
		}

		@Override
		public Duration getHealthyAgeThreshold() {
			return this.healthy;
		}

		@Override
		public Duration getOutOfServiceAgeThreshold() {
			return this.outOfService;
		}
	}

}

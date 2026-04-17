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

import java.time.Duration;

/**
 * Components implementing this interface expose health information based on data age (i.e. since the last update).
 *
 * @author Mark Paluch
 */
public interface RecencyTracker {

	Duration MAX_HEALTHY = Duration.ofMinutes(1);

	Duration OUT_OF_SERVICE = Duration.ofMinutes(10);

	/**
	 * Health state derived from data recency.
	 */
	enum HealthState {

		HEALTHY, DEGRADED, OUT_OF_SERVICE;

		/**
		 * Return {@code true} if the service is healthy based on data recency.
		 *
		 * @return {@code true} if healthy; {@code false} otherwise.
		 */
		public boolean isHealthy() {
			return this == HealthState.HEALTHY;
		}

		/**
		 * Return {@code true} if the service is degraded based on data recency.
		 *
		 * @return {@code true} if degraded; {@code false} otherwise.
		 */
		public boolean isDegraded() {
			return this == HealthState.DEGRADED;
		}

		/**
		 * Return {@code true} if the service is out of service based on data recency.
		 *
		 * @return {@code true} if out of service; {@code false} otherwise.
		 */
		public boolean isOutOfService() {
			return this == HealthState.OUT_OF_SERVICE;
		}
	}

	/**
	 * Return whether this component has already received data updates.
	 *
	 * @return {@code true} if data has been received; {@code false} otherwise.
	 */
	default boolean hasData() {
		return true;
	}

	/**
	 * Return the duration since the last update.
	 * <p>
	 * Data that has just been updated may return {@link Duration#ZERO}. Data that has not yet been updated should be
	 * indicated through {@link #hasData()}.
	 *
	 * @return the duration since the last update.
	 */
	Duration dataAge();

	/**
	 * Return the threshold up to which data is considered healthy.
	 *
	 * @return the healthy data age threshold.
	 */
	default Duration getHealthyAgeThreshold() {
		return MAX_HEALTHY;
	}

	/**
	 * Return the threshold from which data is considered out of service.
	 *
	 * @return the out-of-service data age threshold.
	 */
	default Duration getOutOfServiceAgeThreshold() {
		return OUT_OF_SERVICE;
	}

	/**
	 * Return the current {@link HealthState} based on data recency.
	 * <p>
	 * The default classification uses the following boundary rules:
	 * <ul>
	 * <li>{@link HealthState#HEALTHY}: {@code dataAge <= healthyThreshold}.</li>
	 * <li>{@link HealthState#OUT_OF_SERVICE}: {@code dataAge >= outOfServiceThreshold}.</li>
	 * <li>{@link HealthState#DEGRADED}: values between both thresholds.</li>
	 * </ul>
	 *
	 * @return the derived health state.
	 */
	default HealthState getHealthState() {

		if (!hasData()) {
			return HealthState.OUT_OF_SERVICE;
		}

		Duration healthyThreshold = getHealthyAgeThreshold();
		Duration outOfServiceThreshold = getOutOfServiceAgeThreshold();

		if (healthyThreshold.compareTo(outOfServiceThreshold) > 0) {
			throw new IllegalStateException("Healthy threshold must be less than or equal to out-of-service threshold");
		}

		Duration dataAge = dataAge();

		if (dataAge.compareTo(healthyThreshold) <= 0) {
			return HealthState.HEALTHY;
		}

		if (dataAge.compareTo(outOfServiceThreshold) >= 0) {
			return HealthState.OUT_OF_SERVICE;
		}

		return HealthState.DEGRADED;
	}

}

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

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator displaying the SG ready state and underlying values.
 *
 * @author Mark Paluch
 */
@Component
public class SgReadyHealthIndicator implements HealthIndicator {

	private final SgReadyControlLoop controller;

	public SgReadyHealthIndicator(SgReadyControlLoop controller) {
		this.controller = controller;
	}

	@Override
	public Health health() {

		Health.Builder builder = Health.up();
		builder.withDetail("sg-ready", controller.getState().toString());
		Decision decision = controller.getDecision();

		if (decision != null) {
			builder.withDetail("sg-ready-decision", decision.conditionOutcome().explain());
		}

		return builder.build();
	}

	public SgReadyControlLoop getController() {
		return this.controller;
	}

	public String toString() {
		return "SgReadyHealthIndicator(controller=" + this.getController() + ")";
	}
}

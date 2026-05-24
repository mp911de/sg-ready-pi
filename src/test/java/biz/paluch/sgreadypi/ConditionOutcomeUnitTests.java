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

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ConditionOutcome}.
 *
 * @author Mark Paluch
 */
class ConditionOutcomeUnitTests {

	@Test
	void shouldExplainSingleOutcome() {

		ConditionOutcome outcome = ConditionOutcome.match("Generator power above consumption");

		assertThat(outcome.explain()).containsExactly("Did match: Generator power above consumption");
	}

	@Test
	void shouldExplainNestedTrailRootFirst() {

		ConditionOutcome outcome = ConditionOutcome.match("Generator power above consumption")
				.nestedMatch("SoC above available threshold").nestedNoMatch("SoC below excess start threshold");

		assertThat(outcome.explain()).containsExactly( //
				"Did match: Generator power above consumption", //
				"Did match: SoC above available threshold", //
				"Did not match: SoC below excess start threshold");
	}
}

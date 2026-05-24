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

/**
 * The chosen {@link SgReadyState} together with the {@link ConditionOutcome} chain explaining how it was reached.
 *
 * @param state the chosen SG Ready state.
 * @param conditionOutcome the reasoning trail behind the state.
 * @author Mark Paluch
 */
public record Decision(SgReadyState state, ConditionOutcome conditionOutcome) {

	/**
	 * A decision resulting in {@link SgReadyState#NORMAL}.
	 *
	 * @param outcome
	 * @return
	 */
	static Decision normal(ConditionOutcome outcome) {
		return new Decision(SgReadyState.NORMAL, outcome);
	}

	/**
	 * A decision resulting in {@link SgReadyState#AVAILABLE_PV}.
	 *
	 * @param outcome
	 * @return
	 */
	static Decision availablePv(ConditionOutcome outcome) {
		return new Decision(SgReadyState.AVAILABLE_PV, outcome);
	}

	/**
	 * A decision resulting in {@link SgReadyState#EXCESS_PV}.
	 *
	 * @param outcome
	 * @return
	 */
	static Decision excessPv(ConditionOutcome outcome) {
		return new Decision(SgReadyState.EXCESS_PV, outcome);
	}

}

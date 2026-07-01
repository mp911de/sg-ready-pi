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

import javax.measure.Quantity;

/**
 * Schmitt-trigger style hysteresis for threshold comparisons. A gate stays active until the value drops below the lower
 * {@code off} threshold, and requires the higher {@code on} threshold to switch on again. This avoids flickering when a
 * value hovers around a single threshold. The previous active state is supplied by the caller (here derived from the
 * previously signalled {@link SgReadyState}).
 *
 * @author Mark Paluch
 * @see SgReadyPolicy
 */
class Hysteresis {

	private Hysteresis() {}

	/**
	 * Evaluate a hysteretic threshold gate.
	 *
	 * @param <Q> the quantity dimension being compared.
	 * @param active whether the gate is currently considered active (the previous outcome).
	 * @param value the current value to test.
	 * @param on the upper threshold required to switch the gate on and should be greater than or equal to {@code off}.
	 * @param off the lower threshold at or below which an active gate switches off.
	 * @return {@literal true} if the gate should be active for {@code value}; {@literal false} otherwise.
	 */
	static <Q extends Quantity<Q>> boolean active(boolean active, Quantity<Q> value, Quantity<Q> on, Quantity<Q> off) {
		return SgReadyPolicy.gte(value, active ? off : on);
	}

}

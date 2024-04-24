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
package biz.paluch.sgreadypi.measure;

import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Power;

/**
 * Watt quantity factory.
 *
 * @author Mark Paluch
 */
public class Watt {

	private static final Quantity<Power> ZERO = of(0);

	/**
	 * Create Quantity of Power from {@code watt}.
	 *
	 * @param watts watt of power.
	 * @return
	 */
	public static Quantity<Power> of(int watts) {
		return Quantities.getQuantity(watts, Units.WATT);
	}

	/**
	 * @return zero watt quantity.
	 */
	public static Quantity<Power> zero() {
		return ZERO;
	}
}

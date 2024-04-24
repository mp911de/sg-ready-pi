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
import javax.measure.quantity.Dimensionless;

/**
 * Percent quantity factory.
 *
 * @author Mark Paluch
 */
public class Percent {

	private static final Quantity<Dimensionless> ZERO = of(0);

	/**
	 * Create Quantity from percentage {@code value}.
	 *
	 * @param value percentage value.
	 * @return
	 */
	public static Quantity<Dimensionless> of(int value) {
		return Quantities.getQuantity(value, Units.PERCENT);
	}

	/**
	 * @return zero percent quantity.
	 */
	public static Quantity<Dimensionless> zero() {
		return ZERO;
	}
}

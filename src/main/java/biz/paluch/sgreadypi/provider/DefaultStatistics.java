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
package biz.paluch.sgreadypi.provider;

import tech.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.Unit;
import java.time.Duration;

/**
 * @author Mark Paluch
 */
class DefaultStatistics<Q extends Quantity<Q>> implements MutableStatistics<Q> {

	private final Average average;
	private final Unit<Q> unit;

	private volatile Quantity<Q> mostRecent;

	public DefaultStatistics(Duration duration, Unit<Q> unit) {
		average = new Average(duration);
		this.unit = unit;
	}

	@Override
	public Quantity<Q> getAverage() {
		return Quantities.getQuantity(Math.round(average.getAverage()), unit);
	}

	@Override
	public Quantity<Q> getMostRecent() {
		return mostRecent == null ? Quantities.getQuantity(0, unit) : mostRecent;
	}

	@Override
	public void update(Quantity<Q> value) {
		mostRecent = value;
		average.add(value.getValue().doubleValue());
	}
}

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

import java.time.Duration;

import javax.measure.Quantity;
import javax.measure.Unit;

/**
 * Allows mutation of the {@link Statistics}.
 * 
 * @author Mark Paluch
 */
public interface MutableStatistics<Q extends Quantity<Q>> extends Statistics<Q> {

	/**
	 * Create a new {@link MutableStatistics}.
	 * 
	 * @param <Q> the quantity type.
	 * @param duration the averaging window.
	 * @param unit the unit of the tracked quantity.
	 * @return a new mutable statistics instance.
	 */
	static <Q extends Quantity<Q>> MutableStatistics<Q> create(Duration duration, Unit<Q> unit) {
		return new DefaultStatistics<>(duration, unit);
	}

	/**
	 * Provide a new {@code value} to the average.
	 * 
	 * @param value the new reading to record.
	 */
	void update(Quantity<Q> value);
}

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

import javax.measure.Quantity;

/**
 * Interface providing access to value statistics and the most recent one.
 *
 * @author Mark Paluch
 */
public interface Statistics<Q extends Quantity<Q>> {

	static <Q extends Quantity<Q>> Statistics<Q> just(Quantity<Q> q) {
		return new Statistics<>() {
			@Override
			public Quantity<Q> getAverage() {
				return q;
			}

			@Override
			public Quantity<Q> getMostRecent() {
				return q;
			}
		};
	}

	/**
	 * @return the average value.
	 */
	Quantity<Q> getAverage();

	/**
	 * @return the most recent value.
	 */
	Quantity<Q> getMostRecent();
}

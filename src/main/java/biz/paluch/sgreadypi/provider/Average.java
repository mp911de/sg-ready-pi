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
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * Utility to calculate a moving average for a {@link Duration time window} of data points.
 * 
 * @author Mark Paluch
 */
class Average {

	private final LinkedList<DataPoint> dataPoints = new LinkedList<>();

	private final Duration window;

	public Average(Duration window) {
		this.window = window;
	}

	public void add(double value) {
		synchronized (dataPoints) {
			Instant now = Instant.now();
			Instant limit = now.minus(window);
			dataPoints.add(new DataPoint(now, value));

			Iterator<DataPoint> di = dataPoints.descendingIterator();

			while (di.hasNext()) {
				DataPoint dp = di.next();

				if (dp.time.isBefore(limit)) {
					di.remove();
				}
			}
		}
	}

	public double getAverage() {

		DoubleAdder adder = new DoubleAdder();
		synchronized (dataPoints) {
			dataPoints.forEach(it -> adder.add(it.value));
			return adder.doubleValue() / dataPoints.size();
		}
	}

	record DataPoint(Instant time, double value) {

	}
}

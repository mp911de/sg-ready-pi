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

import biz.paluch.sgreadypi.measure.Percent;
import biz.paluch.sgreadypi.measure.Watt;
import biz.paluch.sgreadypi.provider.Statistics;

import javax.measure.Quantity;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Power;

/**
 * Power generator service (battery, inverter, …) that generates power.
 *
 * @author Mark Paluch
 */
public interface PowerGeneratorService {

	/**
	 * @return percentage of battery charge state.
	 */
	default Quantity<Dimensionless> getBatteryStateOfCharge() {
		return Percent.zero();
	}

	/**
	 * @return available generator power in {@link tech.units.indriya.unit.Units#WATT}.
	 */
	default Statistics<Power> getGeneratorPower() {
		return Statistics.just(Watt.zero());
	}

	/**
	 * @return {@code true} if the service is alive and has recent data.
	 */
	boolean hasData();

	/**
	 * @return {@code true} if the service is out of service (i.e. degraded state exceeded).
	 */
	boolean isOutOfService();

}

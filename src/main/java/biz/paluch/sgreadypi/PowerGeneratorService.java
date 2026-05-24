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
	 * Return the aggregate battery state of charge across all inverters.
	 *
	 * @return the battery state of charge as a percentage; never {@literal null}.
	 */
	default Quantity<Dimensionless> getBatteryStateOfCharge() {
		return Percent.zero();
	}

	/**
	 * Return the generator power statistics, that is the usable solar surplus.
	 *
	 * @return generator power statistics in {@link tech.units.indriya.unit.Units#WATT}; never {@literal null}.
	 */
	default Statistics<Power> getGeneratorPower() {
		return Statistics.just(Watt.zero());
	}

	/**
	 * Return whether the service is alive and holds recent data.
	 *
	 * @return {@literal true} if recent data is available; {@literal false} otherwise.
	 */
	boolean hasData();

	/**
	 * Return whether the service is out of service because its data age exceeded the degraded threshold.
	 *
	 * @return {@literal true} if out of service; {@literal false} otherwise.
	 */
	boolean isOutOfService();

}

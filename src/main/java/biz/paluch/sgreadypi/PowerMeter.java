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

import javax.measure.Quantity;
import javax.measure.quantity.Power;

/**
 * Power meter interface providing power ingress from an external power grid.
 *
 * @author Mark Paluch
 */
public interface PowerMeter {

	/**
	 * @return averaged power ingress from an external power grid.
	 */
	Quantity<Power> getIngress();

	/**
	 * @return momentary power ingress from an external power grid.
	 */
	Quantity<Power> getMomentaryIngress();

	/**
	 * @return averaged power egress to an external power grid.
	 */
	Quantity<Power> getEgress();

	/**
	 * @return momentary power egress to an external power grid.
	 */
	Quantity<Power> getMomentaryEgress();

	/**
	 * @return {@code true} if the service is alive and has recent data.
	 */
	boolean hasData();

}

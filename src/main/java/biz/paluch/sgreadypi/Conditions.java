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
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Power;

/**
 * Snapshot of the sensed inputs a {@link Decision} is made from: power {@code ingress} from the grid, the solar
 * {@code generatorPower} surplus, the battery {@code soc} (state of charge), the net {@code batteryDischarge}, and
 * whether any input is {@code outOfService}.
 *
 * @param ingress power drawn from the grid.
 * @param generatorPower usable solar surplus.
 * @param soc battery state of charge.
 * @param batteryDischarge net power drawn from the batteries (discharging minus charging); negative while charging
 *          dominates.
 * @param outOfService whether the power generator or power meter data is too stale to trust.
 * @author Mark Paluch
 */
public record Conditions(Quantity<Power> ingress, Quantity<Power> generatorPower, Quantity<Dimensionless> soc,
		Quantity<Power> batteryDischarge, boolean outOfService) {
}

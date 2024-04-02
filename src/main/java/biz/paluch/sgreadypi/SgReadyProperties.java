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

import biz.paluch.sgreadypi.output.gpio.GpioProperties;
import lombok.Data;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import java.time.Duration;
import java.util.List;

import javax.measure.Quantity;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Power;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the SG Ready control application.
 * 
 * @author Mark Paluch
 */
@ConfigurationProperties(prefix = "sg")
@Data
public class SgReadyProperties {

	/**
	 * Serial number of the power meter.
	 */
	long powerMeterId;

	/**
	 * Collection of inverter hostnames/IP-adresses.
	 */
	List<String> inverterHosts;

	int inverterPort = 502;

	Duration queryInterval = Duration.ofSeconds(10);

	Duration averaging = Duration.ofSeconds(300);

	/**
	 * Power consumption of the heat pump in Watt.
	 */
	Quantity<Power> heatPumpPowerConsumption;

	/**
	 * Limit above which the SG Ready state is set to normal operations. Any lower power ingress is considered temporary
	 * spikes.
	 */
	Quantity<Power> ingressLimit = Quantities.getQuantity(200, Units.WATT);

	Levels battery = new Levels(Quantities.getQuantity(20, Units.PERCENT), Quantities.getQuantity(80, Units.PERCENT),
			Quantities.getQuantity(60, Units.PERCENT));

	GpioProperties gpio;

	Duration debounce = Duration.ofMinutes(5);

	/**
	 * @param pvAvailable Battery state of Charge indicating unused PV energy. Used to recommend heat pump temperature
	 *          increase/energy availability.
	 * @param pvExcessOn Battery state of Charge to turn on the heat pump to use. Should be higher than
	 *          {@code pvExcessOff} to disable flickering (hysteresis).
	 * @param pvExcessOff Battery state of Charge to turn off forced heat pump usage. Should be lower than
	 *          {@code pvExcessOff} to disable flickering (hysteresis).
	 */
	public record Levels(Quantity<Dimensionless> pvAvailable, Quantity<Dimensionless> pvExcessOn,
			Quantity<Dimensionless> pvExcessOff) {
	}

}

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
import biz.paluch.sgreadypi.output.gpio.GpioProperties;
import biz.paluch.sgreadypi.weather.GeoPosition;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import javax.measure.Quantity;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Power;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the SG Ready control application.
 *
 * @author Mark Paluch
 */
@ConfigurationProperties(prefix = "sg")
@SuppressWarnings("NullAway.Init") // fields are populated by Spring configuration property binding
public class SgReadyProperties {

	/**
	 * Serial number of the power meter.
	 */
	long powerMeterId;

	/**
	 * Collection of inverter hostnames/IP-adresses.
	 */
	List<String> inverterHosts;

	/**
	 * Modbus TCP port used to query the inverters.
	 */
	int inverterPort = 502;

	/**
	 * Interval at which inverters are polled for new readings.
	 */
	Duration queryInterval = Duration.ofSeconds(10);

	/**
	 * Moving-average window applied to power readings to smooth out spikes.
	 */
	Duration averaging = Duration.ofSeconds(300);

	/**
	 * Power consumption of the heat pump in Watt.
	 */
	Quantity<Power> heatPumpPowerConsumption;

	/**
	 * Limit above which the SG Ready state is set to normal operations. Any lower power ingress is considered temporary
	 * spikes.
	 */
	Quantity<Power> ingressLimit = Watt.of(200);

	/**
	 * Limit {@link SgReadyState#EXCESS_PV} to not be applied before a specific local time.
	 */
	@Nullable LocalTime excessNotBefore;

	/**
	 * Limit {@link SgReadyState#EXCESS_PV} to not be applied after a specific local time.
	 */
	@Nullable LocalTime excessNotAfter;

	/**
	 * Battery state-of-charge thresholds that gate state transitions.
	 */
	Levels battery = new Levels(Percent.of(20), Percent.of(80), Percent.of(60));

	/**
	 * GPIO/relay output configuration.
	 */
	GpioProperties gpio;

	/**
	 * Minimum time between applied state changes to limit relay wear.
	 */
	Duration debounce = Duration.ofMinutes(5);

	/**
	 * Weather-based optimization configuration.
	 */
	@Nullable Weather weather;

	public SgReadyProperties() {}

	public long getPowerMeterId() {
		return this.powerMeterId;
	}

	public List<String> getInverterHosts() {
		return this.inverterHosts;
	}

	public int getInverterPort() {
		return this.inverterPort;
	}

	public Duration getQueryInterval() {
		return this.queryInterval;
	}

	public Duration getAveraging() {
		return this.averaging;
	}

	public Quantity<Power> getHeatPumpPowerConsumption() {
		return this.heatPumpPowerConsumption;
	}

	public Quantity<Power> getIngressLimit() {
		return this.ingressLimit;
	}

	public @Nullable LocalTime getExcessNotBefore() {
		return this.excessNotBefore;
	}

	public @Nullable LocalTime getExcessNotAfter() {
		return this.excessNotAfter;
	}

	public Levels getBattery() {
		return this.battery;
	}

	public GpioProperties getGpio() {
		return this.gpio;
	}

	public Duration getDebounce() {
		return this.debounce;
	}

	public @Nullable Weather getWeather() {
		return this.weather;
	}

	public void setPowerMeterId(long powerMeterId) {
		this.powerMeterId = powerMeterId;
	}

	public void setInverterHosts(List<String> inverterHosts) {
		this.inverterHosts = inverterHosts;
	}

	public void setInverterPort(int inverterPort) {
		this.inverterPort = inverterPort;
	}

	public void setQueryInterval(Duration queryInterval) {
		this.queryInterval = queryInterval;
	}

	public void setAveraging(Duration averaging) {
		this.averaging = averaging;
	}

	public void setHeatPumpPowerConsumption(Quantity<Power> heatPumpPowerConsumption) {
		this.heatPumpPowerConsumption = heatPumpPowerConsumption;
	}

	public void setIngressLimit(Quantity<Power> ingressLimit) {
		this.ingressLimit = ingressLimit;
	}

	public void setExcessNotBefore(@Nullable LocalTime excessNotBefore) {
		this.excessNotBefore = excessNotBefore;
	}

	public void setExcessNotAfter(@Nullable LocalTime excessNotAfter) {
		this.excessNotAfter = excessNotAfter;
	}

	public void setBattery(Levels battery) {
		this.battery = battery;
	}

	public void setGpio(GpioProperties gpio) {
		this.gpio = gpio;
	}

	public void setDebounce(Duration debounce) {
		this.debounce = debounce;
	}

	public void setWeather(@Nullable Weather weather) {
		this.weather = weather;
	}

	public String toString() {
		return "SgReadyProperties(powerMeterId=" + this.getPowerMeterId() + ", inverterHosts=" + this.getInverterHosts()
				+ ", inverterPort=" + this.getInverterPort() + ", queryInterval=" + this.getQueryInterval() + ", averaging="
				+ this.getAveraging() + ", heatPumpPowerConsumption=" + this.getHeatPumpPowerConsumption() + ", ingressLimit="
				+ this.getIngressLimit() + ", excessNotBefore=" + this.getExcessNotBefore() + ", excessNotAfter="
				+ this.getExcessNotAfter() + ", battery=" + this.getBattery() + ", gpio=" + this.getGpio() + ", debounce="
				+ this.getDebounce() + ", weather=" + this.getWeather() + ")";
	}

	/**
	 * @param pvAvailable Battery state of Charge indicating unused PV energy. Used to recommend heat pump temperature
	 *          increase/energy availability.
	 * @param pvExcessOn Battery state of Charge to turn on the heat pump to use. Should be higher than
	 *          {@code pvExcessOff} to disable flickering (hysteresis).
	 * @param pvExcessOff Battery state of Charge to turn off forced heat pump usage. Should be lower than
	 *          {@code pvExcessOn} to disable flickering (hysteresis).
	 */
	public record Levels(Quantity<Dimensionless> pvAvailable, Quantity<Dimensionless> pvExcessOn,
			Quantity<Dimensionless> pvExcessOff) {
	}

	/**
	 * Configuration properties to configure weather-based predications considering the sun position.
	 */
	public static class Weather {

		private boolean enabled = false;
		private double latitude;
		private double longitude;
		private Duration notBeforeSunset = Duration.ofHours(1).plus(Duration.ofMinutes(30));

		/**
		 * Duration of {@link SgReadyState#EXCESS_PV} that we want to consume depending on weather predictions. The default
		 * is to stop {@link SgReadyState#EXCESS_PV} until the duration before sunset is reached. In cases, where some hours
		 * before sunset is cloudy, we know that we won't have enough energy, so we try to start earlier consume excess
		 * energy.
		 */
		Duration desiredExcessDuration = Duration.ofHours(3);

		public Weather() {}

		public GeoPosition getGeoPosition() {
			return new GeoPosition(latitude, longitude);
		}

		public boolean isEnabled() {
			return this.enabled;
		}

		public double getLatitude() {
			return this.latitude;
		}

		public double getLongitude() {
			return this.longitude;
		}

		public Duration getNotBeforeSunset() {
			return this.notBeforeSunset;
		}

		public Duration getDesiredExcessDuration() {
			return this.desiredExcessDuration;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		public void setNotBeforeSunset(Duration notBeforeSunset) {
			this.notBeforeSunset = notBeforeSunset;
		}

		public void setDesiredExcessDuration(Duration desiredExcessDuration) {
			this.desiredExcessDuration = desiredExcessDuration;
		}

		public String toString() {
			return "SgReadyProperties.Weather(enabled=" + this.isEnabled() + ", latitude=" + this.getLatitude()
					+ ", longitude=" + this.getLongitude() + ", notBeforeSunset=" + this.getNotBeforeSunset()
					+ ", desiredExcessDuration=" + this.getDesiredExcessDuration() + ")";
		}
	}

}

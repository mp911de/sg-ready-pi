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
package biz.paluch.sgreadypi.output.gpio;

import biz.paluch.sgreadypi.SgReadyState;
import biz.paluch.sgreadypi.output.SgReadyStateConsumer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.library.pigpio.PiGpioException;

/**
 * Output consumer for 3 channels.
 *
 * @author Mark Paluch
 * @link <a href="https://www.waveshare.com/wiki/RPi_Relay_Board">3Ch RPi Relay Board</a>
 */
public class PiRelHat3Ch implements SgReadyStateConsumer {

	private final DigitalOutput ch1;

	private final DigitalOutput ch2;

	private final DigitalOutput ch3 = null; // unused;
	private final Context context;

	public PiRelHat3Ch(Context context) {
		this(context, PIN.D26.getPin(), PIN.D20.getPin());
	}

	public PiRelHat3Ch(Context context, int ch1, int ch2) {

		var ch1Config = DigitalOutput.newConfigBuilder(context).id("BCM D" + ch1).name("CH1")
				.address(ch1).shutdown(DigitalState.HIGH)
				.build();
		var ch2Config = DigitalOutput.newConfigBuilder(context).id("BCM D" + ch2).name("CH2")
				.address(ch2).shutdown(DigitalState.HIGH)
				.build();
		this.ch1 = context.create(ch1Config);
		this.ch2 = context.create(ch2Config);
		this.context = context;
	}

	@PostConstruct
	public void postConstruct(){
		onState(SgReadyState.NORMAL);
	}

	@PreDestroy
	public void preDestroy(){
		try {
			onState(SgReadyState.NORMAL);
		} catch (PiGpioException ignored) {

		}
	}

	@Override
	public void onState(SgReadyState state) {

		this.ch1.state(getState(state.a()));
		this.ch2.state(getState(state.b()));
	}

	private static DigitalState getState(boolean state) {
		return state ? DigitalState.LOW : DigitalState.HIGH;
	}

	DigitalOutput getCh1() {
		return ch1;
	}

	 DigitalOutput getCh2() {
		return ch2;
	}

	 DigitalOutput getCh3() {
		return ch3;
	}
}

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

/**
 * GPIO properties to configure the SG Ready state output.
 *
 * @author Mark Paluch
 */
public record GpioProperties(Rpi3Ch rpi3Ch) {

	/**
	 * 3 Relay configuration of which channel 3 is unused.
	 *
	 * @param pinA GPIO (BCM) pin for SG Ready A.
	 * @param pinB GPIO (BCM) pin for SG Ready B.
	 * @param pinC GPIO (BCM) pin for SG Ready A and B (AND operation of A and B states).
	 */
	public record Rpi3Ch(int pinA, int pinB, int pinC) {

	}

}

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
package biz.paluch.sgreadypi.gpio;

/**
 * Helper Class, used as Raspberry-Pi pin-numbering. Is helpful to see, which pin can act as what I/O provider
 */
public enum PIN {
    SDA1(2),
    SCL1(2),
    TXD(14),
    RXD(15),
    D4(4),
    D5(5),
    D6(6),
    D11(11),
    D16(16),
    D17(17),
    D20(20),
    D21(21),
    D22(22),
    D23(23),
    D24(24),
    D25(25),
    D26(26),
    D27(27),
    MOSI(10),
    MISO(9),
    CEO(8),
    CE1(7),
    PWM12(12),
    PWM13(13),
    PWM18(18),
    PWM19(19);

    private final int pin;

    PIN(int pin) {
        this.pin = pin;
    }

    public int getPin() {
        return pin;
    }
}

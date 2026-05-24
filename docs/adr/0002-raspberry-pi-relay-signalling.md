# Signal SG Ready via potential-free relays on a Raspberry Pi

The controller targets a Raspberry Pi and drives the SG Ready **A** and **B** signals
through external relays (Waveshare RPi Relay Board via Pi4J), not by pulling GPIO pins to
GND directly. Heat pumps expect a potential-free contact, so each signal closes an
independent, electrically isolated circuit; a third relay channel is driven with
`A AND B`. The Pi was chosen for low idle power consumption since the device runs 24/7.

## Consequences

- Hardware lock-in to Pi4J + a relay HAT; GPIO/relay code is guarded by an
  `@ConditionalOnRaspberryPi`
  condition so the app still boots (with a mock context) off-device for development.
- Relay actuation is physical, which is what motivates the time-based **Debounce** (see
  CONTEXT.md).

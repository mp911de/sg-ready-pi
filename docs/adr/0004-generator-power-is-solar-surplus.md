# Generator Power measures solar surplus, not inverter output

**Generator Power** is computed as
`currentActivePower + batteryCharging - abs(batteryDischarging)`, deliberately subtracting
battery discharge. We want the controller to react only to sun-derived surplus, so that
**Excess PV** is never triggered by power that is actually being pulled out of the
battery. A future reader would otherwise reasonably "fix" this by reporting total inverter
output.

## Consequences

- During battery discharge, Generator Power can read low or zero even while the inverter
  is delivering power to the house — this is intended, not a bug.

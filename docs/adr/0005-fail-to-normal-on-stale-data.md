# Fall back to Normal when an input is out of service

If the **Power Generator** or **Power Meter** reports **Out of Service** (its **Recency
Tracker**
data age exceeds the out-of-service threshold), the control loop signals **Normal** rather
than holding the last known **SG Ready State**. Stale data must not keep the heat pump
blocked or forced; returning the signal to neutral is the safe default and lets the heat
pump resume its own schedule.

## Consequences

- A transient sensor outage briefly resets signalling to Normal even if conditions were
  favourable; this is accepted as the conservative choice over acting on unknown current
  conditions.

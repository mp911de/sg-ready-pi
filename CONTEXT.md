# SG Ready Control

This context derives an **SG Ready State** for an electrical heat pump by comparing the
power a home PV system generates against what the household draws from the grid. The
premise is that a hot-water tank is a thermal battery: heating water with surplus solar
power stores energy that the house would otherwise feed into the grid, so the controller
signals the heat pump to heat *more* and *later* when free power is available.

## Language

### SG Ready signalling

**SG Ready**:
The external standard that signals one of four operating modes to a heat pump over two
potential-free relay circuits (the **A signal** and **B signal**). _Avoid_: SGR, SG-Ready
Protocol

**SG Ready State**:
The mode the controller currently signals, encoded as the pair `(A, B)` and rendered as
`a:b`
(for example `1:1`). One of **Normal**, **Blocked**, **Available PV**, or **Excess PV**.
_Avoid_: mode, signal level

**Normal** (`0:0`):
No indication; the heat pump runs its own schedule. _Avoid_: idle, default

**Blocked** (`1:0`):
Instructs the heat pump to reduce consumption (a lock period of up to ~2 hours/day).
_Avoid_: Lock, Locked, Off — the SG Ready standard calls this "Lock", but the codebase
uses
`BLOCKED` and that is canonical here.

**Available PV** (`0:1`):
Recommends the heat pump raise temperature now because consuming power is favourable
(surplus / low tariff), without forcing it. _Avoid_: Available Power, Low Tariff,
Recommendation

**Excess PV** (`1:1`):
Forces the heat pump on to consume excess power. _Avoid_: Excess Power, Switch on Heating,
Forced

### Power and energy

**Power Generator**:
The PV inverter (s) plus attached battery, viewed as a single source of generated power.
Modelled by
`PowerGeneratorService`. _Avoid_: PV, solar source, producer

**Power Meter**:
The grid-connection meter that reports power crossing the household boundary. Modelled by
`PowerMeter`. _Avoid_: smart meter, grid meter

**Ingress**:
Power drawn *from* the grid into the household. _Avoid_: import, consumption, draw

**Egress**:
Power fed *into* the grid from the household. _Avoid_: export, feed-in

**Generator Power**:
The household's usable solar surplus — inverter active power plus battery charging minus
battery discharging — so the figure reflects sun-derived power, not power being pulled out
of the battery. See ADR-0004. _Avoid_: PV power, solar output, inverter output

**State of Charge** (SoC):
The battery charge level as a percentage; the controller's primary gate for moving between
states. _Avoid_: charge level, battery percent

**Heat Pump Power Consumption**:
The configured expected draw of the heat pump; **Generator Power** must exceed it before
any PV state is considered. _Avoid_: load, demand

**Ingress Limit**:
The **Ingress** threshold above which the controller forces **Normal**; smaller ingress is
treated as a temporary spike and ignored. _Avoid_: import threshold

### Control thresholds

**Levels**:
The trio of SoC thresholds that gate state transitions: **pvAvailable**, **pvExcessOn**,
and **pvExcessOff**.

**Hysteresis**:
The gap between **pvExcessOn** (SoC to start **Excess PV**) and **pvExcessOff** (SoC to
stop it), deliberately set apart so the state does not flicker around a single threshold.
_Avoid_: deadband

**Debounce**:
A time-based suppression of rapid state changes, applied on top of **Hysteresis**, to
limit physical relay wear. _Avoid_: throttle, cooldown

**Decision**:
The **SG Ready Policy**'s output: the chosen **SG Ready State** together with a chain of
**Condition Outcomes** explaining how it was reached. _Avoid_: result, verdict

**Condition Outcome**:
One match/no-match step (with a human-readable message) in the reasoning chain behind a
**Decision**; outcomes nest to form an explanation trail surfaced in the health endpoint.
_Avoid_: rule result, check

**SG Ready Policy**:
The pure function that maps a snapshot of **Conditions** (with the current **SG Ready
State**, the weather **Usable Time Range**, and configuration) to a **Decision**. It holds
every state-selection rule — ingress limit, **Generator Power** gate, **Levels** /
**Hysteresis**, time-of-day gating, weather deferral, and the **Out of Service**
fallback — and performs no I/O; the control loop resolves the inputs and feeds them in.
Modelled by
`SgReadyPolicy`. _Avoid_: decision engine, decider, rules engine, strategy

**Conditions**:
The snapshot of sensed inputs a **Decision** is made from: **Ingress**, **Generator
Power**, **State of Charge**, and whether any input is **Out of Service**. _Avoid_:
readings, inputs, power readings, sensor state

### Weather optimisation

**Usable Time Range**:
The window before sunset during which enough sun is forecast to run **Excess PV** without
draining the battery. The controller defers excess consumption to as late as possible
within this window. See ADR-0003. _Avoid_: sun window, optimisation window

**Cloud Coverage**:
Forecast cloudiness (combined low- and mid-level cloud) per hour; coverage below 60%
counts as **sunny time**. _Avoid_: cloudiness, overcast

**Sunny Time** / **Remaining Sun Duration**:
Forecast time below the cloud-coverage threshold between now and the sunset limit — the
budget of generatable solar energy left in the day. _Avoid_: daylight, sun hours

**Not-Before-Sunset**:
The configured duration before sunset after which generating useful surplus is no longer
expected, so **Excess PV** is withheld. _Avoid_: sunset buffer

**Desired Excess Duration**:
How long **Excess PV** is intended to run; if remaining **Sunny Time** is shorter than
this, the controller starts excess consumption earlier instead of deferring. _Avoid_:
target duration

### Data freshness

**Recency Tracker**:
A component that reports a **Health State** derived from how long ago its data last
updated. _Avoid_: freshness checker, watchdog

**Health State**:
One of `HEALTHY`, `DEGRADED`, or `OUT_OF_SERVICE`, classified by data age against the
healthy and out-of-service thresholds. _Avoid_: status

**Out of Service**:
A **Power Generator** or **Power Meter** whose data is too stale to trust; the controller
falls back to **Normal** when any input is out of service. See ADR-0005. _Avoid_: down,
offline, unhealthy

## Flagged ambiguities

- **Standard terms vs. code constants.** The SG Ready standard names the modes "Lock", "
  Available Power", and "Excess Power"; this codebase names them `BLOCKED`,
  `AVAILABLE_PV`, and `EXCESS_PV`. Resolution: the code constants are canonical *within
  the project*; cite the standard's terms only when referring to the external
  specification.
- **"Power" is overloaded.** "Power" appears in **Power Generator**, **Power Meter**, and
  **Generator Power**. The first two are sources/devices; **Generator Power** is a
  measured quantity (solar surplus). Prefer the full term, never bare "power", when the
  distinction matters.
- **Conditions vs. Condition Outcome.** **Conditions** is the snapshot of sensed inputs
  fed *into* the **SG Ready Policy**; a **Condition Outcome** is a match/no-match step in
  the reasoning trail that comes *out* of it. They sit on opposite sides of a
  **Decision** — never use one where you mean the other.

## Example dialogue

> **Dev:** When the inverters report 1500 W, do we go straight to Excess PV?
>
> **Domain expert:** Not by itself. First **Generator Power** has to clear the **Heat Pump
Power
> Consumption** — and remember Generator Power is solar *surplus*, so battery discharge is
> netted
> out. Then it's gated on **State of Charge**: above **pvExcessOn** we go to **Excess
PV**, above
> **pvAvailable** but below that we only signal **Available PV**.
>
> **Dev:** And the gap between pvExcessOn and pvExcessOff — that's just two config keys?
>
> **Domain expert:** That gap *is* the **Hysteresis** — it stops the state oscillating at
> one SoC
> level. On top of it there's **Debounce**, which is time-based, to save the relays.
>
> **Dev:** What if it's 4 p.m. and clear skies but the battery's full?
>
> **Domain expert:** Then weather optimisation kicks in. We compute the **Usable Time
Range** from
> **Cloud Coverage** and sunset and *defer* **Excess PV** to as late as possible — the
> hot-water tank
> holds the heat overnight, so we'd rather heat just before sunset than dump surplus at
> noon. Unless
> the **Remaining Sun Duration** is already shorter than the **Desired Excess Duration**,
> in which
> case we start now.
>
> **Dev:** And if the power meter stops responding?
>
> **Domain expert:** Once its **Recency Tracker** reports **Out of Service**, we fall back
> to
> **Normal**. We never hold a Blocked or Excess signal on stale data.

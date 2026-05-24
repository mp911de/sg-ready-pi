# Defer Excess PV to late in the day using a weather forecast

When surplus solar is available, the controller does **not** immediately force **Excess
PV**. Instead it computes a **Usable Time Range** from sunset and an Open-Meteo
cloud-coverage forecast and defers excess consumption to as late as possible before
sunset. The hot-water tank holds heat overnight, so heating just before sunset maximises
self-consumption and avoids draining the battery at midday only to re-import power in the
evening.

## Consequences

- Adds a hard dependency on an external forecast API (Open-Meteo) and on sunset
  calculation, both guarded behind `sg.weather.enabled`.
- If remaining **Sunny Time** is already shorter than the **Desired Excess Duration**, the
  deferral is abandoned and excess consumption starts immediately — a deliberate exception
  to the "as late as possible" rule.

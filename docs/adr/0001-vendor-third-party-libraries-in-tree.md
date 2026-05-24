# Vendor third-party libraries in-tree

The SMA Speedwire/Modbus support (`cat.joanpujol.smasolar`, from lujop's smasolarlib) and
parts of the solar-positioning code are copied directly into `src/main/java` rather than
declared as Maven dependencies, because no artifacts are published to Maven Central. We
accept owning and maintaining this code in-tree as the price of using these libraries at
all.

## Consequences

- The `cat.joanpujol.smasolar` tree is excluded from our own conventions (Javadoc reviews,
  formatting) — it is treated as vendored, not authored here.
- Upstream fixes must be ported by hand; there is no version bump.

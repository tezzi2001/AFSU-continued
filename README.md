# AFSU

**Advanced Field Storage Unit** — an addon for **IndustrialCraft 2 Experimental** that adds a tier‑5 EU storage block (AFSU) and the **AFB** crafting component.

This project is a **Minecraft 1.12.2** port of [xbony2/AFSU](https://github.com/xbony2/AFSU). Maintained by **tezzi2001**; contributions and PRs are welcome.

---

## Features

| Item | Description |
|------|-------------|
| **AFSU** | Large EU buffer (1B EU cap, high output tier), IC2-style GUI, redstone modes, wrench rotation & removal |
| **AFB** | Crafting ingredient for the AFSU (shaped recipe via IC2 advanced recipes) |

---

## Requirements

| Component | Version |
|-----------|---------|
| Minecraft | **1.12.2** |
| Minecraft Forge | **14.23.5.2847** |
| IndustrialCraft² Experimental | **2.8.222-ex112** |
| Java (build & run) | **JDK 8** |

IC2 is resolved from the [IC2 Maven](http://maven.ic2.player.to/) (`build.gradle`). The mod integrates with IC2’s energy net and crafting; an API-only dependency is not sufficient for a full dev workspace.

---

## Building

```bash
# JDK 8 on PATH (or set JAVA_HOME)
./gradlew clean build
```

Output JAR: `build/libs/` (name follows `archivesBaseName` + version in `build.gradle`).

---

## Development client

```bash
export JAVA_HOME=/path/to/jdk8   # if your default Java is newer
./gradlew runClient
```

`runClient` keeps the Gradle task alive until you close Minecraft; `BUILD SUCCESSFUL` appears after exit.

---

## License & credits

- Original **AFSU** mod: [xbony2](https://github.com/xbony2/AFSU) — see `LICENSE` for upstream copyright.
- **1.12.2 port & maintenance**: tezzi2001 — issues and pull requests welcome.

AFSU
====

An addon to IC2 that adds a tier five energy storage unit.

## Minecraft target

This branch targets:

- Minecraft `1.12.2`
- Forge `14.23.5.2847`
- IndustrialCraft 2 Experimental `2.8.222-ex112`

## Dev setup

1. Install JDK 8 (required for ForgeGradle 2.x / 1.12.2 toolchain).
2. Run:
   - `./gradlew clean build`
3. For dev runs, use:
   - `./gradlew runClient`

## Notes

- IC2 is pulled from the IC2 Maven repository (`http://maven.ic2.player.to/`) with the `dev` classifier.
- This mod uses IC2 internals (container/gui/tile classes), so API-only dependency is not enough.

# Maledict

A Minecraft NeoForge add-on for [Malum](https://www.curseforge.com/minecraft/mc-mods/malum), continuing spirit arcana past
the umbral crystal. The centerpiece is the **Incursus Blade**, a magic scythe that grows as it devours spirits; around
it sit spirit-fed bows and arrows, two shelves of runes, a charm that meddles with time and darkness, two obelisks, and
the rites that call the First Vicissitude.

This README is an overview of how the project is put together. What the mod contains is documented in the Encyclopedia
Arcana, in game; the conventions and the mechanics that were expensive to get right are in [AGENTS.md](AGENTS.md).

## Stack

Minecraft 1.21.1, NeoForge, Java 21 and ModDevGradle. Versions live in `gradle.properties`;
`build.gradle` expands `src/main/templates/META-INF/neoforge.mods.toml` at build time.

Runtime dependencies are **Malum** (the parent mod, and the source of spirit types, rite and infusion systems, and the
codex plumbing), **Lodestone** (attributes, particle and screenshake builders) and **Curios** (the rune, charm and eye
slots). **ChangeLib** is bundled into the jar through `jarJar`. JEI and a few client-side mods are development
conveniences.

## Layout

```
src/main/java/org/brahypno/maledict/
  registry/    DeferredRegisters: items, blocks, entities, effects, enchantments, sounds, tabs
  common/
    entity/    raven/, vicissitude/, projectile/
    item/      weapon/incursus/, weapon/axe/, weapon/bow/, arrow/, rune/, curio/
    curio/     enlightenment/, hangman/, rune/, vicissitude/; shared cooldown helpers
    enchantment/  enchantments and their combat effects
    block/, effect/, rite/, realm/, attribute/, corruption/, vitals/
  client/      item/, raven/, vicissitude/, codex/, tooltip/, infrared/, vitals/, wisdom/, realm/, jei/
  rig/         shared boss geometry, animation and collision calculations
  data/        client/, loot/, recipe/, tags/, worldgen/; MaledictDataGenerators is the entry point
  network/     clientbound/, serverbound/; MaledictNetwork registers all payloads
  mixin/       compatibility hooks and accessors
  config/      common and client config specs
src/main/resources/   hand-written assets
src/generated/        data generator output — regenerate with runData, never edit by hand
src/test/             JUnit and GameTest sources, following the production package layout
art/                  editable source artwork and preview tools, grouped by feature
docs/design/          per-feature design and verification notes
tools/                developer tools, grouped by purpose
```

## Building

```shell
./gradlew build        # Windows: .\gradlew.bat build
./gradlew runData      # regenerate src/generated
./gradlew test         # JUnit tests, also included in build/check
./gradlew runGameTestServer  # real server entity scenarios
```

`runClient`, `runServer`, `runData` and `runGameTestServer` are the development launch tasks.
The distributable jar, including ChangeLib, is written to `build/libs`.
See [src/test/README.md](src/test/README.md) for test commands and scope.

## Documentation

- [AGENTS.md](AGENTS.md) — conventions and confirmed mechanics. Read it before changing anything.
- `docs/design/` — design and verification notes, one directory per feature, some with an `archive/` of superseded
  iterations.
- [CREDITS.md](src/main/resources/CREDITS.md) — licences for bundled audio and image assets.

## License

Code is licensed under the [GNU Lesser General Public License v3.0 only](LICENSE) (`LGPL-3.0-only`). Bundled audio and
image assets carry their own licences and are not covered by the LGPL.

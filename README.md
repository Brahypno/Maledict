# Maledict

A Minecraft Forge add-on for [Malum](https://www.curseforge.com/minecraft/mc-mods/malum), continuing spirit arcana past
the umbral crystal. The centerpiece is the **Incursus Blade**, a magic scythe that grows as it devours spirits; around
it sit spirit-fed bows and arrows, two shelves of runes, a charm that meddles with time and darkness, two obelisks, and
the rites that call the First Vicissitude.

This README is an overview of how the project is put together. What the mod contains is documented in the Encyclopedia
Arcana, in game; the conventions and the mechanics that were expensive to get right are in [AGENTS.md](AGENTS.md).

## Stack

Forge 1.20.1, Java 17, Mixin, ForgeGradle. Versions live in `gradle.properties` — `build.gradle` reads them and
`mods.toml` is expanded from them at build time.

Runtime dependencies are **Malum** (the parent mod, and the source of spirit types, rite and infusion systems, and the
codex plumbing), **Lodestone** (attributes, particle and screenshake builders) and **Curios** (the rune, charm and eye
slots). **ChangeLib** is bundled into the jar through `jarJar`. JEI and a few client-side mods are development
conveniences.

## Layout

```
src/main/java/org/brahypno/maledict/
  registry/    DeferredRegisters: items, blocks, entities, effects, enchantments, sounds, tabs
  common/
    raven/          raven entity, AI, feeding, trust and flight
    vicissitude/    boss entities and vitality; rig/, rite/ and curio/ support the encounter
    weapon/         incursus/, lover/ and remembrance/ items, attacks and projectiles
    rune/           rune items and their combat hooks
    curio/          enlightenment/ and hangman/ charms and their supporting logic
    wisdom/         sacrifice rite, effect, advancement and vision slot
    combat/         damage adaptation shared by the boss and melancholia rune
    attribute/, block/, corruption/, effect/, enchantment/, item/, realm/, vitals/
  client/      matching feature packages for rendering, input, HUD and effects;
               item/ holds cross-feature model predicates, MaledictClientSetup registers client content
  data/        data generators
  network/     packets
  mixin/       platform hooks configured in maledict.mixins.json
  config/      common and client config specs
src/main/resources/   hand-written assets
src/generated/        data generator output — regenerate with runData, never edit by hand
src/test/             JUnit and Forge entity tests, following production package paths
docs/design/          per-feature design and verification notes
```

## Building

```shell
./gradlew build        # Windows: .\gradlew.bat build
./gradlew runData      # regenerate src/generated
./gradlew test         # Forge entity tests, then JUnit; not wired into build or check
```

`runClient`, `runServer` and `gameTestServer` are the standard Forge run configs. The distributable jar is the jarJar
artifact in `build/libs`.

## Documentation

- [AGENTS.md](AGENTS.md) — conventions and confirmed mechanics. Read it before changing anything.
- `docs/design/` — design and verification notes, one directory per feature, some with an `archive/` of superseded
  iterations.
- [CREDITS.md](src/main/resources/CREDITS.md) — licences for bundled audio and image assets.

## License

Code is licensed under the [GNU Lesser General Public License v3.0 only](LICENSE) (`LGPL-3.0-only`). Bundled audio and
image assets carry their own licences and are not covered by the LGPL.

# Maledict

Maledict is a Minecraft Forge 1.20.1 add-on for [Malum](https://www.curseforge.com/minecraft/mc-mods/malum) that
extends the mod past the umbral crystal. Its centerpiece is the **Incursus Blade**, a magic scythe that grows as it
devours spirits; around it sit spirit-fed bows and arrows, a shelf of runes cut from wood and pewter, a charm that
meddles with time and darkness, two obelisks, and the rites that call the First Vicissitude.

## What it adds

- **The Incursus Blade** — a magic scythe infused from Malum's Edge of Deliverance. It cuts through shields, takes
  reduced durability loss, and sweeps every enemy around its wielder instead of striking one at a time. Feed it
  spirit shards and it grows: each of the eight spirits deepens a different property, at a cost that climbs as it
  rises, and the blade's spirit effects strike harder once every spirit has reached the sacred number.
- **Spirit bows and arrows** — the **Remembrance Bow** draws twice as fast as a vanilla bow and looses faster arrows
  that phase through a wall, all the deeper with *Reminiscence*. Infuse it further and it becomes the **Elegy Bow**,
  which releases itself the moment it reaches full draw and keeps firing for as long as you hold it. Eight **spirit
  arrows**, one per non-umbral spirit and each cut from that spirit's shard, carry their spirit's character into
  whatever they hit.
- **Runes** — a growing shelf of them, worked at the runic workbench from runewood and soulwood tablets or from
  **Malignant Pewter**, a metal that refuses magic. The arcana line shrinks a totemic rite's pulse into a charm: a
  second share of every meal, a quarter more experience, a brawl's worth of strength for your own beasts. The void
  line refuses instead — *Stagnant Evolution* holds magic at arm's length, *Rotten Bone* spends your own flesh to
  keep you breathing, and *Melancholia* teaches you to ache less at the blow that keeps landing.
- **The Age of Enlightenment** — a void curio amulet that recovers item cooldowns at double speed, harvests spirits
  from anything it strikes below half health, and renews its own enlightenment on every kill: blows land certain, and
  the nearest enemy is left wearing the Age of Darkness.
- **Obelisks** — the **Soulwood Obelisk** speeds up a spirit altar by half again, and the **Mnemonic Obelisk**
  brings the enchanting power of ten bookshelves to one block.
- **The First Vicissitude** — not something you craft, but something you *call*: the **Rites of Vicissitude** stack
  spirits on a totem — three arcane pry a crack open and four pry it wider, while eldritch spirits set at the very
  bottom reach deeper, one weaving a whole shadow and two bringing the face that no longer holds back. What answers
  is fate's own unwelcome guest. The deeper calls confiscate your equipped curios when the second phase begins,
  handing one back for every hit you land.
- **Enchantments** — *Ectoplasm* spends spirits to speed up your arrows, *Reminiscence* deepens their phasing, and
  *Aftertaste* lets a scythe hit taste the hunger its victim's drops would have fed you.

## In the game

Every addition carries its own Malum codex entry, recipes and all, and the whole set sits together in the Maledict
creative tab. This README stays at the level of what the mod *is*; the numbers live in the book. Maledict is still
in development (version 0.10-SNAPSHOT) — the First Vicissitude in particular is still being tuned.

## Requirements

- Java 17
- Minecraft 1.20.1
- Minecraft Forge 47.4.23 or later
- Malum 1.20.1-1.6.7 or later
- Lodestone 1.20.1-1.6.4.1 or later
- Curios API

Build or download the jar and drop it into your `mods` folder alongside those dependencies.

## Configuration

`config/maledict-common.toml` holds the blade's per-spirit infusion costs, the First Vicissitude's engagement and
damage ranges, how many kinds of harm it learns to shrug off, and which entity its rites summon;
`config/maledict-client.toml` holds the encounter's screenshake.

## Building

Clone the repository and run the Gradle wrapper:

```shell
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

Build artifacts are written to `build/libs`. Two other tasks are worth knowing:

- `./gradlew runData` regenerates `src/generated` after a data change. Never edit those files by hand — fix the
  generator and run it again.
- `./gradlew test` runs the unit tests.

## License

Maledict is licensed under the [GNU Lesser General Public License v3.0 only](LICENSE) (`LGPL-3.0-only`).

# Maledict

Maledict is a Minecraft Forge 1.20.1 add-on for Malum that extends the mod past the umbral crystal with new
weapons, curios, obelisks, and a boss to call. Its centerpiece is the **Incursus Blade**, a magic scythe that grows
as it devours spirits; around it sit spirit-fed bows, a charm that meddles with time, and the rite that summons the
First Vicissitude.

## What it adds

- **The Incursus Blade** — a magic scythe infused from Malum's Edge of Deliverance. It cuts through shields, takes
  reduced durability loss, and sweeps every enemy around its wielder instead of striking one at a time. Feed it
  spirit shards and it grows: each of the eight spirits deepens a different property, at a cost that climbs as it
  rises, and the blade's spirit effects strike harder once every spirit has reached the sacred number.
- **Bows and spirit arrows** — the **Remembrance Bow** draws twice as fast as a vanilla bow and looses faster arrows
  that phase through a wall, all the deeper with Reminiscence. Infuse it further and it becomes the **Elegy Bow**,
  which releases itself the moment it reaches full draw and keeps firing for as long as you hold it. Eight **spirit
  arrows**, one per non-umbral spirit and each made from that spirit's shard, carry their spirit's character: Aerial
  arrows fly fastest, Aqueous arrows ignore water, Sacred arrows punish the undead while Wicked arrows punish the
  living, Infernal arrows mark their target, and Arcane and Eldritch arrows add magic damage on impact.
- **Obelisks** — the **Soulwood Obelisk** speeds up a spirit altar by half again, and the **Mnemonic Obelisk**
  brings the enchanting power of ten bookshelves to one block.
- **The First Vicissitude** — a boss *called* rather than crafted, by four **Rites of Vicissitude** whose recipes
  decide which difficulty answers the totem.
- **Enchantments** — *Ectoplasm* spends spirits to speed up your arrows, *Reminiscence* deepens their phasing, and
  *Aftertaste* lets a scythe hit taste the hunger its victim's drops would have fed you.

Every addition is documented in-game through Malum's codex, which gains its own void-tier entries for these items,
obelisks, and rites.

## Requirements

- Java 17
- Minecraft 1.20.1
- Minecraft Forge 47.4.23 or later
- Malum 1.20.1-1.6.7 or later
- Lodestone 1.20.1-1.6.4.1 or later
- Curios API

## Building

Clone the repository and run the Gradle wrapper:

```shell
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

Build artifacts are written to `build/libs`.

## License

Maledict is licensed under the [GNU Lesser General Public License v3.0 only](LICENSE) (`LGPL-3.0-only`).

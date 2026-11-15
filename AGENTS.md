# Repository Instructions

## Generated resources

- Never edit any file under `src/generated` directly, including cleanup, formatting, restoration, or one-line
  corrections.
- Change the responsible data-generator source, then run `runData` to update `src/generated`.
- Treat every change produced by `runData` as generator output. If that output is wrong or unexpectedly broad, fix the
  generator and rerun it; do not repair the generated files manually.

## Development Notes

- Keep at most 50 GameTest cases and at most 50 JUnit cases in the repository. Replace or remove lower-value
  cases when adding coverage at the limit; do not hide extra cases with filters, disabling, or nested test runners.

- Do not run full builds or packaging tasks (`build`, `assemble`, `jar`, `jarJar`, or reobfuscation tasks) unless
  the user explicitly requests a build, package, or distributable jar. A request to implement or fix something
  does not authorize packaging. Use only the checks needed for the affected behavior; run `runData` when
  generated resources need updating. Do not append `build` to a test or data-generation command.

- Mixins are a last resort. First use existing item/entity overrides, Forge events, extension hooks, and public APIs.
  For custom left-click weapon behavior, check `Item#onLeftClickEntity` before considering an injection into
  `Player#attack`. Introduce a mixin only when those supported extension points cannot implement the required
  behavior, and document the concrete missing capability before adding it.

- Chinese text for Malum codex body pages must contain a literal space after every 13 visible characters. Punctuation
  counts toward the 13-character limit; the inserted layout spaces do not.
- Write the Chinese codex copy first, then derive the English localization from its meaning.
- English codex body copy needs roughly twice the lines of its Chinese source, and a page never clips: too many lines
  are drawn straight over the book frame. Page capacities are TextPage 16, HeadlineTextPage 14, HeadlineTextItemPage 9,
  SpiritRiteTextPage 9 lines. When the English does not fit the page its Chinese source is happy with, add a `TextPage`
  continuation key rather than trimming the translation, and re-split the Chinese across the same two pages (words
  unchanged). `node tools/lang-layout/check-lang-layout.mjs` measures both languages against the real layout, see
  `tools/lang-layout/README.md`.

## Confirmed mechanics

These are settled behaviours, written down because each of them cost several rounds of misreading.

### First Vicissitude adaptation (`DamageAdaptation`)

A sliding window of the **most recently hit damage messages** (`DamageSource#getMsgId()`); the window size is
`firstVicissitude.adaptationLevel` ("adaptation N", default 2).

- **Record first, then judge.** The incoming message is recorded into the window (moving to the front if already
  present, otherwise inserted and the *least recently hit* entry evicted together with its counter) and only then is
  the multiplier decided. Only a message that was **already in the window before this hit** is reduced, by
  `e^-(times it has been hit while remembered)`. A message that was just recorded is full damage.
- Evicted messages keep **no** state: their counter is discarded, so the next hit on them counts as the first.
- Order matters, including inside one tick: a burst is processed one hit at a time, in call order.
- **One tick, one hit per message** (the boss records through `adaptInBatch(message, level, tick)`). A single swing
  lands the *same* `DamageSource` more than once — `DamageProbe` tops the amount up by hitting again with that same
  source, and the arcane channel is cashed in inside the same `LivingHurtEvent` — so counting every call let one
  swing adapt itself: the blade's `scythe_sweep` is dealt twice per use, the second call was already e⁻¹, and a use
  where the other channels did not interleave pushed it to e⁻² (the taunt fired on the fifth `scythe_sweep` record).
  Inside one tick a message that already landed is therefore **not recorded again** and **reuses the first call's
  multiplier**; the next tick is a new batch. `RuneOfMelancholiaItem` keeps the plain `adapt`, where every call counts.

Trace for `adaptation 2`, A→B→C→A→B→C — **nothing is ever reduced**:

```
A: record [A]      judge A was absent → full
B: record [B, A]   judge B was absent → full
C: record [C, B]   judge C was absent → full   (A evicted, counter discarded)
A: record [A, C]   judge A was absent → full   (B evicted, counter discarded)
B: record [B, A]   judge B was absent → full   (C evicted, counter discarded)
C: record [C, B]   judge C was absent → full   (A evicted, counter discarded)
```

Consequences: adaptation 1 cannot reduce a three-message rotation (the previous message is always the one evicted);
adaptation 2 also cannot, for the same reason; a **two**-message rotation under adaptation 2 is held, and reduces from
the third hit on; adaptation 3 holds all three and reduces from the second round on. Note that the dev scythe
(Incursus Blade) lands three messages per swing on its sweep paths (`scythe_sweep` / `magic` / `freeze`), so under
the default of 2 its damage is never adapted. The `freeze` message is the independent
`powder_snow_damage` attribute request and still uses `minecraft:freeze`; the `magic` message is the independent
Lodestone `MAGIC_DAMAGE` attribute request (`minecraft:magic`). The old explicit `voodoo` probe is gone, and both
attribute requests are dispatched after the scythe probe, in registration order (freeze then magic).

### Incursus Blade's arcane channel (`can_trigger_magic_damage`)

The blade's arcane infusion (`MAGIC_DAMAGE` attribute) deals no damage by itself. Lodestone's
`LodestoneAttributeEventHandler#processAttributes` cashes it in **inside the victim's `LivingHurtEvent`**: if the
source type is in `forge:can_trigger_magic_damage` (Malum puts `malum:scythe_melee` and `malum:scythe_sweep` there)
it deals one extra `minecraft:magic` hit worth the attribute, with a null direct entity and the attacker as causing
entity, right after setting `invulnerableTime = 0`. `forge:ignores_magic_attack_cooldown_scalar` is empty, so that
bonus is never scaled by attack strength.

Maledict takes over this attribute for its tagged weapons. `MaledictAttributes` registers Lodestone's
`MAGIC_DAMAGE` with ChangeLib's `AttributeDamageRegistry`, and
`LodestoneAttributeEventHandlerMixin` cancels Lodestone's `processAttributes` copy when the attacker's main hand is
in `maledict:lodestone_magic_takeover` and the original source is in `CAN_TRIGGER_MAGIC` but not `IS_MAGIC`. The
resulting request keeps Lodestone's shape: source `minecraft:magic`, null direct entity, the attacker as causing
entity. The old `markMeleeHurtEvent` compensation is gone.

The aqueous (freeze) channel is no longer an explicit probe. `maledict:powder_snow_damage` is a registered
attribute; the blade's NBT stat feeds a main-hand `ADDITION` modifier, and ChangeLib's `AttributeDamageRegistry`
dispatches one independent `minecraft:freeze` request after the scythe probe. ChangeLib's `ProbeScope` records a
probe result with `totalDealt() > 0` even when its `hurt` path was not observed (an entity that fully overrides
`hurt`, or a probe that settled through setHealth/NBT), so the request is not limited to the vanilla `hurt` event.
Forge multipart bosses expose the body and every animated part as separate entities; `IncursusBladeAttack.uniqueLivingTargets`
collapses the melee and Ascension target lists onto the living root (the nearest hitbox per root), so one swing
still opens one main probe and therefore one freeze/magic round. The boomerang hits a single part and needs no
collapse. The blade's
`DamageTier` still sets the request's base `priority`/`strength` in `IncursusBladeEffects` on
`DamageRequestEvent` (NORMAL), and `BlissRuneEvents` raises it one step (LOW). The unified
`DamageRequestEvent` also covers probe requests; `attribute() != null` distinguishes an attribute
request. The freeze registration uses `AttributeAmountReaders.itemBacked()` so the flying scythe's
item modifiers are composed with the attacker's attributes instead of relying on the item being in
the main hand.

### Boss has no collision movement (`VicissitudeBossEntity`)

`LivingEntity#aiStep` ends with `pushEntities()`, which shoves every pair of **pushable** living entities apart —
`EntitySelector#pushableBy` filters the candidates, `Entity#push` guards each side — and adds that shove straight to
`deltaMovement`, once per tick per overlapping entity. `LivingEntity#isPushable` is true for anything alive, and
`FirstVicissitudeBossEntity`'s steering *blends* (`FLIGHT_STEERING = 0.2`) with the previous velocity instead of
overwriting it, so a player leaning on the boss walks it around at up to ~0.18 blocks/tick.

`VicissitudeBossEntity` therefore answers `isPushable() == false` **and** no-ops the three-argument `push`, because
ram attacks (ender dragon, ravager, hoglin charge, warden sonic boom, moving minecarts) and other mods call that
method directly without ever asking `isPushable`. Players touching the boss are still separated by the boss's own
`pushEntities()`; the wing push (`Player#push`, 0.06 horizontal) is a separate, deliberate shove. Only the boss's
own movement code may change its velocity.

### Vicissitude health is an owned ChangeLib pool (`VicissitudeBossEntity`)

The world `SavedData` ledger remains the only authority. The base implements `CombatProtection.OwnedHealthEntity`,
checks the adapter by identity, and calls `CombatProtection.bindOwned` exactly once in `onAddedToWorld`, after the
ledger record has been restored or initialized. `getHealth()` returns
`CombatProtection.healthView(this, authorityHealth())`; clients prefer ChangeLib's committed snapshot and use
the synced `DISPLAY_VITALITY` only before that snapshot arrives.

Every authority change — damage, healing, canceled-death restore, gate and kill-attempt updates — commits one full
record through the private controller. Resolved damage uses `commitDamage`; other owner updates use `commit`.
The callback only writes the ledger and capability mirror; publishing and events stay outside it. The old `protect`/`unprotect` rebase and the
per-tick grade check are gone: `unprotect`, `protect`, `authorize` and the public `attack` all refuse an owned
binding. Direct `setHealth` and storage damage submit requests to the bound authority. At or below the defense
grade they reenter the boss's `hurt` rules, including immunity, adaptation, cooldowns and the per-hit cap. A higher
canonical stage may force damage through that cap. Do not reintroduce `super.setHealth` in `publishVitality`.

Bind an explicit damage allowance covering the largest hit segment. Every `hurt` uses the same input ceiling,
with or without `beginHit`; there is no unscoped-hurt exemption. Final resolved damage uses
`vitalityController.commitDamage`, sharing the request's remaining allowance after mitigation and channel
aggregation. Healing and lifecycle record updates keep `commit`. Low-stage probe retries cannot reopen the same
request's allowance; the confirmed 100-health/8-limit sequence is 92 for both direct and probed 20-point hits,
and D4 may force all 20 points through D3 to reach 80.
Compensation does not raise the attack stage or reopen its allowance: D3 with compensation against D4 still
stops at 92; an actual escalation to D5 may legally settle the remaining 12 damage and reach 80.

### Age of Enlightenment is a melee multiplier, not a forced crit (`AgeOfEnlightenmentEvents#onMeleeHurt`)

It used to force a vanilla critical hit (`CriticalHitEvent`, `ALLOW` plus a 1.5× modifier). It is now a plain melee
damage multiplier: `LivingHurtEvent` doubles the pre-armour amount when the damage source's **direct entity is a
player** who has the effect. Melee only — the arcane channel, arrows and explosions carry someone else (or nothing) as
the direct entity and keep their own numbers; sweep damage rides the same `player_attack` source, so it doubles with
the swing. Deliberately given up with the crit: the crit particles and sound, and the fact that a forced crit
suppresses the scythe's sweep. A real vanilla crit (the falling attack) still multiplies by 1.5 first and is doubled
on top of that. The multiplier is `ageOfEnlightenment.meleeDamageMultiplier` in `MaledictConfig`, default 2.0.

### Cancelled events never reach `@SubscribeEvent` handlers unless the annotation opts in

`ASMEventHandler#invoke` checks `if (!event.isCanceled() || subInfo.receiveCanceled())` before calling the method, and
`SubscribeEvent#receiveCanceled` defaults to `false`. An annotated handler is therefore **skipped entirely** once an
earlier listener cancels the event, and priority cannot save it: `EventPriority.LOWEST` with a default annotation means
any "look at the already-cancelled event" branch is unreachable against cancels from NORMAL/HIGH. Programmatic
`IEventBus#addListener` reaches the same conclusion by a different route (the `checkCancelled` filter in `EventBus`).
A handler that wants to react to a cancellation must declare `receiveCanceled = true`; `BlissRuneEvents` needed it on all
three handlers, where the forced-hit path had been dead code.

### Registry events: attribute fires before mob_effect, but a `RegistryObject` is still dead until its own event

`GameData#postRegisterEvents` walks `MappedRegistry.getKnownRegistries()`, and Forge feeds that set from
`markKnown()` — called on the **first `register(...)` into a registry**, not from the registry's constructor. So the
order is "which vanilla bootstrap step first puts a value in", measured (temporary probe, datagen, 47.4.23) as:
`sound_event, fluid, block, attribute, mob_effect, particle_type, item, entity_type, …` — **attribute before
mob_effect**, so `AttributeRegistry.MALIGNANT_CONVERSION.get()` does resolve inside a `MobEffect` constructor.
Do not read that order out of `BuiltInRegistries`' source line numbers: they are declaration order and say nothing
(`MOB_EFFECT` is declared at line 123, `ATTRIBUTE` at 172, yet attributes register first because
`Bootstrap#bootStrap` → `DefaultAttributes#validate` forces `Attributes` to class-init early).

The rule that still holds regardless of order: a `RegistryObject` is null until its own registry event runs, and
`RegistryObject#get()` is `Objects.requireNonNull` — an NPE ("Registry Object not present"), not a null return.
Before reverse-engineering whether a foreign handle is usable at registry time, grep the repo for precedent:
`AgeOfDarknessEffect` has always read Malum's and Lodestone's attributes from its constructor, and `fallen`
(`MaledictMobEffects`, an anonymous subclass whose instance block adds the modifier) does the same for
`malum:malignant_conversion`. Level-ups go through Lodestone's `EntityHelper#amplifyEffect`/`extendEffect`, whose
`syncEffect` calls `onEffectUpdated` and therefore re-applies the modifier at the new level.

### Working agreement for mechanics with more than one reading

When a mechanic can be read in more than one way (window/eviction order, "record then judge" vs "judge then record",
counter lifetime, per-attacker vs global), write the intended model down first — the state transitions for a concrete
sequence — and have it confirmed before touching code. Do not infer the model from the feature's name or from how a
comparable mod does it. Then pin the confirmed sequence as a test case.

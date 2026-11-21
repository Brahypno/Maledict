# Repository Instructions

## Documentation map

- `docs/mechanics/` — confirmed player-visible mechanics and numbers, written for the author. When a change
  alters one of them, update the matching page in the same change. Code is the source of truth; a page that
  disagrees with it is a bug in the page.
- `docs/design/` — per-feature design and verification notes.
- `README.md` — project overview and layout.

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

## Settled contracts

Mechanics that were expensive to get right, reduced to what the code must keep doing. The player-visible side of
each one (numbers, formulas, observable behaviour) lives in the page named at the end of the entry; read it before
changing the mechanic.If they dont match, thats usually too lazy human dont chage doc

### Adaptation batching (`DamageAdaptation`)

A sliding window of the **most recently hit damage messages** (`DamageSource#getMsgId()`), size
`firstVicissitude.adaptationLevel`. Record first, then judge; only a message that was already in the window before
the hit is reduced, by `e^-(hits while remembered)`; an evicted message loses its counter.

The boss records through `adaptInBatch(message, level, tick)`: inside one tick a message that already landed is
**not recorded again** and **reuses the first call's multiplier**, because one swing lands the same message several
times (`DamageProbe` tops the amount up with the same source, and the arcane channel is cashed in inside the same
`LivingHurtEvent`). Per-call counting let one swing adapt itself. `RuneOfMelancholiaItem` deliberately keeps the
plain `adapt`, where every call counts.

Page: `docs/mechanics/first-vicissitude-adaptation.md`.

### Incursus Blade channels

- Arcane: `MaledictAttributes` registers Lodestone's `MAGIC_DAMAGE` with ChangeLib's `AttributeDamageRegistry`, and
  `LodestoneAttributeEventHandlerMixin` cancels Lodestone's own `processAttributes` copy when the weapon is in
  `maledict:lodestone_magic_takeover`. Keep the Lodestone request shape (`minecraft:magic`, null direct entity, the
  attacker as causing entity); do not bring back the old `markMeleeHurtEvent` compensation.
- Aqueous: `maledict:powder_snow_damage` is an attribute request registered with
  `AttributeAmountReaders.itemBacked()`, dispatched after the scythe probe (freeze, then magic).
- Multipart bosses: `IncursusBladeAttack.uniqueLivingTargets` collapses the melee and Ascension target lists onto
  the living root so one swing still opens one probe and one freeze/magic round.
- `DamageTier` sets the request's base `priority`/`strength` in `IncursusBladeEffects` on `DamageRequestEvent`
  (NORMAL) and `BlissRuneEvents` raises it one step (LOW). The unified `DamageRequestEvent` covers probe requests
  too; `attribute() != null` marks an attribute request.

Page: `docs/mechanics/incursus-blade-channels.md`.

### Boss contact (`VicissitudeBossEntity`)

`isPushable() == false` **and** the three-argument `push` no-op are both required. `LivingEntity#aiStep` ends with
`pushEntities()`, which shoves pushable pairs apart straight into `deltaMovement`, and the boss blends steering with
its previous velocity instead of overwriting it, so contact would walk it around. Ram attacks (ender dragon, ravager,
hoglin charge, warden sonic boom, moving minecarts) and other mods call `push` directly without ever asking
`isPushable`. Only the boss's own movement code may change its velocity; the wing push is a separate, deliberate
shove.

Page: `docs/mechanics/boss-contact.md`.

### Boss vitality (`VicissitudeBossEntity`)

- The world `SavedData` ledger is the only authority. The base implements `CombatProtection.OwnedHealthEntity`;
  `CombatProtection.bindOwned` runs exactly once in `onAddedToWorld`, after the record has been restored or
  initialised, and `ownsHealthAccess` compares by identity. Read health through `CombatProtection.healthView`;
  clients fall back to the synced `DISPLAY_VITALITY` only before ChangeLib's committed snapshot arrives.
- Every authority change commits one full record through the private controller: resolved damage uses
  `commitDamage`, everything else (healing, lifecycle) uses `commit`. The callback only writes the ledger and the
  capability mirror — publishing and events stay outside it. Do not reintroduce `super.setHealth` in
  `publishVitality`. `unprotect`, `protect`, `authorize` and the public `attack` all refuse an owned binding; the old
  rebase and per-tick grade check are gone.
- `setHealth` and storage damage submit requests to the bound authority. At or below the defense grade they reenter
  the boss's `hurt` rules (immunity, adaptation, cooldowns, per-hit cap); a higher canonical stage may force damage
  through that cap.
- Bind an explicit damage allowance covering the largest hit segment. Every `hurt` uses the same input ceiling, with
  or without `beginHit` — there is no unscoped-hurt exemption; resolved damage shares the request's remaining
  allowance after mitigation and same-tick channel aggregation. Low-stage probe retries cannot reopen the same
  request's allowance. Regression pins: the 100-health/8-limit sequence is 92 for both direct and probed 20-point
  hits; D4 may force all 20 points through D3 to reach 80; compensation alone does not raise the stage or reopen the
  allowance (D3 with compensation against D4 still stops at 92), while an actual escalation to D5 may legally settle
  the remaining 12 damage and reach 80.
- The pool is sized **once per UUID**, in `onAddedToWorld`, as `max(difficulty table, final MAX_HEALTH)`
  (`VicissitudeVitality.initialMaximum`). The difficulty value is a floor: a spawn modifier another mod already
  applied (L2Hostility and friends) lifts it, a reduction never undercuts it. `onAddedToWorld` is the last step of
  joining the world — `finalizeSpawn` and `EntityJoinLevelEvent` both precede it — so the attribute is final there.
  After capture the record is frozen: no per-tick recapture, no resize on later attribute changes, no load-time
  recapture or refill.

Page: `docs/mechanics/boss-vitality.md`.

### Age of Enlightenment (`AgeOfEnlightenmentEvents#onMeleeHurt`)

A plain melee multiplier on `LivingHurtEvent` (the damage source's direct entity is a player with the effect), not
a forced crit. Do not restore the `CriticalHitEvent` path: the lost crit particles and the fact that a forced crit
suppressed the scythe's sweep were given up deliberately. Config key `ageOfEnlightenment.meleeDamageMultiplier`.

Page: `docs/mechanics/age-of-enlightenment.md`.

### Lightward transfer (`IncursusBladeAttack#ownerOf`)

The 5-level reading of the Aufheben Lightward set: a melee swing with the Incursus Blade in the main hand that hits an
`OwnableEntity` also hits its owner. Invariants that must survive refactors:

- **One gate.** `LightwardSet#unlocksWeaponTopUp` is computed once per swing in `IncursusBladeAttack#perform` and feeds
  both readings — the probe top-up (`compensateDamage`) and the owner hit. Never introduce a second test for "is the set
  active here"; the tooltip shows one line for both.
- **The same blade, the same call.** The owner receives another `applyScytheMeleeDamage(..., damage, weaponTruth)`:
  same `SCYTHE_MELEE` source built the same way, same `DamageTier`/probe, same value, same top-up flag. Do not give the
  copy its own damage type, its own `damage_type` data, its own death message, or a separate dispatcher such as
  `MaledictProbeDamage` — that was built once and rejected.
- **Equal means the hit value, not the pet's health delta.** Never read the pet's health loss back to size the copy.
- **Melee swing only.** The thrown scythe and the Ascension rift do not transfer.
- **No self exemption and no recursion.** Hitting your own pet hurts you; the owner's hit never re-enters `perform`, so
  an ownership chain cannot cascade. Ownership is `getOwnerUUID()` resolved in the target's own dimension only.

Page: `docs/mechanics/lightward-transfer.md`.

### Cancelled events never reach `@SubscribeEvent` handlers unless the annotation opts in

`ASMEventHandler#invoke` checks `if (!event.isCanceled() || subInfo.receiveCanceled())` before calling the method, and
`SubscribeEvent#receiveCanceled` defaults to `false`. An annotated handler is therefore **skipped entirely** once an
earlier listener cancels the event, and priority cannot save it: `EventPriority.LOWEST` with a default annotation means
any "look at the already-cancelled event" branch is unreachable against cancels from NORMAL/HIGH. Programmatic
`IEventBus#addListener` reaches the same conclusion by a different route (the `checkCancelled` filter in `EventBus`).
A handler that wants to react to a cancellation must declare `receiveCanceled = true`; `BlissRuneEvents` needed it on
all
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
comparable mod does it. Then pin the confirmed sequence as a test case, record the model in `docs/mechanics/`, and
keep only the invariant that constrains the code in this file.

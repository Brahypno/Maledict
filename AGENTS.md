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
(Incursus Blade) lands three messages per swing — the melee `scythe_melee` hit plus the two attribute requests
(`minecraft:magic` from the arcane channel and `minecraft:freeze` from the aqueous one) — so under the default of 2
its damage is never adapted.

### Incursus Blade's arcane channel (`can_trigger_magic_damage`)

The blade's arcane infusion (`MAGIC_DAMAGE` attribute) deals no damage by itself. Lodestone 1.8.2 cashes it in from
`LodestoneAttributeEventHandler#triggerMagicDamage(LivingDamageEvent.Post)` — **not** from `processAttributes`:
if the source type is in `can_trigger_magic_damage` (Malum puts `malum:scythe_melee` and `malum:scythe_sweep` there,
and `c:can_trigger_magic_damage` carries `minecraft:player_attack`) it deals one extra `minecraft:magic` hit worth
the attribute, with the attacker as both causing entity and direct entity
(`DamageTypeHelper.create(MAGIC, attacker)`). `ignores_magic_attack_cooldown_scalar` is empty, so Lodestone's copy
*is* scaled by attack strength; Maledict's takeover copy (below) is not. `processAttributes(Pre)` only applies the
`MAGIC_RESISTANCE` / `MAGIC_PROFICIENCY` scalings and deals nothing, so cancelling it is a no-op for a non-magic
source — that mistake made the channel deal **double** until 2026-11 (see the two-half entry below).

Consequence: a melee hit that lands **without** running `actuallyHurt` — the damage probe falling back to
`setHealth`, i-frames, immunity, a cancelled attack event — silently loses that magic damage. `IncursusBladeAttack`
probes for exactly that: `IncursusBladeItem#hurtEvent` reports back through `markMeleeHurtEvent` during the hit's
own resolution, and a miss is compensated with the same magic damage. The aqueous (freeze) channel needs no such
compensation: it is the `powder_snow_damage` attribute (next-but-one entry), one request per landed probe, and a probe
that lands nothing flushes nothing for either channel.

### Incursus swing range (`IncursusSwingReach`)

- **One radius per swing.** `entity_reach` (1.21: `Attributes.ENTITY_INTERACTION_RANGE`) `+ 0.5 + sweeping × 0.25`,
  clamped to `MAX_RADIUS` (6.0), with non-finite or non-positive input collapsing to 0.
  `IncursusBladeAttack#perform` computes it once; the hit test and the `IncursusSlashPacket` payload both read that
  number, and the client re-applies `clampRadius` on the wire value. Never let a second formula answer "how far does
  this swing reach".
- **The drawn ring is the damage sphere's equator.** Centre is the attacker's waist (`centerOf`, the same
  algorithm as the client's `anchorOf`), the target is hit when the closest point of its bounding box is within
  the radius, and the melee petal centres land on the radius at the end of the bloom.
- Do not go back to the feet point plus half the width: it missed elevated targets and oversized hitboxes.
- **Charge never touches the radius.** The packet carries the attack strength the swing was settled at, and
  `IncursusRiftGeometry` turns it into opacity (0.30–1.0) and lifetime (0.55–1.0×) only. Future charge feedback
  belongs on opacity, lifetime, slit width or stroke count — never on the ring's size, or the mismatch this
  entry exists to prevent comes straight back. A non-finite charge reads as full charge, not as a blank effect.

Page: `docs/mechanics/incursus-swing-range.md`.

### Incursus swing input (`IncursusBladeClientEvents`)

- **One press is one swing.** NeoForge fires `InputEvent.InteractionKeyMappingTriggered` from two places:
  `Minecraft#startAttack` for a click, and `Minecraft#continueAttack` **once per tick** while the attack key is held
  on a non-air block. The latch in `onInteractionKey` must refuse the repeated ticks — no `RadialAttackPacket`, no
  `resetAttackStrengthTicker`, and an explicit `setSwingHand(false)`: the swing flag defaults to `true` and applies
  whether or not the event is cancelled, so a bare cancel still swings the arm every tick. Release the latch only
  when the key comes up (`ClientTickEvent.Post`).
- **The damage layer has no frequency gate, on purpose.** Ignoring i-frames (the arcane hop clears
  `invulnerableTime`) and unscaled arcane/freeze channels are privileges. Never answer a swing-rate problem with a
  minimum-interval gate in `IncursusBladeAttack#perform`: that deletes the privilege rather than fixing the input.
  A real click rate above the weapon's 0.7/s is legal; its DPS consequence is recorded, not compensated.

Page: `docs/mechanics/incursus-swing-rate.md`.

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

### AI protection silently drops direct AI writes (`VicissitudeBossEntity`)

`VicissitudeBossEntity` implements `ProtectedAiEntity` and binds `AiProtection` in `onAddedToLevel`
(1.20.1's `onAddedToWorld`), with `(protectNoAi, protectTarget, protectGoals) = (true, true, true)`. Once bound,
ChangeLib's `AiMobMixin` / `AiGoalSelectorMixin` sits in front of **every** `Mob#setTarget`, `Mob#setNoAi` and
`GoalSelector` add/remove/control-flag write: an owner write must carry the controller's pending permit, and any
other write is dropped and merely counted in `AiProtection.State#rejectedWrites()`. **Nothing throws** — a goal
that calls `setTarget` on a boss just never gets a target, and the boss keeps whatever it had (or `null`).

So the boss's own target writes all go through `selectAiTarget` (which falls back to `super.setTarget` when the
controller is not bound, i.e. the client); the Demiurge's goals use the package-private
`DemiurgeStatueEntity#selectTargetFromGoal`. Target rules are per entity through `canHoldAiTarget`: the base only
checks "not self, alive, not removed, same level", the First Vicissitude boss additionally requires the candidate to
be in the current roster (`phaseOneTargets` / `phaseTwoParticipants`), the statue additionally requires it to be
non-allied and attackable. A new goal or mechanic that retargets a boss without going through `selectAiTarget` is
dead on arrival, without an error.

Testing this needs the guard's own counter, not the visible state: the boss already overrides `setNoAi` to always
write `false` and `isNoAi()` to always return `false`, so "AI was not disabled" asserts nothing. Read
`AiProtection.state(mob).rejectedWrites()` around the write instead (see
`FirstVicissitudeEntityTest#bystandersStaySafeAndCreativePlayersLoseAggroInBothPhases`).

### Hit rhythm belongs to ChangeLib's binding, and a resumed pool holds a load grace (`VicissitudeBossEntity`)

The second gate after vanilla `invulnerableTime` is **not** state of ours. `CombatProtection.bindOwned` creates a
`Rhythm` inside the binding, and `Controller#interval(getVitalityHitInterval())` configures it as a *refusing* window
(`linear = false`: a hit inside it takes no health at all). The window is stamped where the loss lands, so the landed
loss must be committed through `Controller#commitDamage` (`VicissitudeBossEntity#applyAuthorityDamage`) — a plain
`Controller#commit` would move the health and leave the rhythm unstamped. `hurt` asks the gate read-only with
`CombatProtection.rhythmRemaining(this) > 0`, never a field of its own: `VicissitudeVitality` carries no `gateOpensAt`,
the vitality ledger does not save one, and old saves' `GateOpensAt` key is ignored on load. Rhythm is lived state; a
reload starts a fresh gate instead of restoring a cadence nobody could observe.

That fresh gate is the **load grace**: `onAddedToLevel` marks `resumed` when the ledger already had this UUID, and
`bindVitalityProtection` then calls `Controller#hold(getLoadGraceTicks())` (20). `hold` stamps the window one tick
behind "now", so the first hit of that moment is refused rather than free (a summon is not a resume — its rite carries
its own immunity). Two consequences that cost a round:

- **Never test the grace by sleeping it out.** Out-of-combat recovery refills `BossRecovery.HEAL_FRACTION_PER_TICK`
  (5% of the pool) *per tick*, so a 5-health pool on a 1000-pool boss is full again in two ticks and a test that waits
  20 ticks is testing a healthy boss. Open the gate explicitly instead:
  `CombatProtection.shortenRhythm(boss, Integer.MAX_VALUE)` (see `VicissitudeVitalityTest`).
- A refusing window means the *write* is clamped too (`clampTarget` multiplies the loss by the window's fraction), so
  a mechanic that must punch through the rhythm without reconfiguring it (a skill, an enchantment, a heavy strike)
  calls `Controller#shorten` / `CombatProtection.shortenRhythm`, which cuts the running window and leaves the configured
  length alone.

### Age of Enlightenment is a melee multiplier, not a forced crit (`AgeOfEnlightenmentEvents#onMeleeHurt`)

It used to force a vanilla critical hit (`CriticalHitEvent`, `ALLOW` plus a 1.5× modifier). It is now a plain melee
damage multiplier: `LivingIncomingDamageEvent` (1.21's slot for 1.20.1's `LivingHurtEvent` — both fire before
armour absorbs) doubles the pre-armour amount when the damage source's **direct entity is a
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

### `HIDE_ADDITIONAL_TOOLTIP` does not hide the enchantment rows (`ItemStack#getTooltipLines`)

In 1.21.1 the only tooltip-hiding components are `HIDE_TOOLTIP` and `HIDE_ADDITIONAL_TOOLTIP`; there is no
1.20.1-style `Item#getDefaultTooltipHideFlags` / `TooltipPart.ENCHANTMENTS`. `HIDE_ADDITIONAL_TOOLTIP` guards **only**
`Item#appendHoverText` — the `addToTooltip(...)` calls that follow it (`TRIM`, `STORED_ENCHANTMENTS`,
`ENCHANTMENTS`, `DYED_COLOR`, `LORE`, NeoForge's attribute block) run unconditionally. So an item that redraws its
own enchantment lines in `ItemTooltipEvent` (Lightward armor and weapons) printed every enchantment **twice**, once
raw from the component and once custom, whether or not the level bonus was active.
`LightwardSet#stripVanillaEnchantments` fixes it by recomputing `Enchantment.getFullname(holder, rawLevel)` for each
tag enchantment and removing exactly those components from the tooltip before the custom lines are appended —
`MutableComponent` has a structural `equals`, so the match is exact even when a translation is missing. This is the
same locate-by-recomputation pattern Apotheosis uses in `AdventureModuleClient#affixTooltips` to star-prefix the
attribute rows of over-max affixes: there is no hook that lets an item suppress or rewrite another provider's tooltip
line, so recompute-and-match is the supported way.

### Gameplay enchantment levels go through `GetEnchantmentLevelEvent`, not item overrides

`ItemStack#getEnchantmentLevel` / `getAllEnchantments` first ask the item's own declaration
(`IItemExtension#getEnchantmentLevel` / `#getAllEnchantments`, defaults reading `getTagEnchantments()`) and then post
`GetEnchantmentLevelEvent` (game bus, both sides) with a mutable `ItemEnchantments`; the vanilla enchantment readers
(`EnchantmentHelper.runIterationOnItem`, the `EnchantmentHelper` helpers Neo patched) go through the stack accessors,
so a listener reaches every caller at once. The per-enchantment path posts a table holding **only** the target at its
declared level — level 0 is already dropped, so an existing-but-boosted rule must skip absent enchantments rather than
add them. Apotheosis's `EnchantmentAffix` / `EnchantmentBonus` use exactly this. Prefer one listener over overriding
`IItemExtension` per item class: the 1.20.1 port overrode the hooks on the armor *and* on both weapon classes, and
only the armor made it across, which silently disabled the weapon bonus. The event's own javadoc says it is not fired
for NBT, i.e. the tooltip still shows raw levels and any merged display has to be drawn by hand.

### Server-thread reverse lookup is dead inside a tooltip (`LightwardSet#activeOwner`)

Enchantment reads (now via `GetEnchantmentLevelEvent`) get no owner, so the Lightward set finds the wearer by scanning
`ServerLifecycleHooks.getCurrentServer().getPlayerList()`. That lookup requires `MinecraftServer#isSameThread`, and a
tooltip is built on the **render thread** — never the server thread, even in singleplayer — so the scan always came
back empty and the enchantment bonus was 0 client-side: the enchantment row printed its unboosted level, i.e. exactly
the line vanilla had already printed. `activeOwner` therefore falls back to `LightwardClientState#activeOwnerFor`
(`Minecraft#player`) guarded by `FMLEnvironment.dist == Dist.CLIENT`; the guard must stay *inside* the helper so a
dedicated server never loads the client class. Any client-visible derived value that is computed from a per-entity
reverse lookup needs the same two-branch shape.

### The arcane channel is a two-half arrangement: port both halves or neither

Lodestone 1.8.2 cashes an attacker's `MAGIC_DAMAGE` attribute into an extra `minecraft:magic` hit from
`LodestoneAttributeEventHandler#triggerMagicDamage(LivingDamageEvent.Post)`. Maledict takes that hit over for its own
weapons: `MaledictAttributes#registerAttributeDamage` registers `LodestoneAttributes.MAGIC_DAMAGE` with ChangeLib's
`AttributeDamageRegistry` — gated on the `maledict:lodestone_magic_takeover` **item tag** and on the original source
being in `can_trigger_magic_damage` but not `is_magic` — and `LodestoneAttributeEventHandlerMixin` cancels Lodestone's
copy so the swing does not deal both. Each half is wrong on its own: the mixin alone makes the arcane channel deal
**zero**, the registry alone makes it deal **double**. The 1.21.1 port had neither, which is why the channel silently
fell back to Lodestone's native hit.

**The injection point is `triggerMagicDamage(Post)`, and getting it wrong looks like a working port.**
`processAttributes(LivingDamageEvent.Pre)` in the same class deals no damage at all (it only applies `MAGIC_RESISTANCE`
and `MAGIC_PROFICIENCY`), and a non-magic source exercises neither branch — so a mixin injected there applies cleanly,
never throws, and cancels nothing. The port shipped with exactly that mistake and the channel read **double** for a
whole round: both GameTests that count a lover-axe click (`eleven melee + one magic`) were off by one health point,
and the failure was written down as pre-existing. Verify a takeover mixin by the channel count, not by "the mixin
applied": one sword swing must produce exactly one `magic` `LivingDamageEvent`, with `directEntity == null`.

`MaledictAttributes` is annotated `@EventBusSubscriber(modid = …)` with no `bus`: `FMLCommonSetupEvent` and
`EntityAttributeModificationEvent` implement `IModBusEvent`, and 21.1 ignores the `bus` attribute (it is deprecated
for removal).

### The aqueous channel is `maledict:powder_snow_damage`, and every explicit copy of it is a bug

The freezing channel is an **attribute**, never an explicit probe: the blade's main-hand modifier
(`maledict:incursus_blade/powder_snow_damage`, value = the blade's `powder_snow_damage` stat) writes it onto the
attacker, and `MaledictAttributes` registers it with ChangeLib's `AttributeDamageRegistry` as `minecraft:freeze` +
`AttributeAmountReaders.itemBacked()` at D0/RESPECT with the default `allowAll` trigger. Every landed probe therefore
flushes one freeze request per (attacker, target, source), which is why the melee swing, the thrown scythe, the
Ascension sweep, the Diplopia phantoms and the Lightward owner copy all carry it **without a line of code each**.

Consequences that must not be undone:

- **Do not "add the freezing hit" anywhere.** A second explicit freeze probe lands, and its own flush then dispatches
  *another* freeze request. `DamageChannels` has exactly `(attack, magic)` for this reason; 1.20.1's record never had
  a `frozen` field, and its boomerang/ascension deal only the sweep hit.
- **The tier comes from `DamageRequestEvent`, not from the call site.** `IncursusBladeEffects#applyFreezeAttributePolicy`
  raises the attribute request's ceiling and penetration to the blade's own tier
  (`DamageTier#probeStage` / `#probeStrength`, LIGHT = D3/RESPECT, FINAL = D4/IGNORE_DECLARATION). A thrown scythe
  still gets the right tier because `IncursusScytheBoomerangEntity` puts the blade back in the main hand before it
  probes, and answers `AttackItemProvider#getAttackItem` so `itemBacked()` reads the thrown stack.
- **Owned health pools: the tier only sets the request's ceiling; ChangeLib settles it.** `DamageTier#deal(…,
  internalGrade)` attaches `DamagePolicy.through(D3|D5)` (LIGHT = D3, FINAL = D5) to a Maledict
  `OwnedHealthEntity` (namespace-checked; other mods' owned pools keep the normal probe) when that grade penetrates
  `CombatProtection.defenseGrade`. That is the whole contract: **escalation, health writes and settlement belong to
  ChangeLib**, so do not pre-check immunity, defence or "should this land" here — a weapon that settles its own damage
  is the bug this rule exists to prevent. Two measured consequences worth knowing: a plain medium probe already
  reaches a D4 owned pool (so the grade is an escalation path, not a pass/fail switch), and a D5 ceiling *can* write
  through an entity's own `isDamageImmune` (a First Vicissitude in phase one took 5). That second one is ChangeLib's
  layer, not this repo's — 1.20.1 behaves identically — so fix it there rather than gating the blade.
- **The lover axe is deliberately outside that table**: it always probes at medium (`mediumDamageMethod`, no grade),
  exactly as 1.20.1 did.

### Malum's `mostRecentScreen` will hand the Esoterica book our pages (`LightfallCodex`)

1.8.2's `ProgressionScreenHolder#openCodex` / `#openCodexViaTransition` write the opened holder into
`ProgressionScreenHolder.mostRecentScreen`, and Malum's 奥秘全典 (`EncyclopediaEsotericaItem`) opens
`getAppropriateCodexScreen()` — that field, or the Arcana book when it is null. 奥术全典 (`EncyclopediaArcanaItem`)
hardcodes Arcana and is unaffected. A Maledict screen that opens through its holder therefore silently captures the
Esoterica item: after one 坠光秘典 use, right-clicking the ordinary 奥秘全典 lands on the Lightfall pages. Both paths
must keep the field untouched — `LightfallCodex#withoutBecomingMalumsMostRecent` for the item opens and
`MaledictScreenOpener` for every in-book node (with the pre-open side effect, e.g. `seeTheRevelation`, passed as a
`Runnable`). The book's own memory lives in `LightfallCodex.view` instead: the item reopens the remembered view, and
every in-book node records its destination through `rememberViewFor` — 1.20.1 got that for free because its nodes
called `openCodexViaItem`, while 1.8.2's nodes are holder transitions that never reach our code, so
"reopen where I was" needs that extra step. `CodexEntryScreen` is not a consumer: entry pages always carry their
parent screen and close back through `ProgressionScreenHolder.openCodex(screen, …)`, which does not touch the field.

### The third book's two views are full mirrors (`LightfallVoidScreen`, `LightfallArcanaScreen`)

Both mirror views copy their book's whole entry list and replace only the return node — the Void view copies
`VoidProgressionScreen`, the Arcana view copies `ArcanaProgressionScreen`. That includes Maledict's own entries
(`void.maledict.elegy_bow`, `void.maledict.incursus_blade`, `maledict.corrupt_sacrifice_rite`, `maledict.remembrance_bow`,
…). A filter that skipped `maledict.` / `void.maledict.` (inherited from 1.20.1, where both views had it) left the
mirror without the content its book actually has, which reads as a codex that lost part of its contents. The entries are
shared `PlacedBookEntry` objects, not copies — only the drawn widgets are per screen (`EntryObjectHandler`).

### Working agreement for mechanics with more than one reading

When a mechanic can be read in more than one way (window/eviction order, "record then judge" vs "judge then record",
counter lifetime, per-attacker vs global), write the intended model down first — the state transitions for a concrete
sequence — and have it confirmed before touching code. Do not infer the model from the feature's name or from how a
comparable mod does it. Then pin the confirmed sequence as a test case.

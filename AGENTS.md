# Repository Instructions

## Generated resources

- Never edit any file under `src/generated` directly, including cleanup, formatting, restoration, or one-line
  corrections.
- Change the responsible data-generator source, then run `runData` to update `src/generated`.
- Treat every change produced by `runData` as generator output. If that output is wrong or unexpectedly broad, fix the
  generator and rerun it; do not repair the generated files manually.

## Development Notes

- Chinese text for Malum codex body pages must contain a literal space after every 13 visible characters. Punctuation
  counts toward the 13-character limit; the inserted layout spaces do not.
- Write the Chinese codex copy first, then derive the English localization from its meaning.

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
(Incursus Blade) lands three messages per swing (`scythe_sweep` / `voodoo` / `freeze`), so under the default of 2 its
damage is never adapted.

### Incursus Blade's arcane channel (`can_trigger_magic_damage`)

The blade's arcane infusion (`MAGIC_DAMAGE` attribute) deals no damage by itself. Lodestone's
`LodestoneAttributeEventHandler#processAttributes` cashes it in **inside the victim's `LivingHurtEvent`**: if the
source type is in `forge:can_trigger_magic_damage` (Malum puts `malum:scythe_melee` and `malum:scythe_sweep` there)
it deals one extra `minecraft:magic` hit worth the attribute, with a null direct entity and the attacker as causing
entity, right after setting `invulnerableTime = 0`. `forge:ignores_magic_attack_cooldown_scalar` is empty, so that
bonus is never scaled by attack strength.

Consequence: a melee hit that lands **without** firing `LivingHurtEvent` — the damage probe falling back to
`setHealth`, i-frames, immunity, a cancelled attack event — silently loses that magic damage. `IncursusBladeAttack`
probes for exactly that: `IncursusBladeItem#hurtEvent` reports back through `markMeleeHurtEvent` during the hit's
own resolution, and a miss is compensated with the same magic damage. The aqueous (freeze) channel left
`hurtEvent` for the same reason: it is dealt by `IncursusBladeAttack` now, not by whatever event happens to fire.

### Working agreement for mechanics with more than one reading

When a mechanic can be read in more than one way (window/eviction order, "record then judge" vs "judge then record",
counter lifetime, per-attacker vs global), write the intended model down first — the state transitions for a concrete
sequence — and have it confirmed before touching code. Do not infer the model from the feature's name or from how a
comparable mod does it. Then pin the confirmed sequence as a test case.

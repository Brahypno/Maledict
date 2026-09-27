// Offline throughput probe for the First Vicissitude encounter.
//
// Mirrors the intent logic in FirstVicissitudeBossEntity (tickCooldowns / tickPhaseOneCombat /
// tickPhaseTwoCombat / commandMelee) tick for tick, so the numbers below are the boss' authored
// output rather than a hand estimate. It models a target that stays in the band each phase wants
// to fight from, never dies and never dodges: an upper bound on sustained output.
//
// Usage: node tools/dps-probe/boss-dps.mjs

const PHASE_ONE_WARMUP_TICKS = 20;
const BASE_ATTACK_INTERVAL_TICKS = 30;
const WING_BARRAGE_COOLDOWN = 70;
const CHEST_CAST_COOLDOWN = 90;
const HALO_CAST_COOLDOWN = 80;
const SLASH_COOLDOWN = 20;
const VERTICAL_SLASH_COOLDOWN = 30;
const HEAVY_ATTACK_COOLDOWN = 120;
const DASH_COOLDOWN = 160;
const THROW_COOLDOWN = 120;
const RANGED_FALLBACK_COOLDOWN = 60;

const MELEE_COMMIT_RANGE = 5.75;
const THROW_MIN_RANGE = 6.0;
const THROW_MAX_RANGE = 24.0;
const DASH_MIN_RANGE = 8.0;
const DASH_MAX_RANGE = 20.0;

// Action timings: [duration, releaseTick].
const ACTION = {
    NONE: [0, 0],
    WING_RANGED: [20, 12],
    WING_BARRAGE: [56, 20],
    CAST_FROM_CHEST: [50, 30],
    CAST_FROM_HALO: [60, 40],
    SLASH_HORIZONTAL: [20, 8],
    SLASH_VERTICAL: [30, 14],
    HEAVY_ATTACK: [50, 24],
    DASH: [50, 20],
    SCYTHE_THROW: [28, 16],
    SCYTHE_RECOVER: [10, 0],
    RANGED_FALLBACK: [28, 16],
};

const MELEE_MULTIPLIER = {
    SLASH_HORIZONTAL: 1.0,
    SLASH_VERTICAL: 1.25,
    HEAVY_ATTACK: 1.75,
};

// ATTACK_DAMAGE attribute: base 8 plus the baked weapon modifier.
const DIFFICULTY = {
    SIMPLE: { attack: 13, keys: ['soul_stained_steel_scythe (+5)'] },
    DIFFICULT: { attack: 17, keys: ['edge_of_deliverance (+9)'] },
    COMPLETE: { attack: 11, keys: ['incursus_blade earthen 3 (+3)'] },
    EXTREME: { attack: 17, keys: ['incursus_blade earthen 9 (+9)'] },
};

const BARRIER = 'BARRIER'; // the throw gate; every model below walks the same decision order

/** Damage a single release deals to one target, per difficulty. */
function hitDamage(action, attack) {
    switch (action) {
        case 'CAST_FROM_CHEST':
        case 'CAST_FROM_HALO':
            return attack;                       // chest mark and halo verdict: 1D
        case 'SCYTHE_THROW':
            return attack;                       // outbound 1D, the return leg deals nothing
        case 'DASH':
            return attack;                       // once per target per dash
        case 'RANGED_FALLBACK':
            return attack * 0.75;                // three bolts, one damage instance per round
        case 'SLASH_HORIZONTAL':
        case 'SLASH_VERTICAL':
        case 'HEAVY_ATTACK':
            return attack * MELEE_MULTIPLIER[action];
        default:
            return 0;
    }
}

class Boss {
    constructor(attack, rangedBand, bandFor = null) {
        this.attack = attack;
        this.rangedBand = rangedBand;            // how far the model keeps the target
        this.bandFor = bandFor;                  // optional per-tick override, for the hybrid model
        this.cooldowns = {
            barrage: 0, chest: 0, halo: 0, slash: 0, vertical: 0,
            heavy: 0, dash: 0, throw: 0, ranged: 0,
        };
        this.action = 'NONE';
        this.actionTicks = 0;
        this.specialRotator = 0;
        this.meleeAlternator = 0;
        this.baseSlotIndex = 0;
        this.fanCount = 0;
        this.phaseOneTicks = 0;
        this.roundDamaged = new Set();           // cleared on startAction
        this.dealt = 0;
        this.hits = [];
        this.uptime = 0;
    }

    tickCooldowns() {
        for (const key of Object.keys(this.cooldowns)) {
            this.cooldowns[key] = Math.max(0, this.cooldowns[key] - 1);
        }
    }

    startAction(action) {
        this.action = action;
        this.actionTicks = 0;
        this.roundDamaged.clear();
    }

    endAction() {
        this.action = 'NONE';
        this.actionTicks = 0;
    }

    landed(action, count = 1) {
        for (let i = 0; i < count; i++) {
            this.dealt += hitDamage(action, this.attack);
        }
        this.hits.push(action);
    }

    /**
     * One damage instance, modelled as landing on a single target that never dodges.
     *
     * Melee is special-cased: the mass of the scythe makes the target's 20 tick i-frame gate close,
     * so each swing lands exactly once. The damage frame itself lives inside the blade's hit window
     * (FirstVicissitudeBossEntity#tickBladeHits), not in releaseAction, but per round the outcome is
     * one instance per target either way, so the release frame is used as its stamp.
     */
    release() {
        const action = this.action;
        switch (action) {
            case 'SCYTHE_THROW':                     // outbound 1D; the return leg deals nothing
            case 'RANGED_FALLBACK':                  // three bolts, one round instance
            case 'CAST_FROM_CHEST':
            case 'CAST_FROM_HALO':
            case 'DASH':
            case 'SLASH_HORIZONTAL':
            case 'SLASH_VERTICAL':
            case 'HEAVY_ATTACK':
                this.landed(action);
                return;
            case 'WING_RANGED':
            case 'WING_BARRAGE':
                // Phase one: every bolt presses health to 1, no damage. Phase two: the same bolts
                // carry attackDamage * 0.75, one instance per round.
                if (this.phaseTwo) {
                    this.landed(action);
                }
                return;
            default:
        }
    }

    /**
     * Advance one tick. The spacing probe runs before the intent (to say how far away the target is)
     * and again after the action update (so it can see an action that started or ended this tick).
     */
    tick(tickIndex, phaseTwo) {
        this.phaseTwo = phaseTwo;
        this.tickCooldowns();
        if (phaseTwo) {
            this.sampleBand();
            this.phaseTwoTick();
        } else {
            this.phaseOneTick();
        }
        if (this.action !== 'NONE') {
            this.uptime++;
            const [duration, releaseTick] = ACTION[this.action];
            if (this.actionTicks === releaseTick) {
                this.release();
            }
            // one damage instance per target per action round; the release frames are the only
            // frames that deal damage in this model, so no per-tick dedupe is needed here.
            if (this.actionTicks >= duration - 1) {
                this.endAction();
            } else {
                this.actionTicks++;
            }
        }
        if (phaseTwo) {
            this.sampleBand();
        }
    }

    sampleBand() {
        this.band = this.bandFor === null ? this.rangedBand : this.bandFor(this);
        if (this.band > MELEE_COMMIT_RANGE) {
            this.kiteTicks = (this.kiteTicks ?? 0) + 1;
        }
    }

    phaseOneTick() {
        this.phaseOneTicks++;
        if (this.action !== 'NONE') {
            return;
        }
        if (this.phaseOneTicks < PHASE_ONE_WARMUP_TICKS) {
            return;
        }
        if ((this.phaseOneTicks - PHASE_ONE_WARMUP_TICKS) % BASE_ATTACK_INTERVAL_TICKS !== 0) {
            return;
        }
        const slot = this.baseSlotIndex++;
        if (slot % 2 === 0 && this.tryRangedSpecial()) {
            return;
        }
        this.fanCount++;
        this.startAction('WING_RANGED');
    }

    tryRangedSpecial() {
        for (let attempt = 0; attempt < 3; attempt++) {
            const choice = this.specialRotator % 3;
            this.specialRotator++;
            if (choice === 0) {
                if (this.cooldowns.barrage <= 0) {
                    this.cooldowns.barrage = WING_BARRAGE_COOLDOWN;
                    this.startAction('WING_BARRAGE');
                    return true;
                }
            } else if (choice === 1) {
                if (this.cooldowns.chest <= 0) {
                    this.cooldowns.chest = CHEST_CAST_COOLDOWN;
                    this.startAction('CAST_FROM_CHEST');
                    return true;
                }
            } else if (this.cooldowns.halo <= 0) {
                this.cooldowns.halo = HALO_CAST_COOLDOWN;
                this.startAction('CAST_FROM_HALO');
                return true;
            }
        }
        return false;
    }

    phaseTwoTick() {
        if (this.action !== 'NONE') {
            return;
        }
        const distance = this.band;
        if (distance > THROW_MAX_RANGE) {
            if (this.tryRangedSpecial()) {
                return;
            }
            if (this.cooldowns.ranged <= 0) {
                this.cooldowns.ranged = RANGED_FALLBACK_COOLDOWN;
                this.startAction('RANGED_FALLBACK');
            }
            return;
        }
        if (distance >= THROW_MIN_RANGE) {
            if (this.cooldowns.throw <= 0) {
                this.cooldowns.throw = THROW_COOLDOWN;
                this.startAction('SCYTHE_THROW');
                return;
            }
            if (this.cooldowns.dash <= 0
                && distance >= DASH_MIN_RANGE && distance <= DASH_MAX_RANGE) {
                this.cooldowns.dash = DASH_COOLDOWN;
                this.startAction('DASH');
                return;
            }
            if (this.tryRangedSpecial()) {
                return;
            }
            if (this.cooldowns.ranged <= 0) {
                this.cooldowns.ranged = RANGED_FALLBACK_COOLDOWN;
                this.startAction('RANGED_FALLBACK');
            }
            return;
        }
        if (distance <= MELEE_COMMIT_RANGE) {
            this.commandMelee();
        }
    }

    commandMelee() {
        if (this.cooldowns.heavy <= 0) {
            this.cooldowns.heavy = HEAVY_ATTACK_COOLDOWN;
            this.startAction('HEAVY_ATTACK');
            return;
        }
        if ((this.meleeAlternator + 1) % 3 === 0) {
            if (this.cooldowns.vertical > 0) {
                return;
            }
            this.cooldowns.vertical = VERTICAL_SLASH_COOLDOWN;
            this.meleeAlternator++;
            this.startAction('SLASH_VERTICAL');
            return;
        }
        if (this.cooldowns.slash > 0) {
            return;
        }
        this.cooldowns.slash = SLASH_COOLDOWN;
        this.meleeAlternator++;
        this.startAction('SLASH_HORIZONTAL');
    }
}

function run(label, difficulty, rangedBand, phaseTwo, ticks, bandFor = null, lead = 0) {
    const boss = new Boss(DIFFICULTY[difficulty].attack, rangedBand, bandFor);
    boss.phaseOneTicks = phaseTwo ? 0 : lead;
    const counts = new Map();
    for (let i = 0; i < ticks; i++) {
        boss.tick(i, phaseTwo);
    }
    for (const hit of boss.hits) {
        counts.set(hit, (counts.get(hit) ?? 0) + 1);
    }
    const order = [...counts.entries()].sort((a, b) => b[1] - a[1]);
    const seconds = ticks / 20;
    return {
        label,
        difficulty,
        attack: DIFFICULTY[difficulty].attack,
        ticks,
        seconds,
        totalDamage: round2(boss.dealt),
        dps: round2(boss.dealt / seconds),
        actions: boss.hits.length,
        actionsPerSecond: round3(boss.hits.length / seconds),
        averagePerHit: boss.hits.length ? round2(boss.dealt / boss.hits.length) : 0,
        actionUptime: round3(boss.uptime / ticks),
        kiteShare: round3((boss.kiteTicks ?? 0) / ticks),
        breakdown: order.map(([action, n]) => ({
            action,
            count: n,
            damageEach: round2(hitDamage(action, boss.attack)),
            total: round2(n * hitDamage(action, boss.attack)),
            share: round3((n * hitDamage(action, boss.attack)) / boss.dealt),
        })),
    };
}

function round2(value) {
    return Math.round(value * 100) / 100;
}

function round3(value) {
    return Math.round(value * 1000) / 1000;
}

const MINUTE = 1200;
const results = [];
for (const difficulty of Object.keys(DIFFICULTY)) {
    // Phase one starts at tick 20 of the encounter proper; the lead makes the 60 s windows line up.
    results.push(run(`phase one (${difficulty})`, difficulty, 7.0, false, MINUTE, null, 20));
}
for (const difficulty of Object.keys(DIFFICULTY)) {
    results.push(run(`phase two melee band (${difficulty})`, difficulty, 4.0, true, MINUTE));
    results.push(run(`phase two throw band (${difficulty})`, difficulty, 12.0, true, MINUTE));
    results.push(run(`phase two far band (${difficulty})`, difficulty, 40.0, true, MINUTE));
}

/**
 * The encounter's own spacing, in the model's own terms: the boss keeps 12 blocks of distance while
 * its scythe is out of its hand, and 4 (melee) once it is holding the weapon again.
 *
 * A throw leaves the hand for the 28 tick throw action plus the return leg; the return leg is fixed
 * at {@code returnTailTicks}, and {@code roamingTicks} is how long the boss then keeps orbiting at
 * range before drifting back in. The throw cooldown (120 ticks) is the real brake on the cycle.
 */
function encounterBand(returnTailTicks, roamingTicks) {
    return (boss) => {
        if (boss.action === 'SCYTHE_THROW' && boss.actionTicks <= 1) {
            boss.scytheOut = true;
            boss.kiteLeft = ACTION.SCYTHE_THROW[0] + returnTailTicks + roamingTicks;
        }
        if (boss.scytheOut) {
            if (boss.kiteLeft > 0) {
                boss.kiteLeft = boss.kiteLeft - 1;
            } else {
                boss.scytheOut = false;
            }
        }
        return boss.scytheOut ? 12.0 : 4.0;
    };
}

/** Same state machine, but the kite leg cannot be entered until the first throw has been made. */
function encounterBandFromFirstThrow(returnTailTicks, roamingTicks) {
    let primed = false;
    const inner = encounterBand(returnTailTicks, roamingTicks);
    return (boss) => {
        if (!primed) {
            if (boss.action === 'SCYTHE_THROW' || boss.action === 'DASH') {
                primed = true;
            } else {
                return 12.0;                     // still closing in for the opening throw
            }
        }
        return inner(boss);
    };
}

for (const result of results) {
    console.log(`\n=== ${result.label} ===`);
    console.log(`attack damage D = ${result.attack}`);
    console.log(`damage ${result.totalDamage} over ${result.seconds}s `
                + `=> dps ${result.dps}, actions/s ${result.actionsPerSecond}, `
                + `avg/hit ${result.averagePerHit}, action uptime ${result.actionUptime}`
                + (result.kiteShare ? `, kite share ${result.kiteShare}` : ''));
    for (const row of result.breakdown) {
        console.log(`  ${row.action.padEnd(18)} x${String(row.count).padStart(3)} `
                    + `@ ${String(row.damageEach).padStart(6)} = ${String(row.total).padStart(8)} `
                    + `(${(row.share * 100).toFixed(1)}%)`);
    }
}

// Per-hit damage table, phase independent.
console.log('\n=== per-hit damage by move ===');
for (const [difficulty, data] of Object.entries(DIFFICULTY)) {
    const d = data.attack;
    console.log(`${difficulty.padEnd(9)} D=${String(d).padStart(2)}  `
                + `slash ${round2(d)}, vertical ${round2(d * 1.25)}, heavy ${round2(d * 1.75)}, `
                + `chest/halo/throw/dash ${d}, bolt volley ${round2(d * 0.75)}; `
                + `phase one bolts 0 (press to 1 HP)`);
}

// Phase two, encounter-shaped: the boss' own spacing decides which band it fights from. The return
// leg is fixed at 30 ticks (20 outbound cap plus the trip home); the roaming time is the knob, and
// 0/15/30 ticks cover "back in melee the moment it catches" through "drifts back in slowly".
console.log('\n=== phase two, encounter-shaped spacing (owner=1 target glued to the boss) ===');
for (const difficulty of Object.keys(DIFFICULTY)) {
    for (const roaming of [0, 15, 30]) {
        const result = run(`phase two encounter (${difficulty}, roaming ${roaming}t)`,
                           difficulty, 4.0, true, 12000,
                           encounterBandFromFirstThrow(30, roaming));
        console.log(`${difficulty.padEnd(9)} roaming ${String(roaming).padStart(2)}t  `
                    + `=> dps ${result.dps.toFixed(2)} over ${result.seconds}s, `
                    + `hits/min ${round2(result.actions / (result.seconds / 60))}, `
                    + `kite share ${(result.kiteShare * 100).toFixed(0)}%, `
                    + `breakdown ${result.breakdown.map(r => `${r.action} x${r.count}`).join(', ')}`);
    }
}

// Whole-fight output: phase one runs its difficulty allotment, then phase two takes over. Phase two
// is measured over a long window so the throw/kite cycle and the melee cycle are both sampled fully.
console.log('\n=== whole encounter (phase one allotment + phase two) ===');
for (const [difficulty, data] of Object.entries(DIFFICULTY)) {
    const phaseOneTicks = { SIMPLE: 1800, DIFFICULT: 1400, COMPLETE: 1000, EXTREME: 750 }[difficulty];
    const one = run(`phase one (${difficulty})`, difficulty, 7.0, false, phaseOneTicks, null, 20);
    const two = run(`phase two (${difficulty})`, difficulty, 4.0, true, 12000,
                    encounterBandFromFirstThrow(30, 15));
    const total = one.totalDamage + two.totalDamage;
    const seconds = one.seconds + two.seconds;
    console.log(`${difficulty.padEnd(9)} phase one ${one.totalDamage} over ${one.seconds}s `
                + `(${one.dps.toFixed(2)} dps), phase two ${two.totalDamage} over ${two.seconds}s `
                + `(${two.dps.toFixed(2)} dps) => whole encounter `
                + `${round2(total)} over ${round2(seconds)}s = ${round2(total / seconds)} dps`);
}

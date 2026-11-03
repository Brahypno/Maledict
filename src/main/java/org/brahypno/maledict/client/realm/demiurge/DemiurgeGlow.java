package org.brahypno.maledict.client.realm.demiurge;

import org.brahypno.maledict.common.realm.demiurge.DemiurgeAttack;

/** Rain lights the entire carved pattern, with a slower travelling pulse between casts. */
final class DemiurgeGlow {
    static float strength(float awakeningAge, DemiurgeAttack attack, float attackAge) {
        if (awakeningAge < 0) return 0;
        float waking = smooth(awakeningAge / 20) * (1 - smooth((awakeningAge - 100) / 40));
        float idle = .16F + .04F * (float) Math.sin(awakeningAge * .055);
        float casting = attack.blend(attackAge);
        return Math.max(waking, idle + (1 - idle) * casting);
    }

    static float bone(float strength, float time, int bone) {
        return strength * (.8F + .2F * (float) Math.sin(time * .13 - bone * .45));
    }

    private static float smooth(float value) {
        value = Math.max(0, Math.min(1, value));
        return value * value * (3 - 2 * value);
    }

    private DemiurgeGlow() {}
}

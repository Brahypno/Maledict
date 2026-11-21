package org.brahypno.maledict.common.entity.vicissitude;

/**
 * Immutable committed state shared by the three copies. No display values live here.
 *
 * <p>命中节奏刻意不在这里：它是 ChangeLib 绑定上的时间门（{@code Controller#interval} /
 * {@code #hold} / {@code commitDamage} 盖章），属于活状态而不是存档状态。所以读档不会先恢复一段没人看得到
 * 的节奏，而是由绑定在重新接上时自己关一段装载宽限。
 */
record VicissitudeVitality(float current, float maximum, int killAttempts, long lastKillTick) {
    VicissitudeVitality {
        if (!Float.isFinite(current) || !Float.isFinite(maximum) || maximum <= 0.0F
                || current < 0.0F || current > maximum || killAttempts < 0 || killAttempts > 3
                || (killAttempts == 3 && current != 0.0F)) {
            throw new IllegalArgumentException("Invalid Vicissitude vitality");
        }
    }

    static VicissitudeVitality initial(float maximum) {
        return new VicissitudeVitality(maximum, maximum, 0, Long.MIN_VALUE);
    }

    /**
     * 首次捕获的池子大小：难度表是地板，入场时游戏报出的最终 {@code MAX_HEALTH} 可以把它抬高。
     * 削减永远不能赢，所以作者定的难度值不会被压低；两个候选里哪个不可用（非有限、非正）就退回另一个，
     * 而不是把坏值写进记录。
     */
    static float initialMaximum(float difficultyMaximum, float attributeMaximum) {
        boolean difficultyUsable = Float.isFinite(difficultyMaximum) && difficultyMaximum > 0.0F;
        boolean attributeUsable = Float.isFinite(attributeMaximum) && attributeMaximum > 0.0F;
        if (difficultyUsable && attributeUsable) {
            return Math.max(difficultyMaximum, attributeMaximum);
        }
        if (difficultyUsable) {
            return difficultyMaximum;
        }
        return attributeUsable ? attributeMaximum : 0.0F;
    }

    VicissitudeVitality withCurrent(float value) {
        return new VicissitudeVitality(value, maximum, killAttempts, lastKillTick);
    }

    VicissitudeVitality afterCanceledDeath() {
        return new VicissitudeVitality(Math.min(1.0F, maximum), maximum, 0, Long.MIN_VALUE);
    }

    /** 扣血只动血量；这一击的节奏由绑定上的 ChangeLib 时序门在提交处自行记下。 */
    VicissitudeVitality afterDamage(float amount) {
        return new VicissitudeVitality(Math.max(0.0F, current - amount), maximum, killAttempts, lastKillTick);
    }

    VicissitudeVitality killAttempt(long now, int window) {
        if (current <= 0.0F || now == lastKillTick) {
            return this;
        }
        int attempts = lastKillTick == Long.MIN_VALUE || now < lastKillTick
                || now - lastKillTick > Math.max(1, window) ? 1 : killAttempts + 1;
        return new VicissitudeVitality(attempts >= 3 ? 0.0F : current, maximum, attempts, now);
    }
}

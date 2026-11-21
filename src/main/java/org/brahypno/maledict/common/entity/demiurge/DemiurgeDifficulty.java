package org.brahypno.maledict.common.entity.demiurge;

import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.changelib.DamageHelper.DamageOptions;
import org.brahypno.changelib.DamageHelper.DamageProbe;

/**
 * 造物主的血量与出手强度。
 *
 * <p>伤害数字<b>不在这里缩放</b>，这里只按世界难度决定交付层级：简单与普通走 medium 管线
 * （{@code lighterDamageMethod} 当前与 medium 等价），困难走 final 管线，和平难度完全不出手。
 *
 * <p>1.20.1 曾在这里乘配置倍率，再用一个覆写了 {@code DamageSource#scalesWithDifficulty()} 的匿名
 * 子类挡住原版难度曲线（那个方法从 1.20.1 起弃用，{@code DamageSource} 在 1.21 又变成 record，
 * 既不能覆写也不该再读）。现在的口径是伤害类型自己声明缩放：造物主照旧用
 * {@code mob_attack}/{@code generic}/{@code magic}/{@code indirect_magic}，四者都写着
 * {@code scaling: when_caused_by_living_non_player}，{@code IScalingFunction.DEFAULT} 在
 * {@code Player#hurt} 里按 和平 0 / 简单 min(d/2+1) / 普通 d / 困难 1.5d 算一次，配置倍率因此整块删除。
 *
 * <p>与原版一致，这条曲线只作用于<b>玩家</b>：造物主打其他生物时按原数值结算，
 * 而 1.20.1 的配置倍率对一切活体生效 —— 这是本次简化唯一的对外差异。
 */
public final class DemiurgeDifficulty {
    public static float maxHealth(Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL, EASY -> 400;
            case NORMAL -> 600;
            case HARD -> 800;
        };
    }

    public static boolean hurt(LivingEntity victim, DamageSource source, double amount) {
        return hurt(victim, source, amount, victim.level().getDifficulty());
    }

    static boolean hurt(LivingEntity victim, DamageSource source, double amount, Difficulty difficulty) {
        if (amount <= 0)
            return false;
        float value = (float) amount;
        // The on-hit effects merge overlapping body contacts; ChangeLib owns delivery
        // and cooldown handling without a separate i-frame filter here.
        return switch (difficulty) {
            case EASY -> DamageProbe.lighterDamageMethod(victim, source, value, false).totalDealt() > 0;
            case NORMAL -> DamageProbe.mediumDamageMethod(victim, source, value, false).totalDealt() > 0;
            case HARD -> DamageProbe.finalDamageMethod(victim, source, value, DamageOptions.finalNoRemove(), false)
                                     .totalDealt() > 0;
            case PEACEFUL -> false;
        };
    }

    private DemiurgeDifficulty() {}
}

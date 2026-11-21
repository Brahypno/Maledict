package org.brahypno.maledict.common.combat.refulgence;

import com.sammy.malum.registry.common.MalumParticleEffectTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.awt.Color;

/**
 * 反照的表现层：派发 Malum 既有的 {@code HEX_BOLT_IMPACT}（法术弹命中式的爆散），不新增粒子类型
 * 与网络包，客户端也不用改。
 *
 * <p>配色取共晶材料自己的 {@code tints} 里的青（{@code 9ee0e6}）—— 法典里「青紫交错的折辉」的青那一半。
 *
 * <p>Malum 1.8.2 改掉了这条通路：1.20.1 的 {@code ParticleEffectTypeRegistry} 换成了
 * {@code MalumParticleEffectTypes}，{@code ColorEffectData(primary, secondary)} 这种双色数据也不再存在，
 * 效果统一走 {@code createEffect(...).color(...).spawn(level)} 的 builder。所以这里只给主色，
 * 紫灰那一半留给效果自身的配色，不再硬塞第二个颜色。
 */
public final class RefulgenceVisuals {
    private static final Color PRIMARY = new Color(0x9EE0E6);

    public static void release(LivingEntity victim) {
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        MalumParticleEffectTypes.HEX_BOLT_IMPACT.createEffect(victim)
                                                  .color(PRIMARY)
                                                  .spawn(level);
    }

    private RefulgenceVisuals() {
    }
}

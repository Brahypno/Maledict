package org.brahypno.maledict.common.combat.refulgence;

import net.minecraft.world.entity.player.Player;
import org.brahypno.maledict.common.MaledictAbilities;

/**
 * 「这件武器带不带反照」的唯一判定入口，走 {@link MaledictAbilities} 的载体判定。
 *
 * <p>载体可以是 Maledict 物品标签 {@code maledict:refulgence_carriers} 里的任何物品，也可以是装了
 * Tetra 时工具最终攒出来的效果等级（{@code maledict.refulgence > 0}）—— 能力本身不随 Tetra 开关。
 * 不靠物品名、材料 ID，也不看背包里有没有共晶。
 *
 * <p>只听主手：副手不替主手充能，一把武器一份账；换手、丢地上再捡起来都跟着物品走，见
 * {@link RefulgenceStore}。
 */
public final class RefulgenceAbility {

    public static boolean holds(Player player) {
        return MaledictAbilities.hasRefulgence(player.getMainHandItem());
    }

    private RefulgenceAbility() {
    }
}

package org.brahypno.maledict.common.rite;

import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.registry.common.item.MalumItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.brahypno.maledict.common.effect.SacrificeSchedule;
import org.brahypno.maledict.registry.MaledictMobEffects;

/**
 * 牺牲仪式：五枚精魂（自下而上 邪术 / 大地 / 碧水 / 澄空 / 狱火）凑齐时发动一次，<b>仅限灵魂木图腾</b>。
 *
 * <p>它不召唤任何东西，只在图腾四周 {@link #SELECTION_RADIUS} 格以内找主手持提尔锋的玩家，把「献祭」
 * 按在他们身上。已经拿到「智慧的牺牲」的人跳过——那枚成就就是免选凭据，见 {@link WisdomSacrifice}。
 */
public final class SacrificeRite {
    /**
     * 挑人的范围：以图腾底座方块为中心、边长 33 格的立方体。
     */
    public static final int SELECTION_RADIUS = 16;

    /**
     * 五根柱子自下而上的精魂。Malum 的邪术仪式一律「邪术垫底、其余往上」，本仪式照办；列表末尾即
     * Malum 认定的辨识精魂（狱火），仪式图标取的就是它的脉动。
     */
    public static SacrificeRiteType rite() {
        return org.brahypno.maledict.registry.MaledictRites.CORRUPTED_SACRIFICE.get();
    }

    /**
     * 发动一次：每一个够得着、主手握着提尔锋、又还没有成就的人都被按上献祭。
     *
     * <p>已经带着印记的人再被点一次，印记会刷回完整的九天九夜——原版 {@code MobEffectInstance#update}
     * 对同级效果取较长的那个时长，而我们给的永远是最长的 {@link SacrificeSchedule#DURATION_TICKS}。这是
     * 有意的：仪式重新盖一个印记，凭据与 1080 点伤害预算也一并刷新。若其它机制缩短实际时长，
     * 伤害按实际时长分摊，无需等待服务器时间走满九天。
     *
     * <p>{@code addEffect} 返回 false（被别的模组用 {@code MobEffectEvent.Applicable} 挡下、或目标免疫）
     * 时不留凭据：效果根本没加上去，谈不上「带着它熬九天」。
     */
    public static void offer(TotemBaseBlockEntity totem, ServerLevel level) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, reach(totem))) {
            if (!bearsTyrving(player) || WisdomSacrifice.hasEarned(player)){
                continue;
            }
            MobEffectInstance mark = new MobEffectInstance(MaledictMobEffects.SACRIFICE,
                                                           SacrificeSchedule.DURATION_TICKS, 0, false, true, true);
            if (player.addEffect(mark)) {
                WisdomSacrifice.markOffered(player);
            }
        }
    }

    /**
     * 主手拿着提尔锋；副手、背包与饰品栏一概不算。
     */
    static boolean bearsTyrving(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        return !held.isEmpty() && held.is(MalumItems.TYRVING.get());
    }

    private static AABB reach(TotemBaseBlockEntity totem) {
        BlockPos base = totem.getBlockPos();
        return new AABB(base).inflate(SELECTION_RADIUS);
    }

    private SacrificeRite() {
    }
}

package org.brahypno.maledict.common.rite;

import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import com.sammy.malum.registry.common.SpiritRiteRegistry;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import com.sammy.malum.registry.common.item.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.effect.SacrificeSchedule;
import org.brahypno.maledict.registry.MaledictMobEffects;
import org.jetbrains.annotations.Nullable;

/**
 * 牺牲仪式：五枚精魂（自下而上 邪术 / 大地 / 碧水 / 澄空 / 狱火）凑齐时发动一次，<b>只在灵魂木图腾上兑现</b>
 * ——符文木那一半是空效果，见 {@link SacrificeRiteType}。
 *
 * <p>它不召唤任何东西，只在图腾四周 {@link #SELECTION_RADIUS} 格以内找主手持提尔锋的玩家，把「献祭」
 * 按在他们身上。已经拿到「智慧的牺牲」的人跳过——那枚成就就是免选凭据，见 {@link WisdomSacrifice}。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SacrificeRite {
    /**
     * 仪式在 Malum 仪式表里的 identifier，同时也是 lang 与图标回退的键。
     */
    public static final String IDENTIFIER = "sacrifice_rite";

    /**
     * 挑人的范围：以图腾底座方块为中心、边长 33 格的立方体。
     */
    public static final int SELECTION_RADIUS = 16;

    /**
     * 五根柱子自下而上的精魂。Malum 的邪术仪式一律「邪术垫底、其余往上」，本仪式照办；列表末尾即
     * Malum 认定的辨识精魂（狱火），仪式图标取的就是它的脉动。
     */
    private static final MalumSpiritType[] SPIRITS = {
            SpiritTypeRegistry.ELDRITCH_SPIRIT,
            SpiritTypeRegistry.EARTHEN_SPIRIT,
            SpiritTypeRegistry.AQUEOUS_SPIRIT,
            SpiritTypeRegistry.AERIAL_SPIRIT,
            SpiritTypeRegistry.INFERNAL_SPIRIT,
    };

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // 仪式表是纯静态状态，这里只要排在 Malum 自己的类初始化之后即可。
        event.enqueueWork(SacrificeRite::install);
    }

    public static void install() {
        if (SpiritRiteRegistry.getRite(IDENTIFIER) != null){
            return;
        }
        SpiritRiteRegistry.create(new SacrificeRiteType(IDENTIFIER, SPIRITS));
    }

    /**
     * 已安装的牺牲仪式，未安装时为 null；Codex 的配方页要用它。
     */
    @Nullable
    public static SacrificeRiteType rite() {
        return SpiritRiteRegistry.getRite(IDENTIFIER) instanceof SacrificeRiteType rite ? rite : null;
    }

    /**
     * 发动一次：每一个够得着、主手握着提尔锋、又还没有成就的人都被按上献祭。
     *
     * <p>已经带着印记的人再被点一次，印记会刷回完整的九天九夜——原版 {@code MobEffectInstance#update}
     * 对同级效果取较长的那个时长，而我们给的永远是最长的 {@link SacrificeSchedule#DURATION_TICKS}。这是
     * 有意的：印记从「被按上」的那一刻起算九天，仪式不是把进度往后推，而是重新盖一个，凭据也一并刷新。
     *
     * <p>{@code addEffect} 返回 false（被别的模组用 {@code MobEffectEvent.Applicable} 挡下、或目标免疫）
     * 时不留凭据：效果根本没加上去，谈不上「带着它熬九天」。
     */
    static void offer(TotemBaseBlockEntity totem, ServerLevel level) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, reach(totem))) {
            if (!bearsTyrving(player) || WisdomSacrifice.hasEarned(player)){
                continue;
            }
            MobEffectInstance mark = new MobEffectInstance(MaledictMobEffects.SACRIFICE.get(),
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
        return !held.isEmpty() && held.is(ItemRegistry.TYRVING.get());
    }

    private static AABB reach(TotemBaseBlockEntity totem) {
        BlockPos base = totem.getBlockPos();
        return new AABB(base).inflate(SELECTION_RADIUS);
    }

    private SacrificeRite() {
    }
}

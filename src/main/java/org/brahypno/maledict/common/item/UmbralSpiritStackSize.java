package org.brahypno.maledict.common.item;

import com.mojang.logging.LogUtils;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.brahypno.maledict.Maledict;
import org.slf4j.Logger;

/**
 * 把 Malum 的幽影精魂修回 64 堆叠，和另外八种精魂一致。
 *
 * <p>Malum 的九枚精魂碎片在 {@code ItemRegistry} 的静态块里注册：前八枚用 {@code DEFAULT_PROPERTIES()}
 * （即原版默认的 64），只有 {@code umbral_spirit} 用了 {@code HIDDEN_PROPERTIES()} —— 那是给
 * {@code ritual_shard} 和那批虚空符文准备的「黑水晶之前不显示」的那一档，里面写死了
 * {@code stacksTo(1)}。幽影精魂跟着被归进这一档，成了唯一不能叠的精魂。
 *
 * <p>后果不是手感问题，而是死配方：符文工作台的判定是 Lodestone 的
 * {@code IngredientWithCount#matches}，要求 {@code stack.getCount() >= count}；Malum 自己 25 条
 * runeworking 配方全都要 16 或 32 枚精魂，本模组的 {@code rune_of_melancholia} 也照这个模子要
 * 16 枚幽影精魂。上限是 1 时一个格子永远凑不出 16，抑郁符文在生存里合不出来。
 *
 * <p>为什么改的是字段而不是覆写 {@code getMaxStackSize}：{@code ItemStack#getMaxStackSize()} 转手给
 * {@code IForgeItem#getMaxStackSize(ItemStack)}（接口 default 方法，要改别人的物品就只能 mixin 进它自己
 * 的类），而 {@code /give} 和创造模式走的是 {@code Item#getMaxStackSize()} —— 那个方法 final，直读这个
 * 字段。只改前者会让 {@code /give @s malum:umbral_spirit 64} 拆成 64 个单枚堆。字段是唯一一处改完
 * 就能让所有读法一致的地方；AT 也只是把它放开成 public 非 final，不注入任何字节码。
 *
 * <p>不做成配置项：堆叠上限参与客户端预测与槽位校验，而 Forge 不会同步 config，两边一旦不一致
 * 就会出幽灵物品。写死成 64 从构造上不可能不同步。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class UmbralSpiritStackSize {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 另外八种精魂拿到的就是 {@code Item.Properties} 的默认值。 */
    private static final int SPIRIT_STACK_SIZE = 64;

    /**
     * 注册已经全部结束、世界还没建，此刻改是唯一没有历史包袱的时机：还没有任何 ItemStack 存在过，
     * 也就不可能有人拿着旧上限算出来的数目。
     *
     * <p>这个事件的监听器跑在并行工作线程上（{@code ParallelTransition} 用
     * {@code ThreadSelector.PARALLEL} 分发），本阶段的 deferred queue 在阶段末尾由主线程清空，
     * 所以动游戏状态的那一下走 {@code enqueueWork}，和 {@code SummoningRite} 一个写法。
     */
    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(UmbralSpiritStackSize::apply);
    }

    private static void apply() {
        Item umbral = SpiritTypeRegistry.UMBRAL_SPIRIT.spiritShard.get();
        if (umbral.canBeDepleted()) {
            // 有耐久的物品一旦能叠，伤害值就挂在整摞上，附魔台也会拒收（Item#isEnchantable 要求
            // 堆叠上限恰好是 1）。精魂碎片现在没有耐久，哪天有了就别碰它。
            LOGGER.warn("malum:umbral_spirit is damageable; leaving its stack size alone");
            return;
        }
        umbral.maxStackSize = SPIRIT_STACK_SIZE;
    }

    private UmbralSpiritStackSize() {
    }
}

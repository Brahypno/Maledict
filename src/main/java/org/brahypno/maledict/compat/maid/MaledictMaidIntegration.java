package org.brahypno.maledict.compat.maid;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.brahypno.maledict.Maledict;

/**
 * Loads the Touhou Little Maid handlers only when that mod is present.
 *
 * <p>The reference to {@link MaidEvents} sits behind the {@link ModList} guard, so the JVM resolves
 * that class only when the branch is taken. This is still load-bearing on 1.21: the maid mod's
 * events are ordinary NeoForge game-bus events, so linking {@code MaidEvents} would drag its
 * classes (and therefore {@code com.github.tartaricacid.touhoulittlemaid.**}) into a server that
 * does not ship the mod. The method reference (rather than a lambda that names the class) keeps
 * that link purely in an {@code invokedynamic} bootstrap argument, i.e. resolved on first call.
 *
 * <p>1.21 的两处机制差异：{@code FMLCommonSetupEvent} 自己实现 {@code IModBusEvent}，21.1 起
 * {@code @EventBusSubscriber} 的 {@code bus} 属性被忽略、按事件类型自动分线，所以这里不写
 * {@code bus =}；游戏总线也不再是 {@code MinecraftForge.EVENT_BUS}，改由
 * {@link MaidEvents#register()} 自己去注册 {@code NeoForge.EVENT_BUS}。
 */
@EventBusSubscriber(modid = Maledict.MODID)
public final class MaledictMaidIntegration {
    /**
     * 女仆模组的 mod id。
     *
     * <p>这个常量被 {@code MaledictNetwork} 与 {@code MaledictCreativeTabs} 引用，所以它必须一直是
     * 编译期常量（{@code static final String} 字面量）：那样 javac 会把它内联掉，那两个类不会因为
     * 读一个 id 而链接本类。
     */
    public static final String MAID_MOD_ID = "touhou_little_maid";

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded(MAID_MOD_ID))
            return;
        event.enqueueWork(MaidEvents::register);
    }

    private MaledictMaidIntegration() {
    }
}

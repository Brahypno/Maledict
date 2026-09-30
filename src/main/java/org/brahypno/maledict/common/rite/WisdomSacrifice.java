package org.brahypno.maledict.common.rite;

import com.mojang.logging.LogUtils;
import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.effect.SacrificeSchedule;
import org.brahypno.maledict.registry.MaledictMobEffects;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

/**
 * 「智慧的牺牲」：没有图标、不进进度树的那一枚成就，同时兼任两个凭据——仪式不再选中你，以及那两枚
 * 额外栏位。数据在 {@code data/maledict/advancements/wisdom_sacrifice.json}，判据是 {@code impossible}，
 * 只能由这里发。
 *
 * <h2>发放条件：印记得是仪式按的，而且真的自然到期</h2>
 * 只认「{@code maledict:sacrifice} 到期」是不够的：{@code /effect give}、别的模组的随机效果、清洗与
 * 转移效果之类的机制都能凭空塞一枚进来，一 tick 后到期就白送成就与两枚栏位。所以这里要两样东西同时成立：
 *
 * <ol>
 *     <li><b>凭据</b>：仪式真的把印记按上去时（{@link #markOffered}，只在 {@code addEffect} 返回 true 后
 *     调用）在玩家的 ForgeData 里记一个<b>期限</b>——按下那一刻的世界时间 + 九天九夜。</li>
 *     <li><b>自然到期</b>：{@code MobEffectEvent.Expired} 只在 {@code LivingEntity#tickEffects} 里
 *     {@code MobEffectInstance#tick()} 返回 false 的那一步发；死亡重生根本不复制 {@code activeEffects}，
 *     牛奶与 {@code /effect clear} 走的是 {@code Remove}。</li>
 * </ol>
 *
 * <p>于是凭空来的效果到期时没有凭据、有凭据但期限没到（比如被人清掉后又被塞了个短效果）也都不发；
 * 凭据会在三处收走：自然到期发完成就（{@link #onEffectExpired}）、被非自然移除（{@link #onEffectRemoved}）、
 * 以及死亡（{@link #onDeath}）。世界时间只会前进，被延长或重登都不会让期限判错，故正常走完九天九夜
 * 永远不会被这条期限挡下。
 *
 * <p>栏位走 Curios 的永久槽位修饰符：它落在 {@code CurioStacksHandler} 的 persistentModifiers 上，尺寸由
 * {@code baseSize + 修饰符} 重算并存进 NBT，所以死亡重生的 {@code readTag} 与重登都带得回来；固定 UUID
 * 又让重复发放自然覆盖而不是叠加。{@code ISlotHelper#growSlotType} 那条老路已被标了删除，不再用。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WisdomSacrifice {
    /** 成就 id，对应 {@code data/maledict/advancements/wisdom_sacrifice.json}。 */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "wisdom_sacrifice");

    /** 判据名，与成就文件里的 criteria 一致。 */
    private static final String CRITERION = "impossible";

    /** 额外补的符文栏位，Malum 的 {@code rune} 槽。 */
    public static final String RUNE_SLOT = "rune";

    /** 额外补的眼饰栏位，本模组新增的 {@code delusion} 槽。 */
    public static final String DELUSION_SLOT = "delusion";

    /**
     * 「这枚印记是仪式按的」凭据，值是期限（按下那一刻的世界时间 + {@link SacrificeSchedule#DURATION_TICKS}）。
     * 落点是 Forge 的 ForgeData，随玩家 .dat 一起落盘。
     */
    private static final String OFFERED_KEY = "maledict:sacrifice_offered";

    /** 两枚栏位的修饰符 id：固定值，重复发放按 UUID 覆盖，不会越加越多。 */
    private static final UUID RUNE_SLOT_MODIFIER = UUID.fromString("2d6a33ed-1957-4f63-bd82-b853bcf483ad");
    private static final UUID DELUSION_SLOT_MODIFIER = UUID.fromString("51d36a0f-ba8f-49dd-8a66-8a4416e32a5c");

    private static final String MODIFIER_NAME = "maledict:wisdom_sacrifice";

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 仪式把印记按上去之后记凭据；调用方负责只在效果真的加上去之后调用。 */
    public static void markOffered(ServerPlayer player) {
        long deadline = player.level().getGameTime() + SacrificeSchedule.DURATION_TICKS;
        player.getPersistentData().putLong(OFFERED_KEY, deadline);
    }

    /** 自然到期：仪式按过、且期限已到，才算熬完九天九夜。 */
    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isSacrifice(event.getEffectInstance())) {
            return;
        }
        if (!isOfferedPastDeadline(player)) {
            return;
        }
        clearOffering(player);
        award(player);
    }

    /** 不是自然到期，而是被移除了（牛奶、{@code /effect clear}、别的模组清洗）：印记作废。 */
    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isSacrifice(event.getEffectInstance())) {
            return;
        }
        clearOffering(player);
    }

    /** 死了就是没扛过去（重生不会复制 activeEffects，这里把凭据也一并清掉）。 */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearOffering(player);
        }
    }

    /** 拿到成就的人不再被仪式选中；仪式每次发动都要问一遍。 */
    public static boolean hasEarned(ServerPlayer player) {
        Advancement advancement = advancement(player.server);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    /** 发成就与两枚栏位；重复调用只会发一次。 */
    private static void award(ServerPlayer player) {
        if (hasEarned(player)) {
            return;
        }
        Advancement advancement = advancement(player.server);
        if (advancement == null) {
            LOGGER.warn("Wisdom's Sacrifice: advancement {} is missing from the datapack, nothing granted", ID);
            return;
        }
        player.getAdvancements().award(advancement, CRITERION);
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.addPermanentSlotModifier(RUNE_SLOT, RUNE_SLOT_MODIFIER, MODIFIER_NAME, 1.0D,
                                             AttributeModifier.Operation.ADDITION);
            handler.addPermanentSlotModifier(DELUSION_SLOT, DELUSION_SLOT_MODIFIER, MODIFIER_NAME, 1.0D,
                                             AttributeModifier.Operation.ADDITION);
        });
    }

    private static boolean isSacrifice(@Nullable MobEffectInstance instance) {
        return instance != null && instance.getEffect() == MaledictMobEffects.SACRIFICE.get();
    }

    /** 有凭据，且世界时间已经走过期限。 */
    private static boolean isOfferedPastDeadline(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        return data.contains(OFFERED_KEY)
               && player.level().getGameTime() >= data.getLong(OFFERED_KEY);
    }

    private static void clearOffering(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(OFFERED_KEY)) {
            data.remove(OFFERED_KEY);
        }
    }

    @Nullable
    private static Advancement advancement(MinecraftServer server) {
        return server.getAdvancements().getAdvancement(ID);
    }

    private WisdomSacrifice() {
    }
}

package org.brahypno.maledict.common.ritual.wisdom;

import com.mojang.logging.LogUtils;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.ritual.wisdom.SacrificeEffect;
import org.brahypno.maledict.network.MaledictNetwork;
import org.brahypno.maledict.registry.MaledictMobEffects;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 「智慧的牺牲」：有图标与获得提示、不进进度界面的那一枚成就，同时兼任两个凭据——仪式不再选中你，以及那两枚
 * 额外栏位。数据在 {@code data/maledict/advancements/wisdom_sacrifice.json}，判据是 {@code impossible}，
 * 仪式由这里发放成就；额外栏位在获得成就时发放，包括指令授予。
 * 无 display 的独立父进度负责隐藏整个页签；仅靠 hidden 标记无法在完成后继续隐藏。
 *
 * <h2>发放条件：仪式来源、自然到期、存活</h2>
 * 只认「{@code maledict:sacrifice} 到期」是不够的：{@code /effect give}、别的模组的随机效果、清洗与
 * 转移效果之类的机制都能凭空塞一枚进来，所以这里要三项条件同时成立：
 *
 * <ol>
 *     <li><b>凭据</b>：仪式真的把印记按上去时（{@link #markOffered}，只在 {@code addEffect} 返回 true 后
 *     调用）在玩家的 ForgeData 里记下仪式来源，并以实际效果时长开始记录伤害。</li>
 *     <li><b>自然到期</b>：{@code MobEffectEvent.Expired} 只在 {@code LivingEntity#tickEffects} 里
 *     {@code MobEffectInstance#tick()} 返回 false 的那一步发；死亡重生根本不复制 {@code activeEffects}，
 *     牛奶与 {@code /effect clear} 走的是 {@code Remove}。</li>
 *     <li><b>存活</b>：效果自然到期后仍活着就完成，不以受到或累计的伤害数额为门槛。</li>
 * </ol>
 *
 * <p>凭空来的效果没有仪式凭据，不发成就。自然到期、非自然移除和死亡都会清除凭据与伤害记录；
 * 初始时长缩短或倒计时加速只提高固定伤害的出手频率，不再要求服务器时间走满九天九夜。
 *
 * <p>栏位走 Curios 的永久槽位修饰符：它落在 {@code CurioStacksHandler} 的 persistentModifiers 上，尺寸由
 * {@code baseSize + 修饰符} 重算并存进 NBT，所以死亡重生的 {@code readTag} 与重登都带得回来。
 * {@code ISlotHelper#growSlotType} 那条老路已被标了删除，不再用。
 *
 * <h2>栏位为什么只发一次、丢了还能补</h2>
 * 重复发放之所以不叠加，靠的不是「固定 id 覆盖 map」，而是 Curios 的 {@code modifiersByOperation} 是
 * HashMultimap、{@code AttributeModifier} 又是 record：完全相同的 (id, amount, operation) 会被去重。
 * 同一 id 一旦换了数值就不再「相同」，新旧两份会一起计进尺寸，所以这里一律先查
 * {@code getPermanentModifiers()} 再写，绝不无条件重放。登录走的也是同一条 {@link #ensureSlots}：
 * 旧存档、数据包覆盖、以及「发放那一刻没写成」都能在那一次对账里补齐，而对账本身幂等。
 */
@EventBusSubscriber(modid = Maledict.MODID)
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
     * 「这枚印记是仪式按的」凭据，随玩家 .dat 一起落盘；不再存储服务器时间期限。
     */
    private static final String OFFERED_KEY = "maledict:sacrifice_offered";

    /** 两枚栏位的修饰符 id：固定值，重复发放按 UUID 覆盖，不会越加越多。 */
    private static final ResourceLocation RUNE_SLOT_MODIFIER = ResourceLocation.parse("maledict:wisdom_sacrifice/rune_slot_modifier");
    private static final ResourceLocation DELUSION_SLOT_MODIFIER = ResourceLocation.parse("maledict:wisdom_sacrifice/delusion_slot_modifier");

    /** 每枚栏位的增量；同一 id 换数值会被 Curios 计成两份，所以改这里必须同时清掉旧修饰符。 */
    private static final double SLOT_AMOUNT = 1.0D;

    private static final String MODIFIER_NAME = "maledict:wisdom_sacrifice";

    private static final int SIGHT_LOSS_TICKS = 6 * 60 * 20;
    private static final int BLACKOUT_TICKS = 60;

    // 自然到期的成就事件发生在 tickEffects 的迭代中；新效果必须等本 tick 结束再加。
    private static final Set<ServerPlayer> PENDING_SIGHT_LOSS = new HashSet<>();

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 仪式把印记按上去之后记凭据；调用方负责只在效果真的加上去之后调用。 */
    public static void markOffered(ServerPlayer player) {
        MobEffectInstance effect = player.getEffect(MaledictMobEffects.SACRIFICE);
        if (effect == null) {
            return;
        }
        player.getPersistentData().putBoolean(OFFERED_KEY, true);
        SacrificeEffect.start(player, effect.getDuration());
    }

    /** 自然到期：处理最后的出手节点后，有仪式凭据且仍存活就完成。 */
    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (!isSacrifice(event.getEffectInstance())) {
            return;
        }
        boolean survived = SacrificeEffect.finish(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer player) {
            boolean offered = player.getPersistentData().contains(OFFERED_KEY);
            clearOffering(player);
            if (offered && survived) {
                award(player);
            }
        }
        SacrificeEffect.clear(event.getEntity());
    }

    /** 不是自然到期，而是被移除了（牛奶、{@code /effect clear}、别的模组清洗）：印记作废。 */
    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (!isSacrifice(event.getEffectInstance())) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            clearOffering(player);
        }
        SacrificeEffect.clear(event.getEntity());
    }

    /** 死了就是没扛过去（重生不会复制 activeEffects，这里把凭据也一并清掉）。 */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearOffering(player);
        }
        SacrificeEffect.clear(event.getEntity());
    }

    /** 拿到成就的人不再被仪式选中；仪式每次发动都要问一遍。 */
    public static boolean hasEarned(ServerPlayer player) {
        AdvancementHolder advancement = advancement(player.server);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    /** 发成就；栏位由获得成就的事件发放。 */
    private static void award(ServerPlayer player) {
        if (hasEarned(player)) {
            return;
        }
        AdvancementHolder advancement = advancement(player.server);
        if (advancement == null) {
            LOGGER.warn("Wisdom's Sacrifice: advancement {} is missing from the datapack, nothing granted", ID);
            return;
        }
        player.getAdvancements().award(advancement, CRITERION);
    }

    /** 获得「智慧的牺牲」即永久增加两枚栏位，无论是仪式完成还是指令授予。 */
    @SubscribeEvent
    public static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !ID.equals(event.getAdvancement().id())) {
            return;
        }
        ensureSlots(player);
        PENDING_SIGHT_LOSS.add(player);
    }

    /**
     * 登录对账：成就已经在、栏位却缺了，就在这一次登录补回来。
     *
     * <p>能补上的缺口是「发放那一刻没写成」——Curios 的能力来不及挂、槽位被数据包覆盖、
     * 或者旧存档那时候还没有这段代码。已经有的不会重复加，见 {@link #ensureSlots}。
     */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ensureSlots(player);
        }
    }

    /**
     * 幂等地补齐两枚栏位：已有就不动，没有才写。
     *
     * <p>不能无条件重放。Curios 的 {@code modifiersByOperation} 是 HashMultimap，
     * {@code AttributeModifier} 是 record，所以同一组 (id, amount, operation) 重放会被去重；
     * 但同一 id 换数值后两份都留在里面，{@code update()} 会把它们一起加进尺寸。先查后写对两种情况都成立。
     */
    public static void ensureSlots(ServerPlayer player) {
        if (!hasEarned(player)) {
            return;
        }
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            grantSlot(handler, RUNE_SLOT, RUNE_SLOT_MODIFIER);
            grantSlot(handler, DELUSION_SLOT, DELUSION_SLOT_MODIFIER);
        });
    }

    private static void grantSlot(ICuriosItemHandler handler, String slot, ResourceLocation modifierId) {
        ICurioStacksHandler stacks = handler.getStacksHandler(slot).orElse(null);
        if (stacks == null) {
            // Curios 对没有挂到玩家身上的槽位静默跳过；替它把话说出来，否则栏位无声消失。
            LOGGER.error("Wisdom's Sacrifice: slot '{}' is not attached to {}, {} not granted",
                         slot, handler.getWearer(), modifierId);
            return;
        }
        boolean granted = stacks.getPermanentModifiers().stream().anyMatch(modifier -> modifierId.equals(modifier.id()));
        if (!granted) {
            handler.addPermanentSlotModifier(slot, modifierId, SLOT_AMOUNT, AttributeModifier.Operation.ADD_VALUE);
        }
    }

    /** 两种原版效果持续六分钟；前六十 tick 的独立黑幕与它们同时开始。 */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerPlayer player : PENDING_SIGHT_LOSS) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, SIGHT_LOSS_TICKS));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, SIGHT_LOSS_TICKS));
            MaledictNetwork.sendWisdomBlackout(player, BLACKOUT_TICKS);
        }
        PENDING_SIGHT_LOSS.clear();
    }

    private static boolean isSacrifice(@Nullable MobEffectInstance instance) {
        return instance != null && instance.getEffect() == MaledictMobEffects.SACRIFICE;
    }

    private static void clearOffering(ServerPlayer player) {
        player.getPersistentData().remove(OFFERED_KEY);
    }

    @Nullable
    private static AdvancementHolder advancement(MinecraftServer server) {
        return server.getAdvancements().get(ID);
    }

    private WisdomSacrifice() {
    }
}

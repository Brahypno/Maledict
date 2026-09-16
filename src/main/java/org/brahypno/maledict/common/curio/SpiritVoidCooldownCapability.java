package org.brahypno.maledict.common.curio;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.jetbrains.annotations.Nullable;

/**
 * 魂息虚空的内置冷却，挂在**目标实体**身上——与 Malum 监视者项链的
 * {@code MalumLivingEntityDataCapability.watcherNecklaceCooldown} 完全对称。
 *
 * <p>那边是一个 {@code public int} 字段加每 tick 自减，这里照做：字段 + 递减 + 一条
 * 「还有多久」的查询。区别只有位置——监视者项链从 Lodestone 的 capability 上取，本模组
 * 沿用自己已经有的 {@link org.brahypno.maledict.common.entity.VicissitudeVitalityCapability}
 * 那套写法（{@code @AutoRegisterCapability} + {@code ICapabilitySerializable}）。
 *
 * <p><b>目前它是道保险，不是主力。</b>触发判定是「血量从五成之上跌到五成之下」，
 * 同一只怪在一次掉血过程中只会穿越这条线一次，所以冷却本来就很难被撞到；留着它是为了
 * 万一以后改成「目标处于半血即可触发」那种会连续命中的口径时，限流已经就位。
 *
 * <p>不落盘：5 秒的限流跨存档没有意义，重新载入时从 0 开始即可。因此
 * {@link #serializeNBT()} 返回空标签，Forge 也就不会为它写存档。
 */
@AutoRegisterCapability
@Mod.EventBusSubscriber(modid = Maledict.MODID)
public final class SpiritVoidCooldownCapability implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<SpiritVoidCooldownCapability> CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    /** 两次触发之间的最短间隔（tick）。与监视者项链取同一个值。 */
    public static final int COOLDOWN_TICKS = 100;

    private static final ResourceLocation KEY =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "spirit_void_cooldown");

    /** 剩余冷却，每 tick 减一；0 表示可以触发。对应 Malum 的同名字段。 */
    private int cooldown;

    /**
     * 唯一的 LazyOptional 实例。
     *
     * <p>缓存下来而不是每次 {@code LazyOptional.of(...)} 新建：{@code getCapability} 会被问得
     * 非常频繁，每次新建一个 optional 是白白分配。这里没有需要失效的生命周期，所以不必
     * 像 {@code VicissitudeVitalityCapability} 那样在 {@code addListener} 里 invalidate。
     */
    private final LazyOptional<SpiritVoidCooldownCapability> optional = LazyOptional.of(() -> this);

    /** 给所有生物挂上，这样目标被谁打都能查到同一份记录。 */
    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof LivingEntity) {
            event.addCapability(KEY, new SpiritVoidCooldownCapability());
        }
    }

    /** 每 tick 自减，等价于 Malum 那边 {@code if (c.watcherNecklaceCooldown > 0) c.watcherNecklaceCooldown--;}。 */
    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        SpiritVoidCooldownCapability capability = get(event.getEntity());
        if (capability != null && capability.cooldown > 0) {
            capability.cooldown--;
        }
    }

    /** 取实体身上的记录；没有（未挂载、或客户端侧缺同步）时返回 {@code null}。 */
    @Nullable
    public static SpiritVoidCooldownCapability get(LivingEntity entity) {
        return entity.getCapability(CAPABILITY).orElse(null);
    }

    /**
     * 试着占用一次触发机会。
     *
     * @return {@code true} 表示可以触发（并顺手开始冷却）；{@code false} 表示还在冷却里。
     *         实体上没有这份 capability 时也返回 {@code true}：宁可刷一次，也不要因为
     *         挂载时机问题让效果彻底失效。
     */
    public static boolean tryUse(LivingEntity target) {
        SpiritVoidCooldownCapability capability = get(target);
        if (capability == null) {
            return true;
        }
        if (capability.cooldown > 0) {
            return false;
        }
        capability.cooldown = COOLDOWN_TICKS;
        return true;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        return capability == CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    /** 空标签：这份状态不落盘，见类注释。 */
    @Override
    public CompoundTag serializeNBT() {
        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        // 不落盘，所以也没有要读回来的东西。
    }
}

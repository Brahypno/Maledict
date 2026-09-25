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

/** 魂息虚空的内置冷却，挂在目标实体身上做限流；状态不落盘（重新载入时从 0 开始即可）。 */
@AutoRegisterCapability
@Mod.EventBusSubscriber(modid = Maledict.MODID)
public final class SpiritVoidCooldownCapability implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<SpiritVoidCooldownCapability> CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    /** 两次触发之间的最短间隔（tick）。 */
    public static final int COOLDOWN_TICKS = 100;

    private static final ResourceLocation KEY =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "spirit_void_cooldown");

    /** 剩余冷却；0 表示可以触发。 */
    private int cooldown;

    /** 缓存下来复用：{@code getCapability} 会被问得非常频繁。 */
    private final LazyOptional<SpiritVoidCooldownCapability> optional = LazyOptional.of(() -> this);

    /** 挂在所有生物上，这样无论谁打中目标，查到的都是同一份记录。 */
    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof LivingEntity) {
            event.addCapability(KEY, new SpiritVoidCooldownCapability());
        }
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        SpiritVoidCooldownCapability capability = get(event.getEntity());
        if (capability != null && capability.cooldown > 0) {
            capability.cooldown--;
        }
    }

    /** 未挂载（或客户端侧缺同步）时返回 {@code null}。 */
    @Nullable
    public static SpiritVoidCooldownCapability get(LivingEntity entity) {
        return entity.getCapability(CAPABILITY).orElse(null);
    }

    /**
     * 占用一次触发机会。
     *
     * <p>实体上没有这份 capability 时也返回 {@code true}：宁可刷一次，也不要让效果彻底失效。
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
    }
}

package org.brahypno.maledict.common.curio.enlightenment;

import net.minecraft.core.HolderLookup;
import org.brahypno.maledict.registry.MaledictAttachments;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.jetbrains.annotations.Nullable;

/** 魂息虚空的内置冷却，挂在目标实体身上做限流；状态不落盘（重新载入时从 0 开始即可）。 */
@EventBusSubscriber(modid = Maledict.MODID)
public final class SpiritVoidCooldownCapability implements INBTSerializable<CompoundTag> {

    /** 两次触发之间的最短间隔（tick）。 */
    public static final int COOLDOWN_TICKS = 100;

    private static final ResourceLocation KEY =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "spirit_void_cooldown");

    /** 剩余冷却；0 表示可以触发。 */
    private int cooldown;

    /** 缓存下来复用：{@code getCapability} 会被问得非常频繁。 */

    /** 挂在所有生物上，这样无论谁打中目标，查到的都是同一份记录。 */

    @SubscribeEvent
    public static void tick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        SpiritVoidCooldownCapability capability = get(livingEntity);
        if (capability != null && capability.cooldown > 0) {
            capability.cooldown--;
        }
    }

    /** 未挂载（或客户端侧缺同步）时返回 {@code null}。 */
    @Nullable
    public static SpiritVoidCooldownCapability get(LivingEntity entity) {
        return entity instanceof LivingEntity ? entity.getData(MaledictAttachments.SPIRIT_VOID_COOLDOWN) : null;
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


    /** 空标签：这份状态不落盘，见类注释。 */
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
    }
}

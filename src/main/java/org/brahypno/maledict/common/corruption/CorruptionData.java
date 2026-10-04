package org.brahypno.maledict.common.corruption;

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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.jetbrains.annotations.Nullable;

/**
 * 腐败账目挂在**生物**身上，不挂在符文上：符文只是打标记的那一方，账目跟着被打的目标走，
 * 摘符文、符文被没收、换人佩戴都不影响已经攒下的账。
 *
 * <p>挂在所有 {@link LivingEntity} 上（不只是玩家），因为目标可以是任何生物；账目本身**惰性创建** ——
 * 没腐败过的实体不留对象、存的也是一个空 CompoundTag，所以「给每个生物挂一个 capability」的代价
 * 只有一次对象分配。
 */
@AutoRegisterCapability
@Mod.EventBusSubscriber(modid = Maledict.MODID)
public final class CorruptionData implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<CorruptionData> CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    private static final ResourceLocation KEY =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "corruption");

    /** 缓存下来复用：{@code getCapability} 会被问得非常频繁。 */
    private final LazyOptional<CorruptionData> optional = LazyOptional.of(() -> this);

    @Nullable
    private CorruptionState state;

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof LivingEntity) {
            CorruptionData provider = new CorruptionData();
            event.addCapability(KEY, provider);
            event.addListener(provider.optional::invalidate);
        }
    }

    @Nullable
    public static CorruptionData get(Entity entity) {
        return entity.getCapability(CAPABILITY).orElse(null);
    }

    /** 要写账目时用这个：没账目就当场开一个。 */
    public CorruptionState state() {
        if (state == null) {
            state = new CorruptionState();
        }
        return state;
    }

    /** 只想看看有没有账目时用这个：不产生对象，也没有副作用。 */
    @Nullable
    public CorruptionState stateIfPresent() {
        return state;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        return capability == CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return state == null ? new CompoundTag() : state.save();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (!tag.isEmpty()) {
            state = CorruptionState.load(tag);
        }
    }
}

package org.brahypno.maledict.common.entity;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.jetbrains.annotations.Nullable;

/** A read-only capability mirror; it never supplies authority to a joined boss. */
@AutoRegisterCapability
@Mod.EventBusSubscriber(modid = Maledict.MODID)
public final class VicissitudeVitalityCapability implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<VicissitudeVitalityCapability> CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});
    private static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "vicissitude_vitality");
    private final LazyOptional<VicissitudeVitalityCapability> optional = LazyOptional.of(() -> this);
    private final Mirror mirror = new Mirror();

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof VicissitudeBossEntity) {
            VicissitudeVitalityCapability provider = new VicissitudeVitalityCapability();
            event.addCapability(KEY, provider);
            event.addListener(provider.optional::invalidate);
        }
    }

    public float current() {
        return mirror.snapshot().current();
    }

    public float maximum() {
        return mirror.snapshot().maximum();
    }

    public int killAttempts() {
        return mirror.snapshot().killAttempts();
    }

    VicissitudeVitality snapshot() {
        return mirror.snapshot();
    }

    void synchronize(VicissitudeVitality authority) {
        mirror.synchronize(authority);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        return capability == CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return mirror.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        mirror.deserializeNBT(tag);
    }

    /** Serialization policy independent of Forge's transformed capability-token bootstrap. */
    static final class Mirror implements INBTSerializable<CompoundTag> {
        private VicissitudeVitality value = VicissitudeVitality.initial(1.0F);
        private boolean bound;

        VicissitudeVitality snapshot() {
            return value;
        }

        void synchronize(VicissitudeVitality authority) {
            value = authority;
            bound = true;
        }

        @Override
        public CompoundTag serializeNBT() {
            return VicissitudeVitalityLedger.encode(value);
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            if (!bound && tag.contains("Maximum")) {
                try {
                    value = VicissitudeVitalityLedger.decode(tag);
                } catch (IllegalArgumentException ignored) {
                    // Untrusted entity NBT is replaced by the ledger on joining anyway.
                }
            }
        }
    }
}

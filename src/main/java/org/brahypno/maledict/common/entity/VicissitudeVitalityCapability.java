package org.brahypno.maledict.common.entity;

import net.minecraft.core.HolderLookup;
import org.brahypno.maledict.registry.MaledictAttachments;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.jetbrains.annotations.Nullable;

/** A read-only capability mirror; it never supplies authority to a joined boss. */
public final class VicissitudeVitalityCapability implements INBTSerializable<CompoundTag> {
    private static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "vicissitude_vitality");
    private final Mirror mirror = new Mirror();


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
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return mirror.serializeNBT();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        mirror.deserializeNBT(tag);
    }

    /** Serialization policy independent of Forge's transformed capability-token bootstrap. */
    static final class Mirror {
        private VicissitudeVitality value = VicissitudeVitality.initial(1.0F);
        private boolean bound;

        VicissitudeVitality snapshot() {
            return value;
        }

        void synchronize(VicissitudeVitality authority) {
            value = authority;
            bound = true;
        }

        public CompoundTag serializeNBT() {
            return VicissitudeVitalityLedger.encode(value);
        }

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

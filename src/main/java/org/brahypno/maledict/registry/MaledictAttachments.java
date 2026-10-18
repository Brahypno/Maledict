package org.brahypno.maledict.registry;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.corruption.CorruptionData;
import org.brahypno.maledict.common.curio.SpiritVoidCooldownCapability;
import org.brahypno.maledict.common.entity.VicissitudeVitalityCapability;
import org.brahypno.maledict.common.vitals.DelayedVitals;

import java.util.function.Supplier;

public final class MaledictAttachments {
    public static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Maledict.MODID);

    public static final Supplier<AttachmentType<CorruptionData>> CORRUPTION = TYPES.register(
            "corruption", () -> AttachmentType.serializable(CorruptionData::new).build());
    public static final Supplier<AttachmentType<DelayedVitals>> DELAYED_VITALS = TYPES.register(
            "delayed_vitals", () -> AttachmentType.serializable(DelayedVitals::new).build());
    public static final Supplier<AttachmentType<SpiritVoidCooldownCapability>> SPIRIT_VOID_COOLDOWN = TYPES.register(
            "spirit_void_cooldown", () -> AttachmentType.builder(SpiritVoidCooldownCapability::new).build());
    public static final Supplier<AttachmentType<VicissitudeVitalityCapability>> VICISSITUDE_VITALITY = TYPES.register(
            "vicissitude_vitality", () -> AttachmentType.serializable(VicissitudeVitalityCapability::new).build());

    private MaledictAttachments() {}
}

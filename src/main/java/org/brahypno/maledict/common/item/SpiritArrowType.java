package org.brahypno.maledict.common.item;

import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import com.sammy.malum.registry.common.SpiritTypeRegistry;

import java.util.function.Supplier;

public enum SpiritArrowType {
    SACRED(() -> SpiritTypeRegistry.SACRED_SPIRIT),
    WICKED(() -> SpiritTypeRegistry.WICKED_SPIRIT),
    ARCANE(() -> SpiritTypeRegistry.ARCANE_SPIRIT),
    ELDRITCH(() -> SpiritTypeRegistry.ELDRITCH_SPIRIT),
    AERIAL(() -> SpiritTypeRegistry.AERIAL_SPIRIT),
    AQUEOUS(() -> SpiritTypeRegistry.AQUEOUS_SPIRIT),
    EARTHEN(() -> SpiritTypeRegistry.EARTHEN_SPIRIT),
    INFERNAL(() -> SpiritTypeRegistry.INFERNAL_SPIRIT);

    private static final SpiritArrowType[] VALUES = values();
    private final Supplier<MalumSpiritType> spiritType;

    SpiritArrowType(Supplier<MalumSpiritType> spiritType) {
        this.spiritType = spiritType;
    }

    public MalumSpiritType getSpiritType() {
        return spiritType.get();
    }

    public static SpiritArrowType byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : SACRED;
    }
}

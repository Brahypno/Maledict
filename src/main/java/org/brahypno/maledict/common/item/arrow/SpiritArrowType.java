package org.brahypno.maledict.common.item.arrow;

import com.sammy.malum.core.systems.spirit.type.SpiritArcanaType;
import com.sammy.malum.registry.common.magic.MalumSpiritTypes;

import java.util.function.Supplier;

public enum SpiritArrowType {
    SACRED(() -> MalumSpiritTypes.SACRED_SPIRIT),
    WICKED(() -> MalumSpiritTypes.WICKED_SPIRIT),
    ARCANE(() -> MalumSpiritTypes.ARCANE_SPIRIT),
    ELDRITCH(() -> MalumSpiritTypes.ELDRITCH_SPIRIT),
    AERIAL(() -> MalumSpiritTypes.AERIAL_SPIRIT),
    AQUEOUS(() -> MalumSpiritTypes.AQUEOUS_SPIRIT),
    EARTHEN(() -> MalumSpiritTypes.EARTHEN_SPIRIT),
    INFERNAL(() -> MalumSpiritTypes.INFERNAL_SPIRIT);

    private static final SpiritArrowType[] VALUES = values();
    private final Supplier<com.sammy.malum.core.systems.registry.SpiritHolder<SpiritArcanaType>> spiritType;

    SpiritArrowType(Supplier<com.sammy.malum.core.systems.registry.SpiritHolder<SpiritArcanaType>> spiritType) {
        this.spiritType = spiritType;
    }

    public com.sammy.malum.core.systems.registry.SpiritHolder<SpiritArcanaType> getSpiritType() {
        return spiritType.get();
    }

    public static SpiritArrowType byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : SACRED;
    }
}

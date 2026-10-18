package org.brahypno.maledict.registry;

import net.minecraft.core.Holder;
import com.sammy.malum.core.systems.registry.SpiritHolder;
import com.sammy.malum.core.systems.registry.rite.DeferredRiteEntityEffectTypes;
import com.sammy.malum.core.systems.registry.rite.DeferredRiteTypes;
import com.sammy.malum.core.systems.registry.rite.RiteHolder;
import com.sammy.malum.core.systems.spirit.type.SpiritArcanaType;
import com.sammy.malum.registry.common.magic.MalumSpiritTypes;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity.BossDifficulty;
import org.brahypno.maledict.common.rite.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MaledictRites {
    public static final DeferredRiteEntityEffectTypes EFFECTS = DeferredRiteEntityEffectTypes.create(Maledict.MODID);
    public static final DeferredRiteTypes RITES = DeferredRiteTypes.create(Maledict.MODID);
    private static final Map<BossDifficulty, RiteHolder<VicissitudeRiteType>> SUMMONING = new EnumMap<>(BossDifficulty.class);

    private static final com.sammy.malum.core.systems.registry.rite.RiteEffectHolder<OneShotRiteEffect> SACRIFICE_NATURAL =
            EFFECTS.register("sacrifice_rite_natural", () -> new OneShotRiteEffect((totem, level) -> {}));
    private static final com.sammy.malum.core.systems.registry.rite.RiteEffectHolder<OneShotRiteEffect> SACRIFICE_CORRUPTED =
            EFFECTS.register("sacrifice_rite_corrupted", () -> new OneShotRiteEffect(SacrificeRite::offer));
    public static final RiteHolder<SacrificeRiteType> SACRIFICE = RITES.register("sacrifice_rite", () ->
            new SacrificeRiteType(SACRIFICE_NATURAL, false, sacrificeSpirits()));
    public static final RiteHolder<SacrificeRiteType> CORRUPTED_SACRIFICE = RITES.register("corrupted_sacrifice_rite", () ->
            new SacrificeRiteType(SACRIFICE_CORRUPTED, true, sacrificeSpirits()));

    static {
        registerSummoning("vicissitude_rite", BossDifficulty.SIMPLE, 3, 0);
        registerSummoning("greater_vicissitude_rite", BossDifficulty.DIFFICULT, 4, 0);
        registerSummoning("eldritch_vicissitude_rite", BossDifficulty.COMPLETE, 3, 1);
        registerSummoning("greater_eldritch_vicissitude_rite", BossDifficulty.EXTREME, 3, 2);
    }

    private static void registerSummoning(String name, BossDifficulty difficulty, int arcane, int eldritch) {
        var effect = EFFECTS.register(name, () -> new OneShotRiteEffect((totem, level) -> SummoningRite.summon(totem, level, difficulty)));
        List<SpiritHolder<SpiritArcanaType>> spirits = new ArrayList<>();
        for (int i = 0; i < eldritch; i++) spirits.add(MalumSpiritTypes.ELDRITCH_SPIRIT);
        for (int i = 0; i < arcane; i++) spirits.add(MalumSpiritTypes.ARCANE_SPIRIT);
        var natural = RITES.register(name, () -> new VicissitudeRiteType(effect, false, spirits, difficulty));
        RITES.register("corrupted_" + name, () -> new VicissitudeRiteType(effect, true, spirits, difficulty));
        SUMMONING.put(difficulty, natural);
    }

    public static VicissitudeRiteType summoning(BossDifficulty difficulty) {
        return SUMMONING.get(difficulty).get();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static com.sammy.malum.core.systems.registry.rite.RiteHolder<com.sammy.malum.core.systems.rite.SpiritRiteType> holder(
            com.sammy.malum.core.systems.rite.SpiritRiteType rite) {
        for (var entry : RITES.getEntries()) {
            if (entry.value() == rite) return (RiteHolder) entry;
        }
        throw new IllegalArgumentException("Unregistered Maledict rite");
    }

    private static List<SpiritHolder<SpiritArcanaType>> sacrificeSpirits() {
        return List.of(MalumSpiritTypes.ELDRITCH_SPIRIT, MalumSpiritTypes.EARTHEN_SPIRIT,
                MalumSpiritTypes.AQUEOUS_SPIRIT, MalumSpiritTypes.AERIAL_SPIRIT, MalumSpiritTypes.INFERNAL_SPIRIT);
    }

    private MaledictRites() {}
}

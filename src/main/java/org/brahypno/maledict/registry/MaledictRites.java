package org.brahypno.maledict.registry;

import com.sammy.malum.core.systems.registry.SpiritHolder;
import com.sammy.malum.core.systems.registry.rite.DeferredRiteEntityEffectTypes;
import com.sammy.malum.core.systems.registry.rite.DeferredRiteTypes;
import com.sammy.malum.core.systems.registry.rite.RiteHolder;
import com.sammy.malum.core.systems.spirit.type.SpiritArcanaType;
import com.sammy.malum.registry.common.magic.MalumSpiritTypes;
import com.sammy.malum.registry.common.magic.rite.MalumSpiritRiteTypes;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.entity.vicissitude.FirstVicissitudeBossEntity.BossDifficulty;
import org.brahypno.maledict.common.entity.vicissitude.rite.*;
import org.brahypno.maledict.common.ritual.OneShotRiteEffect;
import org.brahypno.maledict.common.ritual.invocation.InvocationLedger;
import org.brahypno.maledict.common.ritual.invocation.InvocationRite;
import org.brahypno.maledict.common.ritual.wisdom.SacrificeRite;
import org.brahypno.maledict.common.ritual.wisdom.SacrificeRiteType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MaledictRites {
    public static final DeferredRiteEntityEffectTypes EFFECTS = DeferredRiteEntityEffectTypes.create(Maledict.MODID);
    // Totem lookup scans this specific DeferredRegister, so rites use its malum namespace.
    // Malum attaches it to its own event bus; do not register it again on ours.
    public static final DeferredRiteTypes RITES = MalumSpiritRiteTypes.RITE_TYPES;
    private static final Map<BossDifficulty, RiteHolder<VicissitudeRiteType>> SUMMONING = new EnumMap<>(BossDifficulty.class);

    private static final com.sammy.malum.core.systems.registry.rite.RiteEffectHolder<OneShotRiteEffect> INVOCATION_EFFECT =
            EFFECTS.register("invocation", () -> new OneShotRiteEffect((totem, level) -> InvocationLedger.get(level).complete(level, totem)));
    public static final RiteHolder<InvocationRite> INVOCATION = RITES.register("maledict_invocation_rite", () ->
            new InvocationRite(INVOCATION_EFFECT, false));
    public static final RiteHolder<InvocationRite> CORRUPTED_INVOCATION = RITES.register("corrupted_maledict_invocation_rite", () ->
            new InvocationRite(INVOCATION_EFFECT, true));

    // 1.20.1 的牺牲仪式是一条注册、内部按木头分派两半；1.8.2 的 matches() 先比 totem.corrupted 再比精魂序列，
    // 所以这里成对注册：自然那条（符文木）挂空效果——柱子照常点亮、随即熄回闲置，但什么都不发生。
    private static final com.sammy.malum.core.systems.registry.rite.RiteEffectHolder<OneShotRiteEffect> SACRIFICE_REFUSED =
            EFFECTS.register("sacrifice_rite_refused", () -> new OneShotRiteEffect((totem, level) -> {
            }));
    private static final com.sammy.malum.core.systems.registry.rite.RiteEffectHolder<OneShotRiteEffect> SACRIFICE_CORRUPTED =
            EFFECTS.register("sacrifice_rite_corrupted", () -> new OneShotRiteEffect(SacrificeRite::offer));
    public static final RiteHolder<SacrificeRiteType> SACRIFICE = RITES.register("sacrifice_rite", () ->
            new SacrificeRiteType(SACRIFICE_REFUSED, false, sacrificeSpirits()));
    public static final RiteHolder<SacrificeRiteType> CORRUPTED_SACRIFICE = RITES.register("corrupted_sacrifice_rite", () ->
            new SacrificeRiteType(SACRIFICE_CORRUPTED, true, sacrificeSpirits()));

    // 拘魂仪式的两个木头变体都不在仪式结束时做事：状态改动发生在激活那一刻（SoulbindingRitual），
    // 所以这里的 effect 只负责让一次性仪式照常收尾。
    private static final com.sammy.malum.core.systems.registry.rite.RiteEffectHolder<OneShotRiteEffect> SOULBINDING_EFFECT =
            EFFECTS.register("soulbinding", () -> new OneShotRiteEffect((totem, level) -> {
            }));
    public static final RiteHolder<org.brahypno.maledict.common.ritual.soulbinding.SoulbindingRite> SOULBINDING =
            RITES.register(org.brahypno.maledict.common.ritual.soulbinding.SoulbindingRite.IDENTIFIER,
                           () -> new org.brahypno.maledict.common.ritual.soulbinding.SoulbindingRite(
                                   SOULBINDING_EFFECT, false));
    public static final RiteHolder<org.brahypno.maledict.common.ritual.soulbinding.SoulbindingRite> CORRUPTED_SOULBINDING =
            RITES.register(org.brahypno.maledict.common.ritual.soulbinding.SoulbindingRite.CORRUPTED_IDENTIFIER,
                           () -> new org.brahypno.maledict.common.ritual.soulbinding.SoulbindingRite(
                                   SOULBINDING_EFFECT, true));

    static {
        registerSummoning("vicissitude_rite", BossDifficulty.SIMPLE, 3, 0);
        registerSummoning("greater_vicissitude_rite", BossDifficulty.DIFFICULT, 4, 0);
        registerSummoning("eldritch_vicissitude_rite", BossDifficulty.COMPLETE, 3, 1);
        registerSummoning("greater_eldritch_vicissitude_rite", BossDifficulty.EXTREME, 3, 2);
    }

    private static void registerSummoning(String name, BossDifficulty difficulty, int arcane, int eldritch) {
        var effect = EFFECTS.register(name, () -> new OneShotRiteEffect((totem, level) -> SummoningRite.summon(totem, level, difficulty)));
        List<SpiritHolder<SpiritArcanaType>> spirits = new ArrayList<>();
        for (int i = 0; i < eldritch; i++)
            spirits.add(MalumSpiritTypes.ELDRITCH_SPIRIT);
        for (int i = 0; i < arcane; i++)
            spirits.add(MalumSpiritTypes.ARCANE_SPIRIT);
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
            if (entry.value() == rite)
                return (RiteHolder) entry;
        }
        throw new IllegalArgumentException("Unregistered Maledict rite");
    }

    private static List<SpiritHolder<SpiritArcanaType>> sacrificeSpirits() {
        return List.of(MalumSpiritTypes.ELDRITCH_SPIRIT, MalumSpiritTypes.EARTHEN_SPIRIT,
                       MalumSpiritTypes.AQUEOUS_SPIRIT, MalumSpiritTypes.AERIAL_SPIRIT, MalumSpiritTypes.INFERNAL_SPIRIT);
    }

    private MaledictRites() {}
}

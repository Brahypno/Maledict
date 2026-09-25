package org.brahypno.maledict.common.rite;

import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.common.spiritrite.TotemicRiteEffect;
import com.sammy.malum.common.spiritrite.TotemicRiteType;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity.BossDifficulty;

/**
 * The encounter's own rites: one spirit recipe per difficulty, none of them used by any other
 * totemic rite. A rite is identified by the spirit list on the totem poles, bottom pole first.
 */
public final class VicissitudeRiteType extends TotemicRiteType {
    private final BossDifficulty difficulty;

    public VicissitudeRiteType(String identifier, BossDifficulty difficulty,
                               MalumSpiritType... spirits) {
        super(identifier, spirits);
        this.difficulty = difficulty;
    }

    public BossDifficulty difficulty() {
        return difficulty;
    }

    /**
     * Built while the base constructor runs, before {@link #difficulty} exists, which is why they
     * hold the rite itself: by the time a rite fires, this object is fully built.
     */
    @Override
    protected TotemicRiteEffect getNaturalRiteEffect() {
        return new SummoningRiteEffect(this);
    }

    @Override
    protected TotemicRiteEffect getCorruptedEffect() {
        return new SummoningRiteEffect(this);
    }

    /**
     * Malum builds this path from the rite's own identifier in its own namespace, which would point
     * at a file that does not exist for a rite it never shipped.
     */
    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath("malum", "textures/vfx/rite/arcane.png");
    }

    private static final class SummoningRiteEffect extends TotemicRiteEffect {
        private final VicissitudeRiteType owner;

        private SummoningRiteEffect(VicissitudeRiteType owner) {
            super(MalumRiteEffectCategory.ONE_TIME_EFFECT);
            this.owner = owner;
        }

        @Override
        protected void doRiteEffect(TotemBaseBlockEntity totem, ServerLevel level) {
            SummoningRite.summon(totem, level, owner.difficulty());
        }
    }
}

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
 * totemic rite.
 *
 * <p>A rite is identified by the spirit list on the totem poles, bottom pole first, so a new rite
 * only has to avoid every existing combination. Malum's table is arcane + one element twice,
 * eldritch + arcane + one element twice, and five arcane; nothing there repeats arcane four times,
 * and nothing at all uses umbral, which is what the two higher recipes are built from.
 *
 * <p>The recipe is the price and the tier: more arcane spirits open the call wider, and umbral at
 * the bottom reaches deeper. Every effect is a {@code ONE_TIME_EFFECT}, so a soulwood totem fires
 * each rite once per activation and can never spawn a crowd.
 */
@SuppressWarnings({"removal"})
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
     * The effects are built while the base constructor runs, before {@link #difficulty} exists,
     * which is why they hold the rite itself instead of a copy of the difficulty: by the time a
     * rite actually fires, this object is fully built.
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
     * Malum builds this path out of the rite's own identifier inside its own namespace, which
     * would point at a file that does not exist for a rite it never shipped. The arcane rite's
     * art is the closest fit for "uncontrolled creation" and costs no new asset.
     */
    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation("malum", "textures/vfx/rite/arcane.png");
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

package org.brahypno.maledict.common.rite;

import net.minecraft.core.Holder;
import com.sammy.malum.core.systems.registry.SpiritHolder;
import com.sammy.malum.core.systems.registry.rite.RiteEffectHolder;
import com.sammy.malum.core.systems.rite.SpiritRiteType;
import com.sammy.malum.core.systems.rite.effect.SpiritRiteEffect;
import com.sammy.malum.core.systems.spirit.type.SpiritArcanaType;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity.BossDifficulty;

public final class VicissitudeRiteType extends SpiritRiteType {
    private final BossDifficulty difficulty;
    public VicissitudeRiteType(RiteEffectHolder<? extends SpiritRiteEffect> effect,
            boolean corrupted, List<SpiritHolder<SpiritArcanaType>> spirits, BossDifficulty difficulty) {
        super(effect, corrupted, List.copyOf(spirits));
        this.difficulty = difficulty;
    }

    public BossDifficulty difficulty() { return difficulty; }
    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath("malum", "textures/vfx/rite/arcane.png");
    }

    @Override
    public String getLangKey() { return "malum.gui.rite." + getName(); }
}

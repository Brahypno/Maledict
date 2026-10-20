package org.brahypno.maledict.common.rite;

import com.sammy.malum.core.systems.registry.SpiritHolder;
import com.sammy.malum.core.systems.registry.rite.RiteEffectHolder;
import com.sammy.malum.core.systems.rite.SpiritRiteType;
import com.sammy.malum.core.systems.rite.effect.SpiritRiteEffect;
import com.sammy.malum.core.systems.spirit.type.SpiritArcanaType;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public final class SacrificeRiteType extends SpiritRiteType {
    public SacrificeRiteType(RiteEffectHolder<? extends SpiritRiteEffect> effect,
            List<SpiritHolder<SpiritArcanaType>> spirits) {
        super(effect, true, List.copyOf(spirits));
    }

    @Override
    public ResourceLocation getIcon() {
        return com.sammy.malum.registry.common.magic.rite.MalumSpiritRiteTypes
                .RITE_OF_THE_BURNING_FERVOR.value().getIcon();
    }

    @Override
    public String getLangKey() { return "malum.gui.rite.corrupted_sacrifice_rite"; }

    @Override
    public String getCodexEntryLangKey() { return "malum.gui.book.entry.page.text.corrupt_sacrifice_rite"; }

    @Override
    public String getEffectLangKey() { return getCodexEntryLangKey() + ".hover"; }
}

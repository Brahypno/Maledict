package org.brahypno.maledict.common.rite;

import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.core.systems.rite.effect.SpiritRiteEffect;
import com.sammy.malum.core.systems.spirit.type.SpiritArcanaType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.function.BiConsumer;

public final class OneShotRiteEffect extends SpiritRiteEffect {
    private final BiConsumer<TotemBaseBlockEntity, ServerLevel> action;

    public OneShotRiteEffect(BiConsumer<TotemBaseBlockEntity, ServerLevel> action) {
        this.action = action;
    }

    @Override
    public boolean triggerRiteEffect(ServerLevel level, BlockPos pos, SpiritArcanaType spirit, RiteParameters parameters) {
        if (!(level.getBlockEntity(pos) instanceof TotemBaseBlockEntity totem)) return false;
        action.accept(totem, level);
        totem.setState(level, TotemBaseBlockEntity.TotemBaseState.INACTIVE);
        return true;
    }
}

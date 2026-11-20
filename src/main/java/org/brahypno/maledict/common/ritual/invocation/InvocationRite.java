package org.brahypno.maledict.common.ritual.invocation;

import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.common.block.curiosities.totem.TotemPoleBlockEntity;
import com.sammy.malum.core.systems.registry.SpiritHolder;
import com.sammy.malum.core.systems.registry.rite.RiteEffectHolder;
import com.sammy.malum.core.systems.rite.SpiritRiteType;
import com.sammy.malum.core.systems.rite.effect.SpiritRiteEffect;
import com.sammy.malum.core.systems.spirit.type.SpiritArcanaType;
import com.sammy.malum.registry.common.magic.MalumSpiritTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.brahypno.maledict.registry.MaledictRites;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.brahypno.maledict.Maledict;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Maledict.MODID)
public final class InvocationRite extends SpiritRiteType {
    public InvocationRite(RiteEffectHolder<? extends SpiritRiteEffect> effect, boolean corrupted) {
        super(effect, corrupted, List.of(MalumSpiritTypes.ARCANE_SPIRIT, MalumSpiritTypes.EARTHEN_SPIRIT,
                MalumSpiritTypes.SACRED_SPIRIT, MalumSpiritTypes.AERIAL_SPIRIT));
    }

    public static InvocationRite get() {
        return MaledictRites.INVOCATION.get();
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            for (var holder : MaledictRites.RITES.getEntries()) {
                SpiritRiteType other = holder.get();
                if (!(other instanceof InvocationRite) && other.getSpirits().equals(get().getSpirits())) {
                    throw new IllegalStateException("Invocation spirit sequence is already used by " + holder.getId());
                }
            }
        });
    }

    public record Structure(TotemBaseBlockEntity base, List<TotemPoleBlockEntity> poles, Direction direction) {
        boolean unchanged(ServerLevel level) {
            Structure current = inspect(level, base);
            return current != null && current.base() == base && current.poles().equals(poles)
                    && current.direction() == direction;
        }
    }

    public static Structure inspect(Level level, TotemBaseBlockEntity base) {
        InvocationRite rite = base.corrupted ? MaledictRites.CORRUPTED_INVOCATION.get() : get();
        if (level.getBlockEntity(base.getBlockPos()) != base) return null;
        List<TotemPoleBlockEntity> poles = new ArrayList<>();
        List<SpiritArcanaType> spirits = new ArrayList<>();
        Direction direction = null;
        BlockPos pos = base.getBlockPos().above();
        while (pos.getY() < level.getMaxBuildHeight()) {
            if (!level.hasChunkAt(pos)) return null;
            if (!(level.getBlockEntity(pos) instanceof TotemPoleBlockEntity pole)) break;
            Direction facing = pole.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (direction == null) direction = facing;
            if (pole.isSoulwood() != base.corrupted || facing != direction || pole.getSpirit() == null) return null;
            poles.add(pole);
            spirits.add(pole.getSpirit());
            if (poles.size() > rite.getSpirits().size()) return null;
            pos = pos.above();
        }
        return matchesSpirits(rite.getSpirits(), spirits) ? new Structure(base, List.copyOf(poles), direction) : null;
    }

    private static boolean matchesSpirits(List<SpiritHolder<SpiritArcanaType>> expected, List<SpiritArcanaType> actual) {
        if (expected.size() != actual.size()) return false;
        for (int i = 0; i < expected.size(); i++) if (!expected.get(i).is(actual.get(i))) return false;
        return true;
    }

    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath("malum", "textures/vfx/rite/aerial.png");
    }

    @Override
    public String getLangKey() { return "malum.gui.rite." + getName(); }

    @Override
    public String getEffectLangKey() {
        return "malum.gui.book.entry.page.text." + (isCorrupted() ? "corrupt_" : "") + "maledict_invocation_rite.hover";
    }
}

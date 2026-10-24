package org.brahypno.maledict.common.invocation;

import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.common.block.curiosities.totem.TotemPoleBlockEntity;
import com.sammy.malum.common.spiritrite.TotemicRiteEffect;
import com.sammy.malum.common.spiritrite.TotemicRiteType;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import com.sammy.malum.registry.common.SpiritRiteRegistry;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.brahypno.maledict.Maledict;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class InvocationRite extends TotemicRiteType {
    public static final String IDENTIFIER = "maledict_invocation_rite";

    private InvocationRite() {
        super(IDENTIFIER, SpiritTypeRegistry.ARCANE_SPIRIT, SpiritTypeRegistry.EARTHEN_SPIRIT,
                SpiritTypeRegistry.SACRED_SPIRIT, SpiritTypeRegistry.AERIAL_SPIRIT);
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            InvocationRite rite = new InvocationRite();
            if (SpiritRiteRegistry.getRite(IDENTIFIER) == null) {
                if (SpiritRiteRegistry.getRite(rite.spirits) != null) {
                    throw new IllegalStateException("Invocation spirit sequence is already used by another rite");
                }
                SpiritRiteRegistry.create(rite);
            }
        });
    }

    public static InvocationRite get() {
        return SpiritRiteRegistry.getRite(IDENTIFIER) instanceof InvocationRite rite ? rite : null;
    }

    public record Structure(TotemBaseBlockEntity base, List<TotemPoleBlockEntity> poles, Direction direction) {
        boolean unchanged(ServerLevel level) {
            Structure current = inspect(level, base);
            return current != null && current.base() == base && current.poles().equals(poles)
                    && current.direction() == direction;
        }
    }

    public static Structure inspect(Level level, TotemBaseBlockEntity base) {
        InvocationRite rite = get();
        if (rite == null || level.getBlockEntity(base.getBlockPos()) != base) return null;
        List<TotemPoleBlockEntity> poles = new ArrayList<>();
        List<MalumSpiritType> spirits = new ArrayList<>();
        Direction direction = null;
        BlockPos pos = base.getBlockPos().above();
        while (pos.getY() < level.getMaxBuildHeight()) {
            if (!level.hasChunkAt(pos)) return null;
            if (!(level.getBlockEntity(pos) instanceof TotemPoleBlockEntity pole)) break;
            Direction facing = pole.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (direction == null) direction = facing;
            if (pole.isSoulwood != base.isSoulwood || facing != direction || pole.type == null) return null;
            poles.add(pole);
            spirits.add(pole.type);
            if (poles.size() > rite.spirits.size()) return null;
            pos = pos.above();
        }
        return rite.spirits.equals(spirits) ? new Structure(base, List.copyOf(poles), direction) : null;
    }

    @Override
    protected TotemicRiteEffect getNaturalRiteEffect() {
        return new Effect();
    }

    @Override
    protected TotemicRiteEffect getCorruptedEffect() {
        return new Effect();
    }

    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath("malum", "textures/vfx/rite/aerial.png");
    }

    private static final class Effect extends TotemicRiteEffect {
        Effect() {
            super(MalumRiteEffectCategory.ONE_TIME_EFFECT);
        }

        @Override
        protected void doRiteEffect(TotemBaseBlockEntity totem, ServerLevel level) {
            InvocationLedger.get(level).complete(level, totem);
        }
    }
}

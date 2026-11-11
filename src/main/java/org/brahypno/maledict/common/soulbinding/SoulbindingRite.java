package org.brahypno.maledict.common.soulbinding;

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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The one Rite of Soulbinding. Malum registers a single rite with a natural (Runewood) and a
 * corrupted (Soulwood) effect, so both halves live here instead of being two separate rites.
 *
 * <p>The rite's actual work is performed by {@link SoulbindingRitual} when the totem base is
 * activated: that is the only place Malum exposes the player who started the rite. By the time a
 * one-time rite effect runs, Malum has already dropped that player context, and the specification
 * requires the checks to happen at activation against a player with a definite identity. The two
 * effects are therefore deliberately empty; the totem still assembles, lights up and resets as a
 * normal one-time rite.
 *
 * <p>Spirit order is bottom-to-top: Eldritch, Wicked, Arcane. It is registered here only if no
 * other rite already owns the sequence; a collision fails loudly instead of silently shadowing
 * another mod's rite.
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SoulbindingRite extends TotemicRiteType {
    public static final String IDENTIFIER = "maledict_soulbinding_rite";

    private static final MalumSpiritType[] SPIRITS = {
            SpiritTypeRegistry.ELDRITCH_SPIRIT,
            SpiritTypeRegistry.WICKED_SPIRIT,
            SpiritTypeRegistry.ARCANE_SPIRIT
    };

    private static final TotemicRiteEffect DORMANT = new DormantEffect();

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(SoulbindingRite::install);
    }

    public static void install() {
        if (SpiritRiteRegistry.getRite(IDENTIFIER) != null) {
            return;
        }
        SoulbindingRite rite = new SoulbindingRite();
        TotemicRiteType conflict = SpiritRiteRegistry.getRite(rite.spirits);
        if (conflict != null) {
            throw new IllegalStateException("Soulbinding spirit sequence " + rite.spirits
                                                    + " is already used by rite " + conflict.identifier);
        }
        SpiritRiteRegistry.create(rite);
    }

    @Nullable
    public static SoulbindingRite get() {
        return SpiritRiteRegistry.getRite(IDENTIFIER) instanceof SoulbindingRite rite ? rite : null;
    }

    /**
     * Whether the poles above this base are exactly the Soulbinding arrangement: same wood and
     * facing as the base, no missing spirits, no extra poles. Reading the blocks directly is
     * necessary before assembly, because the base only learns its pole positions while assembling.
     */
    public static boolean matches(Level level, TotemBaseBlockEntity base) {
        SoulbindingRite rite = get();
        if (rite == null) {
            return false;
        }
        List<MalumSpiritType> spirits = new ArrayList<>();
        Direction direction = null;
        BlockPos pos = base.getBlockPos().above();
        while (pos.getY() < level.getMaxBuildHeight()) {
            if (!level.hasChunkAt(pos)) {
                return false;
            }
            if (!(level.getBlockEntity(pos) instanceof TotemPoleBlockEntity pole)) {
                break;
            }
            Direction facing = pole.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (direction == null) {
                direction = facing;
            }
            if (pole.isSoulwood != base.isSoulwood || facing != direction || pole.type == null) {
                return false;
            }
            spirits.add(pole.type);
            if (spirits.size() > rite.spirits.size()) {
                return false;
            }
            pos = pos.above();
        }
        return rite.spirits.equals(spirits);
    }

    private SoulbindingRite() {
        super(IDENTIFIER, SPIRITS);
    }

    @Override
    protected TotemicRiteEffect getNaturalRiteEffect() {
        return DORMANT;
    }

    @Override
    protected TotemicRiteEffect getCorruptedEffect() {
        return DORMANT;
    }

    @Override
    public ResourceLocation getIcon() {
        // The identifying spirit is the top one (Arcane); Malum's own rite icons are per spirit.
        return ResourceLocation.fromNamespaceAndPath("malum", "textures/vfx/rite/arcane.png");
    }

    private static final class DormantEffect extends TotemicRiteEffect {
        private DormantEffect() {
            super(MalumRiteEffectCategory.ONE_TIME_EFFECT);
        }

        @Override
        protected void doRiteEffect(TotemBaseBlockEntity totem, ServerLevel level) {
            // Soulbinding is applied at activation by SoulbindingRitual; the rite only marks the
            // structure itself and must not repeat the state change here.
        }
    }
}

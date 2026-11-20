package org.brahypno.maledict.common.entity.vicissitude.rite;

import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.common.block.curiosities.totem.TotemPoleBlockEntity;
import com.sammy.malum.core.systems.spirit.type.SpiritArcanaType;
import com.sammy.malum.registry.common.magic.rite.MalumSpiritRiteTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.config.MaledictConfig;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Maledict.MODID)
public final class VicissitudeRiteConfirmationEvents {
    private static final RiteConfirmations CONFIRMATIONS = new RiteConfirmations();
    private static long serverTick;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getMainHandItem().getItem() instanceof org.brahypno.maledict.common.ritual.invocation.InvocationSigilItem) return;
        if (!MaledictConfig.SUMMONING_RITE.get() || player.isSpectator()
                || event.getUseBlock() == TriState.FALSE) return;
        // Match vanilla's crouching bypass of block use, including item sneak-bypass hooks.
        if (event.getUseBlock() != TriState.TRUE && player.isSecondaryUseActive()
                && (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty())
                && !(player.getMainHandItem().doesSneakBypassUse(event.getLevel(), event.getPos(), player)
                && player.getOffhandItem().doesSneakBypassUse(event.getLevel(), event.getPos(), player))) return;

        Level level = event.getLevel();
        if (!(level.getBlockEntity(event.getPos()) instanceof TotemBaseBlockEntity base)) {
            if (event.getHand() == InteractionHand.MAIN_HAND) CONFIRMATIONS.remove(player.getUUID());
            return;
        }
        VicissitudeRiteType rite = findRite(level, base);
        if (rite == null || base.isActiveOrAssembling()) {
            if (event.getHand() == InteractionHand.MAIN_HAND) CONFIRMATIONS.remove(player.getUUID());
            return; // Preserve Malum's invalid, assembling and active interactions.
        }
        // Never let the second hand establish or complete a confirmation, even on a later tick.
        if (event.getHand() == InteractionHand.OFF_HAND) {
            consume(event);
            return;
        }
        if (SummoningRite.hasNearbySummon(player.serverLevel(), event.getPos())) {
            CONFIRMATIONS.remove(player.getUUID());
            consume(event);
            player.displayClientMessage(Component.translatable("message.maledict.vicissitude_rite.occupied")
                    .withStyle(ChatFormatting.DARK_RED), true);
            return;
        }
        if (!SummoningRite.hasSummoningSpace(player.serverLevel(), event.getPos())) {
            CONFIRMATIONS.remove(player.getUUID());
            consume(event);
            player.displayClientMessage(Component.translatable("message.maledict.vicissitude_rite.cramped")
                    .withStyle(ChatFormatting.DARK_RED), true);
            return;
        }
        var target = new RiteConfirmations.Target(level.dimension().location().toString(),
                event.getPos().asLong(), rite.getRegistryName().toString());
        var result = CONFIRMATIONS.click(player.getUUID(), target, serverTick);
        if (result == RiteConfirmations.Result.CONFIRMED) return; // Original block use starts assembly.
        consume(event);
        if (result == RiteConfirmations.Result.WARN) {
            player.displayClientMessage(Component.translatable("message.maledict.vicissitude_rite.warning")
                    .withStyle(ChatFormatting.DARK_RED)
                    .append("\n")
                    .append(Component.translatable("message.maledict.vicissitude_rite.confirm")
                            .withStyle(ChatFormatting.GRAY)), true);
        }
    }

    /** Mirror addTotemPole's wood/facing/spirit checks without starting its animation or mutating it. */
    static VicissitudeRiteType findRite(Level level, TotemBaseBlockEntity base) {
        List<SpiritArcanaType> spirits = new ArrayList<>();
        Direction facing = null;
        for (BlockPos pos = base.getBlockPos().above(); pos.getY() < level.getMaxBuildHeight(); pos = pos.above()) {
            if (!(level.getBlockEntity(pos) instanceof TotemPoleBlockEntity pole)) break;
            Direction next = pole.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (facing == null) facing = next;
            if (pole.isSoulwood() != base.corrupted || next != facing || pole.getSpirit() == null) return null;
            spirits.add(pole.getSpirit());
        }
        for (var holder : MalumSpiritRiteTypes.RITE_TYPES.getEntries()) {
            if (!(holder.get() instanceof VicissitudeRiteType rite)
                    || rite.isCorrupted() != base.corrupted || rite.getSpirits().size() != spirits.size()) continue;
            boolean matches = true;
            for (int i = 0; i < spirits.size(); i++) {
                if (!rite.getSpirits().get(i).is(spirits.get(i))) {
                    matches = false;
                    break;
                }
            }
            if (matches) return rite;
        }
        return null;
    }

    private static void consume(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++serverTick % 20 == 0) CONFIRMATIONS.expire(serverTick);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CONFIRMATIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStarting(ServerAboutToStartEvent event) {
        reset();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        reset();
    }

    private static void reset() {
        CONFIRMATIONS.clear();
        serverTick = 0;
    }

    private VicissitudeRiteConfirmationEvents() {}
}

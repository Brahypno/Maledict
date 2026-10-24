package org.brahypno.maledict.common.invocation;

import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;

@Mod.EventBusSubscriber(modid = Maledict.MODID)
public final class InvocationEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof InvocationSigilItem)
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof TotemBaseBlockEntity base)) return;
        if (event.getUseBlock() == Event.Result.DENY || event.getUseItem() == Event.Result.DENY) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != InteractionHand.MAIN_HAND
                || player.isSpectator() || !player.mayUseItemAt(event.getPos(), event.getFace(), event.getItemStack())) return;
        ServerLevel level = player.serverLevel();
        String error = start(level, player, base);
        if (error != null) player.displayClientMessage(Component.translatable("message.maledict.invocation." + error), true);
    }

    static String start(ServerLevel level, ServerPlayer player, TotemBaseBlockEntity base) {
        ItemStack stack = player.getMainHandItem();
        InvocationDefinition definition = InvocationDefinition.find(level, stack);
        if (definition == null || !InvocationSpawns.supports(definition)) return "invalid";
        if (base.isSoulwood != definition.soulwood()) return definition.soulwood() ? "soulwood" : "runewood";
        InvocationLedger ledger = InvocationLedger.get(level);
        if (base.isActiveOrAssembling() || ledger.contains(base.getBlockPos())) return "busy";
        InvocationRite.Structure structure = InvocationRite.inspect(level, base);
        if (structure == null) return "structure";
        if (definition.soulwood() && level.getDifficulty() == Difficulty.PEACEFUL) return "peaceful";
        Mob probe = InvocationSpawns.create(level, definition);
        if (probe == null) return "invalid";
        if (InvocationSpawns.findSpace(level, base.getBlockPos(), structure.direction(), definition, probe) == null) return "space";
        if (!ledger.begin(level, structure, definition, stack)) return "busy";
        base.onUse(player, InteractionHand.MAIN_HAND);
        if (base.state != TotemBaseBlockEntity.TotemRiteState.ASSEMBLING) {
            ledger.abandonUnpaid(base.getBlockPos());
            return "structure";
        }
        stack.shrink(1);
        player.containerMenu.broadcastChanges();
        return null;
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        // Validate before Malum's block-entity tick can finish an altered spirit sequence.
        if (event.phase == TickEvent.Phase.START && event.level instanceof ServerLevel level) {
            InvocationLedger.get(level).tick(level);
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            InvocationLedger.get(level).interrupt(event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            for (ServerLevel level : event.getPlayerList().getServer().getAllLevels()) {
                InvocationLedger.get(level).interruptAll();
            }
        }
    }

    private InvocationEvents() {
    }
}

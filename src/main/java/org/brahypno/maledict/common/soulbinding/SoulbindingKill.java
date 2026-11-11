package org.brahypno.maledict.common.soulbinding;

import com.sammy.malum.registry.common.DamageTypeRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.MaledictTags;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Binds an armed tool to the first valid kill.
 *
 * <p>{@link LivingDeathEvent} only supplies the dying entity and the damage source, never the
 * killing stack. Two very short-lived records are therefore kept per player:
 *
 * <ul>
 *     <li>an <b>action</b>, recorded on {@link AttackEntityEvent} (the standard
 *     {@code Player#attack} path) or on a {@code malum:scythe_melee} hurt (Malum scythes and the
 *     Incursus Blade's custom radial attack, which never goes through {@code Player#attack});</li>
 *     <li>a <b>target context</b>, recorded on any player-caused hurt in the same tick whose direct
 *     entity is the player (or absent) and whose main-hand stack still matches the action's stack.</li>
 * </ul>
 *
 * <p>Death only binds when the target context still matches the dying entity, dimension and tick,
 * the damage source's causing entity is that player, its direct entity is the player or absent, and
 * the stack is still armed and in {@code #maledict:soul_bindable}. This deliberately accepts any
 * damage type in the same tick as the action — sweep, the arcane/freeze channels, voodoo and
 * modded non-melee channels included — while still rejecting another player's finisher,
 * projectiles, summons, environment damage, a hand swap, and delayed damage on a later tick.
 *
 * <p>Binding is idempotent because it requires an {@code ARMED} record; once {@code BOUND}, a
 * duplicate death event can no longer capture a second spirit.
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID)
public final class SoulbindingKill {
    private record Action(ResourceKey<Level> dimension, long tick, ItemStack stack) {
    }

    private record Attack(ResourceKey<Level> dimension, int targetId, long tick, ItemStack stack) {
    }

    private static final Map<UUID, Action> ACTIONS = new HashMap<>();
    private static final Map<UUID, Attack> ATTACKS = new HashMap<>();

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        recordAction(player);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide
                || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Entity direct = event.getSource().getDirectEntity();
        if (direct != null && direct != player) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !stack.is(MaledictTags.SOUL_BINDABLE) || !SoulbindingData.isArmed(stack)) {
            return;
        }

        // The action is the swing itself. Standard attacks were already recorded by
        // AttackEntityEvent; the scythe's direct channel is the custom path's equivalent.
        if (event.getSource().is(DamageTypeRegistry.SCYTHE_MELEE)) {
            ACTIONS.put(player.getUUID(), new Action(player.level().dimension(),
                    player.level().getGameTime(), stack.copy()));
        }

        Action action = ACTIONS.get(player.getUUID());
        if (action == null
                || action.tick() != player.level().getGameTime()
                || !action.dimension().equals(player.level().dimension())
                || !sameTool(action.stack(), stack)) {
            return;
        }
        ATTACKS.put(player.getUUID(), new Attack(player.level().dimension(), event.getEntity().getId(),
                player.level().getGameTime(), stack.copy()));
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer deadPlayer) {
            clear(deadPlayer.getUUID());
        }
        if (event.getEntity().getType().is(MaledictTags.SOUL_BINDING_BLACKLIST)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Entity direct = event.getSource().getDirectEntity();
        if (direct != null && direct != player) {
            return;
        }
        Attack attack = ATTACKS.get(player.getUUID());
        if (attack == null) {
            return;
        }
        if (!attack.dimension().equals(player.level().dimension())
                || attack.targetId() != event.getEntity().getId()
                || attack.tick() != player.level().getGameTime()) {
            return;
        }
        ATTACKS.remove(player.getUUID());

        ItemStack current = player.getMainHandItem();
        if (!sameTool(attack.stack(), current)) {
            return;
        }
        if (SoulbindingData.bind(current, event.getEntity())) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity().getUUID());
    }

    private static void recordAction(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !stack.is(MaledictTags.SOUL_BINDABLE) || !SoulbindingData.isArmed(stack)) {
            return;
        }
        ACTIONS.put(player.getUUID(), new Action(player.level().dimension(),
                player.level().getGameTime(), stack.copy()));
    }

    /**
     * The same tool, not necessarily byte-identical NBT: a swing's own {@code hurtEnemy} durability
     * damage and the Incursus Blade's stat updates happen between the action and a sweep/magic
     * channel, and a full {@link ItemStack#matches} would reject the very channels this context is
     * meant to allow. The item type and the {@code ARMED} soulbinding record are the stable
     * identity; swapping to a different item or to an unarmed copy still fails.
     */
    private static boolean sameTool(ItemStack previous, ItemStack current) {
        return !previous.isEmpty() && !current.isEmpty()
                && ItemStack.isSameItem(previous, current)
                && previous.is(MaledictTags.SOUL_BINDABLE)
                && current.is(MaledictTags.SOUL_BINDABLE)
                && SoulbindingData.isArmed(previous)
                && SoulbindingData.isArmed(current);
    }

    private static void clear(UUID player) {
        ACTIONS.remove(player);
        ATTACKS.remove(player);
    }

    private SoulbindingKill() {
    }
}

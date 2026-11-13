package org.brahypno.maledict.compat.maid;

import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAndItemTransformEvent;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidDeathEvent;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidFavorabilityLevelChangeEvent;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidTombstoneEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.github.tartaricacid.touhoulittlemaid.item.AbstractStoreMaidItem;
import com.github.tartaricacid.touhoulittlemaid.item.ItemFilm;
import com.github.tartaricacid.touhoulittlemaid.world.data.MaidWorldData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.brahypno.maledict.common.MaledictTags;
import org.brahypno.maledict.common.soulbinding.SoulbindingData;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.network.MaledictNetwork;
import org.brahypno.maledict.registry.MaledictItems;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Touhou Little Maid specific listeners. This class is loaded only through
 * {@link MaledictMaidIntegration}, so the ordinary Maledict jar never links against the optional mod.
 */
public final class MaidEvents {
    private static final String IMPRISONED_TAG = "MaledictImprisonedMaid";
    private static final double COOP_RADIUS = 32.0D;
    private static final Map<UUID, MaidAttack> ATTACKS = new HashMap<>();
    private static final Map<UUID, Long> LAST_COOP = new HashMap<>();
    private static final Map<UUID, Long> LAST_MAID_SOUND = new HashMap<>();

    private record MaidAttack(UUID player, UUID maid, ResourceKey<Level> dimension, long tick,
                              String msgId, ItemStack stack) {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(MaidEvents.class);
    }

    @SubscribeEvent
    public static void onFavorability(MaidFavorabilityLevelChangeEvent event) {
        EntityMaid maid = event.getMaid();
        if (maid.level().isClientSide || event.getNewLevel() <= event.getOldLevel())
            return;
        UUID owner = maid.getOwnerUUID();
        if (owner == null)
            return;
        ServerLevel level = (ServerLevel) maid.level();
        MaledictMaidBondSavedData data = MaledictMaidBondSavedData.get(level);
        if (data.isGiftedPlayer(owner) || data.isGiftedMaid(maid.getUUID()))
            return;

        int from = Math.max(0, event.getOldLevel());
        int to = Math.min(3, event.getNewLevel());
        for (int tier = from + 1; tier <= to; tier++) {
            if (data.hasGiftAttempt(maid.getUUID(), tier))
                continue;
            data.markGiftAttempt(maid.getUUID(), tier);
            if (maid.getRandom().nextFloat() >= chance(tier))
                continue;

            ItemStack sword = MaidBondData.createGiftSword(maid.getUUID(), owner, ownerName(level, owner),
                                                           maid.getName(), tier, maid.getSoundPackId(), maid.getModelId());
            data.markGifted(owner, maid.getUUID());
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
            if (player != null){
                deliver(player, sword);
            }else {
                data.addPendingGift(owner, sword);
            }
            return;
        }
    }

    @Nullable
    private static String ownerName(ServerLevel level, UUID owner) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player != null)
            return player.getGameProfile().getName();
        return level.getServer().getProfileCache().get(owner)
                    .map(profile -> profile.getName()).orElse(null);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide
            || !(event.getSource().getEntity() instanceof ServerPlayer player))
            return;
        Entity direct = event.getSource().getDirectEntity();
        if (direct != null && direct != player)
            return;

        ItemStack stack = player.getMainHandItem();
        if (stack.is(MaledictItems.FORSAKEN_VOW.get())){
            playTransformedSound(player, event.getEntity(), stack);
            return;
        }

        MaidBondData.Gift gift = MaidBondData.read(stack);
        if (gift == null || !MaidBondData.isOriginal(stack)
            || !gift.originalOwner().equals(player.getUUID()))
            return;

        LivingEntity target = event.getEntity();
        if (target instanceof EntityMaid maid && gift.giverMaid().equals(maid.getUUID())
            && player.getUUID().equals(maid.getOwnerUUID())
            && SoulbindingData.state(stack) == SoulbindingData.State.ARMED){
            ATTACKS.put(maid.getUUID(), new MaidAttack(player.getUUID(), maid.getUUID(),
                                                       maid.level().dimension(), maid.level().getGameTime(),
                                                       event.getSource().getMsgId(), stack.copy()));
        }

        if (target == player || player.isAlliedTo(target))
            return;
        if (target instanceof EntityMaid maid && player.getUUID().equals(maid.getOwnerUUID()))
            return;
        long now = player.level().getGameTime();
        if (LAST_COOP.getOrDefault(player.getUUID(), Long.MIN_VALUE) == now)
            return;
        LAST_COOP.put(player.getUUID(), now);
        issueOrder(player, target, gift);
    }

    @SubscribeEvent
    public static void onMaidDeath(MaidDeathEvent event) {
        EntityMaid maid = event.getMaid();
        if (maid.level().isClientSide || maid.getType().is(MaledictTags.SOUL_BINDING_BLACKLIST))
            return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player))
            return;
        Entity direct = event.getSource().getDirectEntity();
        if (direct != null && direct != player)
            return;
        if (!player.getUUID().equals(maid.getOwnerUUID()))
            return;

        MaidAttack attack = ATTACKS.get(maid.getUUID());
        if (!validAttack(attack, player, maid, event.getSource()))
            return;
        ItemStack stack = player.getMainHandItem();
        if (!MaidBondData.sameGift(stack, attack.stack()) || !MaidBondData.isOriginal(stack)
            || SoulbindingData.state(stack) != SoulbindingData.State.ARMED)
            return;

        MaledictMaidBondSavedData data = MaledictMaidBondSavedData.get((ServerLevel) maid.level());
        int refused = data.refusedDeaths(maid.getUUID());
        if (refused < 3){
            data.setRefusedDeaths(maid.getUUID(), refused + 1);
            event.setCanceled(true);
            maid.setHealth(Math.max(1.0F, maid.getMaxHealth() * 0.5F));
            player.displayClientMessage(Component.translatable(
                    "message.maledict.maid_bond.death_" + (refused + 1)), true);
            maid.playSound(InitSounds.MAID_PLAYER.get(), 1.0F, 1.0F);
            ATTACKS.remove(maid.getUUID());
        }
        // Fourth qualified death: the context stays for LivingDeathEvent. That handler performs
        // the sword replacement and the permanent record together, or does neither.
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof EntityMaid maid) || maid.level().isClientSide)
            return;
        MaidAttack attack = ATTACKS.remove(maid.getUUID());
        if (attack == null || event.isCanceled())
            return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player))
            return;
        if (!validAttack(attack, player, maid, event.getSource()))
            return;
        if (!player.getUUID().equals(maid.getOwnerUUID()))
            return;

        ItemStack stack = player.getMainHandItem();
        if (!MaidBondData.sameGift(stack, attack.stack()) || !MaidBondData.isOriginal(stack)
            || SoulbindingData.state(stack) != SoulbindingData.State.ARMED)
            return;

        MaledictMaidBondSavedData data = MaledictMaidBondSavedData.get((ServerLevel) maid.level());
        if (data.refusedDeaths(maid.getUUID()) != 3)
            return;

        ItemStack transformed = MaidBondData.createTransformedSword(stack, maid);
        // Requirement: never create the permanent record without the Forsaken Vow. There is no
        // offline queue or later tick that could desynchronize the two.
        player.setItemInHand(InteractionHand.MAIN_HAND, transformed);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        data.imprison(maid.getUUID());
        strikeAndPunish(player, maid);
    }

    @SubscribeEvent
    public static void onMaidTombstone(MaidTombstoneEvent event) {
        EntityMaid maid = event.getMaid();
        if (maid.level().isClientSide)
            return;
        MaledictMaidBondSavedData data = MaledictMaidBondSavedData.get((ServerLevel) maid.level());
        if (!data.isImprisoned(maid.getUUID()))
            return;

        var items = event.getTombstone().getItems();
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!(stack.getItem() instanceof ItemFilm))
                continue;
            CompoundTag info = AbstractStoreMaidItem.getMaidData(stack);
            if (info.hasUUID("UUID") && info.getUUID("UUID").equals(maid.getUUID())){
                items.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof EntityMaid maid) || maid.level().isClientSide)
            return;
        MaledictMaidBondSavedData data = MaledictMaidBondSavedData.get((ServerLevel) maid.level());
        if (data.isImprisoned(maid.getUUID())
            || maid.getPersistentData().getBoolean(IMPRISONED_TAG)){
            event.setCanceled(true);
            maid.discard();
        }
    }

    @SubscribeEvent
    public static void onTransformToMaid(MaidAndItemTransformEvent.ToMaid event) {
        EntityMaid maid = event.getMaid();
        if (maid.level().isClientSide)
            return;
        CompoundTag data = event.getData();
        if (!data.hasUUID("UUID"))
            return;
        UUID stored = data.getUUID("UUID");
        MaledictMaidBondSavedData saved = MaledictMaidBondSavedData.get((ServerLevel) maid.level());

        // Film-like restore paths call readAdditionalSaveData, which deliberately does not restore
        // UUID. Maids with any crossover record keep their identity so gift/refusal limits survive.
        if (saved.knowsMaid(stored))
            maid.setUUID(stored);
        if (saved.isImprisoned(stored)){
            maid.getPersistentData().putBoolean(IMPRISONED_TAG, true);
            CompoundTag forgeData = data.getCompound("ForgeData");
            forgeData.putBoolean(IMPRISONED_TAG, true);
            data.put("ForgeData", forgeData);
        }
    }

    @SubscribeEvent
    public static void onTransformToItem(MaidAndItemTransformEvent.ToItem event) {
        EntityMaid maid = event.getMaid();
        if (maid.level().isClientSide)
            return;
        MaledictMaidBondSavedData data = MaledictMaidBondSavedData.get((ServerLevel) maid.level());
        if (!data.isImprisoned(maid.getUUID()))
            return;
        CompoundTag maidData = event.getData();
        for (String key : new java.util.ArrayList<>(maidData.getAllKeys()))
            maidData.remove(key);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        MaledictMaidBondSavedData data = MaledictMaidBondSavedData.get((ServerLevel) player.level());
        ItemStack pending = data.takePendingGift(player.getUUID());
        if (pending != null && !pending.isEmpty())
            deliver(player, pending);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID player = event.getEntity().getUUID();
        ATTACKS.entrySet().removeIf(entry -> entry.getValue().player().equals(player));
        LAST_COOP.remove(player);
        LAST_MAID_SOUND.remove(player);
    }

    private static void strikeAndPunish(ServerPlayer player, EntityMaid giver) {
        ServerLevel level = (ServerLevel) player.level();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null){
            bolt.moveTo(player.getX(), player.getY(), player.getZ());
            bolt.setCause(player);
            level.addFreshEntity(bolt);
        }

        MinecraftServer server = player.getServer();
        if (server == null)
            return;
        for (ServerLevel otherLevel : server.getAllLevels()) {
            for (Entity entity : otherLevel.getEntities().getAll()) {
                if (entity instanceof EntityMaid other
                    && !other.getUUID().equals(giver.getUUID())
                    && player.getUUID().equals(other.getOwnerUUID())){
                    other.getFavorabilityManager().reduceWithoutLevel(128);
                }
            }
        }
    }

    private static void playTransformedSound(ServerPlayer player, LivingEntity target, ItemStack stack) {
        long now = player.level().getGameTime();
        Long last = LAST_MAID_SOUND.get(player.getUUID());
        if (last != null && now - last < MaledictConfig.SOULBINDING_SPIRIT_SOUND_COOLDOWN.get())
            return;
        LAST_MAID_SOUND.put(player.getUUID(), now);

        MaidBondData.Gift gift = MaidBondData.read(stack);
        playMaidVoice(player, gift == null ? null : gift.soundPackId(),
                      target.position().add(0, target.getBbHeight() * 0.5, 0));
    }

    private static void playMaidVoice(ServerPlayer player, @Nullable String soundPack, Vec3 pos) {
        if (soundPack != null && !soundPack.isBlank()){
            // One packet, one recipient: the attacking player, never nearby players.
            MaledictNetwork.sendMaidVoice(player, MaidBondData.MAID_HURT_PLAYER_SOUND, soundPack, pos);
        }else {
            // TLM's maid sound events have no vanilla ogg; a normal hurt sound is at least audible.
            player.playNotifySound(SoundEvents.PLAYER_HURT, SoundSource.VOICE, 1.0F, 1.0F);
        }
    }

    private static void issueOrder(ServerPlayer player, LivingEntity target, MaidBondData.Gift gift) {
        if (!isGiverMaid(player, gift.giverMaid()))
            return;
        for (EntityMaid maid : player.level().getEntitiesOfClass(EntityMaid.class,
                                                                 player.getBoundingBox().inflate(COOP_RADIUS),
                                                                 maid -> maid.isAlive() && player.getUUID().equals(maid.getOwnerUUID())
                                                                         && maid.canAttack(target))) {
            maid.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        }
    }

    private static boolean validAttack(
            @Nullable MaidAttack attack, ServerPlayer player,
            EntityMaid maid, DamageSource source) {
        return attack != null
               && attack.player().equals(player.getUUID())
               && attack.tick() == maid.level().getGameTime()
               && attack.dimension().equals(maid.level().dimension())
               && attack.msgId().equals(source.getMsgId());
    }

    private static boolean isGiverMaid(ServerPlayer player, UUID giver) {
        MinecraftServer server = player.getServer();
        if (server == null)
            return false;
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(giver) instanceof EntityMaid maid){
                return player.getUUID().equals(maid.getOwnerUUID());
            }
        }
        MaidWorldData worldData = MaidWorldData.get(player.level());
        if (worldData == null)
            return false;
        var infos = worldData.getInfos(player.getUUID());
        if (infos == null)
            return false;
        for (var info : infos) {
            if (info.getEntityId().equals(giver))
                return true;
        }
        return false;
    }

    private static void deliver(ServerPlayer player, ItemStack stack) {
        if (player.getInventory().add(stack)){
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            return;
        }
        ItemEntity drop = player.drop(stack, false);
        if (drop != null){
            drop.setNoPickUpDelay();
            drop.setExtendedLifetime();
        }
    }

    private static float chance(int tier) {
        return switch (tier) {
            case 1 -> 0.20F;
            case 2 -> 0.50F;
            case 3 -> 0.70F;
            default -> 0.0F;
        };
    }

    private MaidEvents() {
    }
}

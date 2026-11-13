package org.brahypno.maledict.compat.maid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Overworld-authoritative state for the maid crossover. All methods run on the server thread.
 */
public final class MaledictMaidBondSavedData extends SavedData {
    private static final String ID = "maledict_maid_bond";
    private static final String GIFTED_PLAYERS = "GiftedPlayers";
    private static final String GIFTED_MAIDS = "GiftedMaids";
    private static final String GIFT_ATTEMPTS = "GiftAttempts";
    private static final String REFUSED_DEATHS = "RefusedDeaths";
    private static final String IMPRISONED = "ImprisonedMaidUUIDs";
    private static final String PENDING_GIFTS = "PendingGifts";

    private final Set<UUID> giftedPlayers = new HashSet<>();
    private final Set<UUID> giftedMaids = new HashSet<>();
    private final Set<UUID> imprisoned = new HashSet<>();
    private final Map<UUID, Integer> giftAttempts = new HashMap<>();
    private final Map<UUID, Integer> refusedDeaths = new HashMap<>();
    private final Map<UUID, CompoundTag> pendingGifts = new HashMap<>();

    public static MaledictMaidBondSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                MaledictMaidBondSavedData::load, MaledictMaidBondSavedData::new, ID);
    }

    public boolean isGiftedPlayer(UUID player) {
        return giftedPlayers.contains(player);
    }

    public boolean isGiftedMaid(UUID maid) {
        return giftedMaids.contains(maid);
    }

    /**
     * Whether this maid has any persistent crossover record and must keep its original UUID on restore.
     */
    public boolean knowsMaid(UUID maid) {
        return giftedMaids.contains(maid) || imprisoned.contains(maid)
               || giftAttempts.containsKey(maid) || refusedDeaths.containsKey(maid);
    }

    public boolean hasGiftAttempt(UUID maid, int level) {
        int bit = 1 << (level - 1);
        return (giftAttempts.getOrDefault(maid, 0) & bit) != 0;
    }

    public void markGiftAttempt(UUID maid, int level) {
        giftAttempts.merge(maid, 1 << (level - 1), (a, b) -> a | b);
        setDirty();
    }

    public void markGifted(UUID player, UUID maid) {
        giftedPlayers.add(player);
        giftedMaids.add(maid);
        setDirty();
    }

    public int refusedDeaths(UUID maid) {
        return Math.min(3, Math.max(0, refusedDeaths.getOrDefault(maid, 0)));
    }

    public void setRefusedDeaths(UUID maid, int count) {
        refusedDeaths.put(maid, Math.min(3, Math.max(0, count)));
        setDirty();
    }

    public boolean isImprisoned(UUID maid) {
        return imprisoned.contains(maid);
    }

    public void imprison(UUID maid) {
        if (imprisoned.add(maid))
            setDirty();
    }

    public void addPendingGift(UUID player, ItemStack stack) {
        pendingGifts.put(player, stack.save(new CompoundTag()));
        setDirty();
    }

    @Nullable
    public ItemStack takePendingGift(UUID player) {
        CompoundTag tag = pendingGifts.remove(player);
        if (tag == null)
            return null;
        setDirty();
        return ItemStack.of(tag);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put(GIFTED_PLAYERS, uuidSet(giftedPlayers));
        tag.put(GIFTED_MAIDS, uuidSet(giftedMaids));
        tag.put(IMPRISONED, uuidSet(imprisoned));
        CompoundTag attempts = new CompoundTag();
        giftAttempts.forEach((uuid, value) -> attempts.putInt(uuid.toString(), value));
        tag.put(GIFT_ATTEMPTS, attempts);
        CompoundTag refused = new CompoundTag();
        refusedDeaths.forEach((uuid, value) -> refused.putInt(uuid.toString(), value));
        tag.put(REFUSED_DEATHS, refused);
        CompoundTag gifts = new CompoundTag();
        pendingGifts.forEach((uuid, value) -> gifts.put(uuid.toString(), value));
        tag.put(PENDING_GIFTS, gifts);
        return tag;
    }

    private static MaledictMaidBondSavedData load(CompoundTag tag) {
        MaledictMaidBondSavedData data = new MaledictMaidBondSavedData();
        readUuidSet(tag.getCompound(GIFTED_PLAYERS), data.giftedPlayers);
        readUuidSet(tag.getCompound(GIFTED_MAIDS), data.giftedMaids);
        readUuidSet(tag.getCompound(IMPRISONED), data.imprisoned);
        CompoundTag attempts = tag.getCompound(GIFT_ATTEMPTS);
        for (String key : attempts.getAllKeys()) {
            uuid(key).ifPresent(id -> data.giftAttempts.put(id, attempts.getInt(key)));
        }
        CompoundTag refused = tag.getCompound(REFUSED_DEATHS);
        for (String key : refused.getAllKeys()) {
            uuid(key).ifPresent(id -> data.refusedDeaths.put(id, refused.getInt(key)));
        }
        CompoundTag gifts = tag.getCompound(PENDING_GIFTS);
        for (String key : gifts.getAllKeys()) {
            uuid(key).ifPresent(id -> data.pendingGifts.put(id, gifts.getCompound(key)));
        }
        return data;
    }

    private static CompoundTag uuidSet(Set<UUID> values) {
        CompoundTag tag = new CompoundTag();
        values.forEach(uuid -> tag.putBoolean(uuid.toString(), true));
        return tag;
    }

    private static void readUuidSet(CompoundTag tag, Set<UUID> target) {
        for (String key : tag.getAllKeys())
            uuid(key).ifPresent(target::add);
    }

    private static java.util.Optional<UUID> uuid(String value) {
        try {
            return java.util.Optional.of(UUID.fromString(value));
        }
        catch (IllegalArgumentException ignored) {
            return java.util.Optional.empty();
        }
    }
}

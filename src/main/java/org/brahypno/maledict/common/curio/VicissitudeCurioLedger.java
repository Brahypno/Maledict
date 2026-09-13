package org.brahypno.maledict.common.curio;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * World level record of confiscated Curios that still have to return to their owner.
 *
 * <p>Ownership is single: a stack is either held by a live boss for gradual returns or owned by
 * this ledger, never both, and it is only removed once delivery has actually succeeded. That is
 * what makes "full inventory" safe: the record simply stays here and is retried on login,
 * respawn, slot changes and the low frequency online queue.
 */
public final class VicissitudeCurioLedger extends SavedData {
    public static final String FILE_ID = "maledict_vicissitude_curios";

    private final Map<UUID, List<Entry>> pending = new LinkedHashMap<>();
    private final Map<UUID, Long> lastAttempt = new LinkedHashMap<>();

    public record Entry(String slotIdentifier, int slotIndex, ItemStack stack) {
    }

    public static VicissitudeCurioLedger get(@Nullable ServerLevel level) {
        if (level == null || level.getServer() == null) {
            throw new IllegalStateException("Curio ledger requires a running server");
        }
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                VicissitudeCurioLedger::load, VicissitudeCurioLedger::new, FILE_ID);
    }

    public static VicissitudeCurioLedger load(CompoundTag tag) {
        VicissitudeCurioLedger ledger = new VicissitudeCurioLedger();
        for (Tag playerTag : tag.getList("Players", Tag.TAG_COMPOUND)) {
            CompoundTag compound = (CompoundTag) playerTag;
            if (!compound.hasUUID("Player")) {
                continue;
            }
            List<Entry> entries = new ArrayList<>();
            for (Tag itemTag : compound.getList("Items", Tag.TAG_COMPOUND)) {
                CompoundTag item = (CompoundTag) itemTag;
                ItemStack stack = ItemStack.of(item.getCompound("Stack"));
                if (!stack.isEmpty()) {
                    entries.add(new Entry(item.getString("Slot"), item.getInt("Index"), stack));
                }
            }
            if (!entries.isEmpty()) {
                ledger.pending.put(compound.getUUID("Player"), entries);
            }
        }
        return ledger;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, List<Entry>> entry : pending.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            CompoundTag player = new CompoundTag();
            player.putUUID("Player", entry.getKey());
            ListTag items = new ListTag();
            for (Entry curio : entry.getValue()) {
                CompoundTag item = new CompoundTag();
                item.putString("Slot", curio.slotIdentifier());
                item.putInt("Index", curio.slotIndex());
                item.put("Stack", curio.stack().save(new CompoundTag()));
                items.add(item);
            }
            player.put("Items", items);
            players.add(player);
        }
        tag.put("Players", players);
        return tag;
    }

    /** Adds stacks that the boss handed over; callers must stop tracking them afterwards. */
    public void addAll(UUID owner, Collection<Entry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        List<Entry> target = pending.computeIfAbsent(owner, ignored -> new ArrayList<>());
        for (Entry entry : entries) {
            if (!entry.stack().isEmpty()) {
                target.add(new Entry(entry.slotIdentifier(), entry.slotIndex(), entry.stack().copy()));
            }
        }
        setDirty();
    }

    public List<Entry> entries(UUID owner) {
        List<Entry> entries = pending.get(owner);
        return entries == null ? List.of() : List.copyOf(entries);
    }

    public boolean hasPending(UUID owner) {
        List<Entry> entries = pending.get(owner);
        return entries != null && !entries.isEmpty();
    }

    public List<UUID> owners() {
        return new ArrayList<>(pending.keySet());
    }

    /** Replaces the stored balance with what is still undeliverable. */
    public void replace(UUID owner, List<Entry> remaining) {
        if (remaining.isEmpty()) {
            pending.remove(owner);
        } else {
            pending.put(owner, new ArrayList<>(remaining));
        }
        setDirty();
    }

    public long lastAttempt(UUID owner) {
        return lastAttempt.getOrDefault(owner, Long.MIN_VALUE);
    }

    public void markAttempt(UUID owner, long gameTime) {
        lastAttempt.put(owner, gameTime);
    }
}

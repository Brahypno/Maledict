package org.brahypno.maledict.common.curio;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * World level record of confiscated Curios that still have to return to their owner.
 *
 * <p>A stack is either held by a live boss or owned by this ledger, never both, and it only
 * leaves the record once delivery has actually succeeded.
 */
public final class VicissitudeCurioLedger extends SavedData {
    public static final String FILE_ID = "maledict_vicissitude_curios";

    private final Map<UUID, List<Entry>> pending = new LinkedHashMap<>();
    private final Map<UUID, Long> lastAttempt = new LinkedHashMap<>();
    private final Map<UUID, Long> awaitingEquipment = new LinkedHashMap<>();
    private final Set<UUID> delivering = new HashSet<>();

    /** A final return site survives the boss, chunk unloads and server restarts. */
    public record ReturnSite(ResourceLocation dimension, BlockPos position) {
    }

    public record Entry(String slotIdentifier, int slotIndex, ItemStack stack,
                        @Nullable UUID encounter, @Nullable ReturnSite returnSite) {
        public Entry(String slotIdentifier, int slotIndex, ItemStack stack) {
            this(slotIdentifier, slotIndex, stack, null, null);
        }

        public Entry withStack(ItemStack remainder) {
            return new Entry(slotIdentifier, slotIndex, remainder, encounter, returnSite);
        }
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
                    ResourceLocation dimension = item.contains("ReturnDimension", Tag.TAG_STRING)
                            ? ResourceLocation.tryParse(item.getString("ReturnDimension")) : null;
                    ReturnSite site = dimension != null
                            ? new ReturnSite(dimension,
                                    BlockPos.of(item.getLong("ReturnPosition"))) : null;
                    entries.add(new Entry(item.getString("Slot"), item.getInt("Index"), stack,
                            item.hasUUID("Encounter") ? item.getUUID("Encounter") : null, site));
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
                if (curio.encounter() != null) item.putUUID("Encounter", curio.encounter());
                if (curio.returnSite() != null) {
                    item.putString("ReturnDimension", curio.returnSite().dimension().toString());
                    item.putLong("ReturnPosition", curio.returnSite().position().asLong());
                }
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
                target.add(entry.withStack(entry.stack().copy()));
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

    public void replace(UUID owner, List<Entry> remaining) {
        if (remaining.isEmpty()) {
            pending.remove(owner);
            lastAttempt.remove(owner);
            awaitingEquipment.remove(owner);
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

    public void equipmentChanged(UUID owner, long gameTime) {
        awaitingEquipment.put(owner, gameTime);
    }

    public void equipmentUpdated(UUID owner, long gameTime) {
        Long changedAt = awaitingEquipment.get(owner);
        // A Curios callback may restore more equipment within this very tick; it needs a later pass.
        if (changedAt != null && gameTime > changedAt) awaitingEquipment.remove(owner);
    }

    public boolean equipmentHasSettled(UUID owner) {
        return !awaitingEquipment.containsKey(owner);
    }

    public boolean beginDelivery(UUID owner) {
        return delivering.add(owner);
    }

    public void endDelivery(UUID owner) {
        delivering.remove(owner);
    }

    /** Only this encounter's released items become eligible for overflow chests. */
    public void finishEncounter(UUID encounter, ReturnSite site) {
        pending.replaceAll((owner, entries) -> entries.stream().map(entry ->
                encounter.equals(entry.encounter()) && entry.returnSite() == null
                        ? new Entry(entry.slotIdentifier(), entry.slotIndex(), entry.stack(), encounter, site)
                        : entry).collect(Collectors.toCollection(ArrayList::new)));
        setDirty();
    }
}

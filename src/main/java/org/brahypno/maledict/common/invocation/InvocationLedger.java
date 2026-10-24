package org.brahypno.maledict.common.invocation;

import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Per-dimension paid summons and pending physical refunds, saved even when the base is gone. */
public final class InvocationLedger extends SavedData {
    private final Map<BlockPos, Entry> entries = new HashMap<>();

    private static final class Entry {
        final ItemStack paid;
        final InvocationDefinition definition;
        final InvocationRite.Structure structure;
        final long started;
        boolean refund;
        boolean settling;

        Entry(ItemStack paid, InvocationDefinition definition, InvocationRite.Structure structure, long started) {
            this.paid = paid.copyWithCount(1);
            this.definition = definition;
            this.structure = structure;
            this.started = started;
            this.refund = structure == null;
        }
    }

    public static InvocationLedger get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(InvocationLedger::load, InvocationLedger::new,
                "maledict_invocations");
    }

    public boolean contains(BlockPos pos) {
        return entries.containsKey(pos);
    }

    public boolean begin(ServerLevel level, InvocationRite.Structure structure,
                         InvocationDefinition definition, ItemStack paid) {
        BlockPos pos = structure.base().getBlockPos().immutable();
        if (entries.containsKey(pos)) return false;
        entries.put(pos, new Entry(paid, definition, structure, level.getGameTime()));
        setDirty();
        return true;
    }

    /** Only used when block activation failed before payment. */
    public void abandonUnpaid(BlockPos pos) {
        entries.remove(pos);
        setDirty();
    }

    public void complete(ServerLevel level, TotemBaseBlockEntity base) {
        BlockPos pos = base.getBlockPos();
        Entry entry = entries.get(pos);
        if (entry == null || entry.refund || entry.settling) return;
        entry.settling = true;
        boolean success = false;
        try {
            if (entry.structure.base() != base || !entry.structure.unchanged(level)) return;
            Mob mob = InvocationSpawns.create(level, entry.definition);
            if (mob == null) return;
            Vec3 at = InvocationSpawns.findSpace(level, pos, entry.structure.direction(), entry.definition, mob);
            if (at == null || !InvocationSpawns.summon(level, entry.definition, mob)) return;
            success = true;
            entries.remove(pos);
            level.sendParticles(ParticleTypes.SOUL, at.x, at.y + .8D, at.z, 40, .6D, .7D, .6D, .03D);
            level.playSound(null, pos, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.BLOCKS, .7F, 1.2F);
        } finally {
            if (!success) entry.refund = true;
            entry.settling = false;
            setDirty();
        }
    }

    public void tick(ServerLevel level) {
        for (var pair : new ArrayList<>(entries.entrySet())) {
            BlockPos pos = pair.getKey();
            Entry entry = pair.getValue();
            if (entry.settling || entries.get(pos) != entry) continue;
            if (!entry.refund && (!level.hasChunkAt(pos) || !entry.structure.unchanged(level)
                    || entry.structure.base().state != TotemBaseBlockEntity.TotemRiteState.ASSEMBLING
                    || level.getGameTime() - entry.started > 200)) {
                entry.refund = true;
                setDirty();
            }
            if (!entry.refund) {
                if (level.getGameTime() % 5 == 0) {
                    BlockPos center = pos.relative(entry.structure.direction(), 3);
                    level.sendParticles(entry.definition.soulwood() ? ParticleTypes.SOUL : ParticleTypes.ENCHANT,
                            center.getX() + .5D, center.getY() + .6D, center.getZ() + .5D, 6, .6D, .5D, .6D, .01D);
                }
            } else if (level.hasChunkAt(pos)) {
                if (entry.structure != null && level.getBlockEntity(pos) == entry.structure.base()
                        && entry.structure.base().state == TotemBaseBlockEntity.TotemRiteState.ASSEMBLING) {
                    entry.structure.base().setState(TotemBaseBlockEntity.TotemRiteState.IDLE);
                }
                if (level.getGameTime() % 10 == 0) dropRefund(level, pos, entry);
            }
        }
    }

    private void dropRefund(ServerLevel level, BlockPos pos, Entry entry) {
        entry.settling = true;
        try {
            for (int radius = 0; radius <= 2; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                        for (int dy : new int[]{1, 0, 2}) {
                            BlockPos candidate = pos.offset(dx, dy, dz);
                            if (!level.hasChunkAt(candidate) || !level.isInWorldBounds(candidate)
                                    || !level.getWorldBorder().isWithinBounds(candidate)) continue;
                            ItemEntity drop = new ItemEntity(level, candidate.getX() + .5D, candidate.getY() + .25D,
                                    candidate.getZ() + .5D, entry.paid.copy());
                            if (!level.noCollision(drop, drop.getBoundingBox())) continue;
                            drop.setDefaultPickUpDelay();
                            if (level.addFreshEntity(drop)) {
                                entries.remove(pos);
                                setDirty();
                            }
                            return; // A cancelled join stays pending, rather than trying to bypass its listener.
                        }
                    }
                }
            }
        } finally {
            entry.settling = false;
        }
    }

    public void interrupt(ChunkPos chunk) {
        for (var pair : entries.entrySet()) {
            if (new ChunkPos(pair.getKey()).equals(chunk) && !pair.getValue().settling) pair.getValue().refund = true;
        }
        setDirty();
    }

    public void interruptAll() {
        entries.values().stream().filter(entry -> !entry.settling).forEach(entry -> entry.refund = true);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        entries.forEach((pos, entry) -> {
            CompoundTag value = new CompoundTag();
            value.putLong("Base", pos.asLong());
            value.put("Paid", entry.paid.save(new CompoundTag()));
            list.add(value);
        });
        tag.put("Pending", list);
        return tag;
    }

    public static InvocationLedger load(CompoundTag tag) {
        InvocationLedger ledger = new InvocationLedger();
        for (Tag element : tag.getList("Pending", Tag.TAG_COMPOUND)) {
            CompoundTag value = (CompoundTag) element;
            ItemStack paid = ItemStack.of(value.getCompound("Paid"));
            if (!paid.isEmpty()) ledger.entries.put(BlockPos.of(value.getLong("Base")), new Entry(paid, null, null, 0));
        }
        return ledger; // Restarted summons become refunds; they never resume a partially assembled rite.
    }
}

package org.brahypno.maledict.common.rite;

import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.registry.common.SoundRegistry;
import com.sammy.malum.registry.common.block.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntPredicate;

/** Snapshot the participating totem before Malum clears its one-time rite's pole list. */
public final class VicissitudeTotemConsumption {
    private record Layer(BlockPos position, BlockState state) {}

    private final List<Layer> layers;
    private final List<Layer> upperLogs;
    private int consumed;

    private VicissitudeTotemConsumption(List<Layer> layers, List<Layer> upperLogs, int consumed) {
        this.layers = layers;
        this.upperLogs = upperLogs;
        this.consumed = consumed;
    }

    public static VicissitudeTotemConsumption capture(TotemBaseBlockEntity totem) {
        List<Layer> layers = new ArrayList<>();
        for (var pole : totem.getTotemPoles()) {
            layers.add(new Layer(pole.getBlockPos().immutable(), pole.getBlockState()));
        }
        layers.sort(Comparator.comparingInt((Layer layer) -> layer.position().getY()).reversed());
        layers.add(new Layer(totem.getBlockPos().immutable(), totem.getBlockState()));
        BlockPos top = layers.get(0).position();
        var level = totem.getLevel();
        List<Layer> upperLogs = new ArrayList<>();
        for (int y : findUpperLogs(top.getY(), level.getMaxBuildHeight(),
                height -> level.getBlockState(top.above(height - top.getY())).isAir(),
                height -> isRitualLog(level.getBlockState(top.above(height - top.getY()))))) {
            BlockPos position = top.above(y - top.getY());
            upperLogs.add(new Layer(position, level.getBlockState(position)));
        }
        upperLogs.sort(Comparator.comparingInt((Layer layer) -> layer.position().getY()).reversed());
        return new VicissitudeTotemConsumption(layers, upperLogs, 0);
    }

    /** Search the column up to its first air block; only the two requested log types are selected. */
    static List<Integer> findUpperLogs(int topY, int maxBuildHeight,
                                       IntPredicate isAir, IntPredicate isLog) {
        List<Integer> heights = new ArrayList<>();
        for (int y = topY + 1; y < maxBuildHeight; y++) {
            if (isAir.test(y)) break;
            if (isLog.test(y)) heights.add(y);
        }
        return heights;
    }

    private static boolean isRitualLog(BlockState state) {
        return state.is(BlockRegistry.SOULWOOD_LOG.get()) || state.is(BlockRegistry.RUNEWOOD_LOG.get());
    }

    public void tick(ServerLevel level, float ticks, BiConsumer<Integer, Vec3> effect) {
        int target = VicissitudeSummoning.totemLayersConsumed(ticks, layers.size());
        while (consumed < target) {
            int index = consumed++;
            if (index == 0) {
                for (int i = 0; i < upperLogs.size(); i++) {
                    // Extra logs share the top pole's tick, not its network deduplication key.
                    consume(level, upperLogs.get(i), layers.size() + i, 1.2F, effect);
                }
            }
            consume(level, layers.get(index), index, 1.2F - index * 0.1F, effect);
        }
    }

    private static void consume(ServerLevel level, Layer layer, int sequence, float pitch,
                                BiConsumer<Integer, Vec3> effect) {
        // Skip positions whose blocks were changed during the entrance.
        if (!level.getBlockState(layer.position()).equals(layer.state())) return;
        // Set air directly: no loot and no player-break callback cancelling the whole totem.
        if (!level.setBlock(layer.position(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) return;
        level.playSound(null, layer.position(), SoundRegistry.SOUL_SHATTER.get(),
                SoundSource.BLOCKS, 0.65F, pitch);
        effect.accept(sequence, Vec3.atCenterOf(layer.position()));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Consumed", consumed);
        tag.put("Layers", saveLayers(layers));
        tag.put("UpperLogs", saveLayers(upperLogs));
        return tag;
    }

    private static ListTag saveLayers(List<Layer> layers) {
        ListTag savedLayers = new ListTag();
        for (Layer layer : layers) {
            CompoundTag saved = new CompoundTag();
            saved.put("Position", NbtUtils.writeBlockPos(layer.position()));
            saved.put("State", NbtUtils.writeBlockState(layer.state()));
            savedLayers.add(saved);
        }
        return savedLayers;
    }

    public static VicissitudeTotemConsumption load(ServerLevel level, CompoundTag tag) {
        return new VicissitudeTotemConsumption(loadLayers(level, tag.getList("Layers", Tag.TAG_COMPOUND)),
                loadLayers(level, tag.getList("UpperLogs", Tag.TAG_COMPOUND)), tag.getInt("Consumed"));
    }

    private static List<Layer> loadLayers(ServerLevel level, ListTag savedLayers) {
        List<Layer> layers = new ArrayList<>();
        for (int i = 0; i < savedLayers.size(); i++) {
            CompoundTag saved = savedLayers.getCompound(i);
            layers.add(new Layer(NbtUtils.readBlockPos(saved.getCompound("Position")),
                    NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK),
                            saved.getCompound("State"))));
        }
        return layers;
    }
}

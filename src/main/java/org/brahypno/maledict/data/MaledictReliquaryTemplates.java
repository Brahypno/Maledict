package org.brahypno.maledict.data;

import com.google.common.hash.Hashing;
import com.sammy.malum.registry.common.block.BlockRegistry;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import org.brahypno.maledict.registry.MaledictBlocks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Authored geometry lives here. The compressed vanilla templates are exclusively runData output. */
public final class MaledictReliquaryTemplates implements DataProvider {
    private final Path directory;

    public MaledictReliquaryTemplates(PackOutput output) {
        directory = output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve("maledict/structures/inverted_reliquary");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return CompletableFuture.runAsync(() -> {
            for (String variant : new String[]{"intact", "fractured", "sunken"}) {
                try {
                    var bytes = new ByteArrayOutputStream();
                    NbtIo.writeCompressed(template(variant), bytes);
                    byte[] data = bytes.toByteArray();
                    output.writeIfNeeded(directory.resolve(variant + ".nbt"), data, Hashing.sha1().hashBytes(data));
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            }
        });
    }

    public static CompoundTag template(String variant) {
        Builder b = new Builder();
        boolean fractured = variant.equals("fractured");
        boolean sunken = variant.equals("sunken");
        BlockState rock = BlockRegistry.TWISTED_ROCK.get().defaultBlockState();
        BlockState cryingTainted = MaledictBlocks.CRYING_TAINTED_ROCK.get().defaultBlockState();
        BlockState cryingTwisted = MaledictBlocks.CRYING_TWISTED_ROCK.get().defaultBlockState();
        BlockState bricks = BlockRegistry.TAINTED_ROCK_BRICKS.get().defaultBlockState();
        BlockState trim = BlockRegistry.POLISHED_TAINTED_ROCK.get().defaultBlockState();
        BlockState tiles = BlockRegistry.TAINTED_ROCK_TILES.get().defaultBlockState();
        BlockState runes = BlockRegistry.RUNIC_TAINTED_ROCK_BRICKS.get().defaultBlockState();

        // A clipped footprint and a vaulted air volume leave the outside rock/caves untouched.
        for (int x = 0; x < 35; x++) {
            for (int z = 0; z < 35; z++) {
                int dx = Math.abs(x - 17), dz = Math.abs(z - 17);
                if (dx + dz > 29) continue;
                int roof = 19 - Math.max(0, (dx - 10) / 2) - Math.max(0, (dz - 12) / 2);
                int top = Math.max(dx, dz) <= 10 ? 2 : 3;
                for (int y = 0; y <= roof; y++) {
                    BlockState state;
                    if (y < top) state = rock;
                    else if (y == top) {
                        int wear = Math.floorMod(x * 43 + z * 71, 23);
                        state = wear < 4 ? rock : wear == 4 ? Blocks.CRACKED_NETHER_BRICKS.defaultBlockState() : tiles;
                        if (dx == 10 || dz == 10) state = trim;
                    } else if (y == roof && ((x * 7 + z * 11) % 9 != 0)) {
                        boolean seep = Math.abs(x - 9) + Math.abs(z - 12) <= 3
                                || Math.abs(x - 26) + Math.abs(z - 23) <= 2;
                        state = seep ? cryingTwisted : rock;
                    }
                    else state = Blocks.AIR.defaultBlockState();
                    b.put(x, y, z, state);
                }
                // Fragmentary double-thick walls. Southern and western gaps expose the hall to caves.
                boolean wall = (dx >= 15 || dz >= 15) && dx + dz <= 28;
                boolean doorway = (z >= 31 && dx <= 4) || (x <= 3 && Math.abs(z - 12) <= 4);
                if (wall && !doorway) {
                    int wallHeight = 5 + Math.floorMod(x * 13 + z * 19, 8);
                    if (fractured && x < 8 && z < 22) wallHeight = 4;
                    b.box(x, 4, z, x, wallHeight, z, bricks);
                    if (Math.floorMod(x * 3 + z, 11) == 0) {
                        b.box(x, Math.max(4, wallHeight - 3), z, x, wallHeight, z, cryingTainted);
                    }
                    if (Math.floorMod(x + z, 7) == 0) b.put(x, 5, z, Blocks.BONE_BLOCK.defaultBlockState());
                }
            }
        }

        // Eight columns surround, rather than obstruct, the open 21 x 21 fighting floor.
        int index = 0;
        for (int x : new int[]{5, 29}) {
            for (int z : new int[]{6, 13, 21, 28}) {
                int height = switch (index++) {
                    case 0, 5 -> 17;
                    case 2, 7 -> fractured ? 6 : 17;
                    case 1, 4 -> 10;
                    default -> 7;
                };
                b.box(x - 1, 4, z - 1, x + 1, 4, z + 1, trim);
                b.box(x, 5, z, x, height, z, BlockRegistry.TAINTED_ROCK_COLUMN.get().defaultBlockState());
                if (height == 17) b.box(x - 1, 17, z - 1, x + 1, 17, z + 1, trim);
                else {
                    b.put(x, height, z, rock);
                    b.put(x, height - 1, z, cryingTainted);
                }
                b.put(x, 5, z - 1, Blocks.CHISELED_NETHER_BRICKS.defaultBlockState());
            }
        }

        // Surviving overhead beams carry the inverted coffin at both ends.
        b.box(5, 18, 9, 29, 18, 10, bricks);
        b.box(5, 18, 24, 29, 18, 25, bricks);
        b.box(16, 17, 9, 18, 18, 12, trim);
        b.box(16, 17, 22, 18, 18, 25, trim);
        // Top-wide stone casing; narrow descending lid, framed with restrained dark red masonry.
        b.box(13, 16, 11, 21, 17, 23, bricks);
        b.box(14, 15, 12, 20, 15, 22, trim);
        b.box(15, 14, 13, 19, 14, 21, Blocks.NETHER_BRICKS.defaultBlockState());
        b.box(16, 13, 14, 18, 13, 20, runes);
        for (int z : new int[]{14, 17, 20}) {
            b.put(17, 13, z, Blocks.END_STONE_BRICKS.defaultBlockState());
            b.put(14, 16, z, Blocks.BONE_BLOCK.defaultBlockState());
            b.put(20, 16, z, Blocks.BONE_BLOCK.defaultBlockState());
        }
        b.put(16, 13, 17, Blocks.CRYING_OBSIDIAN.defaultBlockState());
        b.put(18, 13, 17, Blocks.CRYING_OBSIDIAN.defaultBlockState());
        b.box(16, 15, 14, 18, 15, 20, Blocks.PURPUR_BLOCK.defaultBlockState());
        for (int z : new int[]{15, 19}) {
            b.box(13, 16, z, 13, 17, z, cryingTainted);
            b.box(21, 16, z, 21, 17, z, cryingTainted);
        }
        b.put(15, 14, 13, cryingTainted);
        b.put(19, 14, 21, cryingTainted);

        // Low platform with a raised, visibly distinct one-use altar at its center.
        b.box(14, 3, 12, 20, 3, 22, trim);
        b.box(15, 3, 13, 19, 3, 21, runes);
        b.box(17, 3, 13, 17, 3, 21, Blocks.END_STONE_BRICKS.defaultBlockState());
        b.put(17, 4, 17, MaledictBlocks.RELIQUARY_ALTAR.get().defaultBlockState());

        // Broken columns and bone-bearing rubble stay outside the central arena.
        BlockState horizontalBone = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        b.box(7, 4, 24, 8, 5, 29, rock);
        b.box(8, 5, 25, 8, 5, 28, horizontalBone);
        b.box(26, 4, 7, 28, 5, 10, bricks);
        b.put(27, 6, 8, Blocks.BONE_BLOCK.defaultBlockState());
        for (BlockPos pos : new BlockPos[]{new BlockPos(6, 4, 16), new BlockPos(28, 4, 23), new BlockPos(11, 4, 29)}) {
            b.put(pos.getX(), pos.getY(), pos.getZ(), Blocks.SOUL_SOIL.defaultBlockState());
            b.put(pos.getX(), pos.getY() + 1, pos.getZ(), MaledictBlocks.WITHERLIGHT_MUSHROOM.get().defaultBlockState());
        }
        if (fractured) {
            b.box(13, 14, 11, 16, 17, 13, Blocks.AIR.defaultBlockState());
            b.box(14, 15, 14, 15, 16, 15, Blocks.PURPUR_BLOCK.defaultBlockState());
            b.box(8, 4, 8, 11, 6, 11, bricks);
            b.box(9, 7, 9, 10, 7, 10, trim);
            b.put(10, 5, 12, Blocks.END_STONE_BRICKS.defaultBlockState());
        }
        if (sunken) {
            for (int x = 1; x < 34; x++) {
                for (int z = 1; z < 34; z++) {
                    if (Math.abs(x - 17) + Math.abs(z - 17) > 28) continue;
                    int edge = Math.max(Math.abs(x - 17), Math.abs(z - 17));
                    if (edge < 12) continue;
                    int height = 3 + (edge - 11) / 2 + Math.floorMod(x * 5 + z * 3, 2);
                    b.box(x, 4, z, x, height, z, rock);
                    if (x < 6 && z >= 18 && z <= 24 || z > 29 && x >= 23 && x <= 27) {
                        b.put(x, height, z, cryingTwisted);
                    }
                    if ((x + z) % 13 == 0) b.put(x, height, z, Blocks.BONE_BLOCK.defaultBlockState());
                }
            }
            // Keep the southern approach exposed despite the accumulated sediment.
            b.box(15, 4, 27, 19, 8, 34, Blocks.AIR.defaultBlockState());
        }
        // Two separate burial pockets inside the overhead coffin. Clear the lid space, too.
        b.chest(15, 16, 16, Direction.WEST);
        b.chest(19, 16, 18, Direction.EAST);
        return b.save();
    }

    private static final class Builder {
        private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        private final Map<BlockPos, BlockState> chests = new LinkedHashMap<>();

        void put(int x, int y, int z, BlockState state) {
            blocks.put(new BlockPos(x, y, z), state);
        }

        void box(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
            for (BlockPos pos : BlockPos.betweenClosed(x1, y1, z1, x2, y2, z2)) {
                blocks.put(pos.immutable(), state);
            }
        }

        void chest(int x, int y, int z, Direction facing) {
            BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing);
            chests.put(new BlockPos(x, y, z), chest);
            put(x, y, z, chest);
            put(x, y + 1, z, Blocks.AIR.defaultBlockState());
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
            tag.put("size", ints(35, 20, 35));
            tag.put("entities", new ListTag());
            Map<BlockState, Integer> palette = new LinkedHashMap<>();
            blocks.forEach((pos, state) -> {
                if (!chests.containsKey(pos)) palette.computeIfAbsent(state, key -> palette.size());
            });
            Map<BlockPos, Integer> chestIds = new LinkedHashMap<>();
            chests.keySet().forEach(pos -> chestIds.put(pos, palette.size() + chestIds.size()));
            ListTag entries = new ListTag();
            blocks.forEach((pos, state) -> {
                CompoundTag entry = new CompoundTag();
                entry.put("pos", ints(pos.getX(), pos.getY(), pos.getZ()));
                if (chests.containsKey(pos)) {
                    entry.putInt("state", chestIds.get(pos));
                    CompoundTag chest = new CompoundTag();
                    chest.putString("id", "minecraft:chest");
                    chest.putString("LootTable", MaledictRealmLoot.RELIQUARY.toString());
                    entry.put("nbt", chest);
                } else entry.putInt("state", palette.get(state));
                entries.add(entry);
            });
            // Vanilla selects one palette uniformly from the template origin. The same origin is
            // reused for every chunk, so box count stays fixed across chunk generation and reloads.
            ListTag palettes = new ListTag();
            for (int chestCount = 0; chestCount <= 2; chestCount++) {
                ListTag states = new ListTag();
                palette.keySet().forEach(state -> states.add(NbtUtils.writeBlockState(state)));
                int chestIndex = 0;
                for (BlockState state : chests.values()) {
                    states.add(NbtUtils.writeBlockState(chestIndex++ < chestCount ? state : Blocks.AIR.defaultBlockState()));
                }
                palettes.add(states);
            }
            tag.put("palettes", palettes);
            tag.put("blocks", entries);
            return tag;
        }

        private static ListTag ints(int... values) {
            ListTag list = new ListTag();
            for (int value : values) list.add(IntTag.valueOf(value));
            return list;
        }
    }

    @Override
    public String getName() {
        return "Inverted Reliquary templates";
    }
}

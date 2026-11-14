package org.brahypno.maledict.data;

import com.google.common.hash.Hashing;
import com.sammy.malum.registry.common.block.BlockEntityRegistry;
import com.sammy.malum.registry.common.block.BlockRegistry;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.brahypno.maledict.common.realm.reliquary.InvertedReliquaryStructure;
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

    private static final int MAX_CRYING_STONE = 8;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState BLACKSTONE = Blocks.BLACKSTONE.defaultBlockState();
    private static final BlockState BRICKS = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState CRACKED = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState BASALT = Blocks.BASALT.defaultBlockState();
    private static final BlockState BASALT_BEAM = BASALT.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
    private static final BlockState SMOOTH_BASALT = Blocks.SMOOTH_BASALT.defaultBlockState();
    private static final BlockState NETHER_BRICKS = Blocks.NETHER_BRICKS.defaultBlockState();
    private static final BlockState RED_NETHER_BRICKS = Blocks.RED_NETHER_BRICKS.defaultBlockState();
    private static final BlockState NETHER_TRIM = Blocks.CHISELED_NETHER_BRICKS.defaultBlockState();
    private static final BlockState CRACKED_NETHER = Blocks.CRACKED_NETHER_BRICKS.defaultBlockState();
    private static final BlockState BONE = Blocks.BONE_BLOCK.defaultBlockState();
    private static final BlockState BONE_BEAM = BONE.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
    private static final BlockState CHAIN = Blocks.CHAIN.defaultBlockState();
    private static final int CENTER = InvertedReliquaryStructure.CENTER;
    private static final int SIZE = InvertedReliquaryStructure.SIZE;
    private static final int HEIGHT = InvertedReliquaryStructure.HEIGHT;

    public static CompoundTag template(String variant) {
        Builder b = new Builder();
        BlockState rock = BlockRegistry.TWISTED_ROCK.get().defaultBlockState();

        hollow(b, rock);
        entrances(b);
        altar(b);
        coffin(b);
        niches(b);

        switch (variant) {
            case "intact" -> intact(b);
            case "fractured" -> fractured(b);
            case "sunken" -> sunken(b, rock);
            default -> throw new IllegalArgumentException("Unknown reliquary variant: " + variant);
        }
        braziers(b);
        b.put(17, 18, 22, CHAIN);
        b.chest(17, 16, 14, Direction.SOUTH);
        b.chest(17, 16, 20, Direction.NORTH);
        cryingStone(b, variant);
        return b.save();
    }

    private static void hollow(Builder b, BlockState rock) {
        BlockState tiles = BlockRegistry.TAINTED_ROCK_TILES.get().defaultBlockState();
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                int dx = Math.abs(x - CENTER);
                int dz = Math.abs(z - CENTER);
                int edge = Math.max(dx, dz);
                boolean south = z >= 26 && dx <= 3;
                boolean west = x <= 8 && Math.abs(z - 12) <= 3;
                if (dx + dz > 29 || (edge == CENTER && !south && !west)) continue;

                int roof = HEIGHT - 1 - Math.max(0, (edge - 12) / 2);
                int wear = Math.floorMod(x * 43 + z * 71, 23);
                b.box(x, 0, z, x, 2, z, rock);
                BlockState paving = dx <= 1 ? NETHER_BRICKS : wear < 3 ? rock : wear < 8 ? tiles : BRICKS;
                if (dx == 10 || dz == 10) paving = BLACKSTONE;
                if (south || west) paving = wear < 5 ? CRACKED : NETHER_BRICKS;
                b.put(x, 3, z, paving);
                b.box(x, 4, z, x, roof - 1, z, AIR);
                b.put(x, roof, z, wear < 7 ? rock : BLACKSTONE);

                if (edge >= 14) {
                    // Keep masonry over both passages so the walls still meet the vault.
                    int base = south ? 9 : west ? 8 : 4;
                    int top = edge == 14 ? HEIGHT - 1 : roof;
                    b.box(x, base, z, x, top, z, wear < 5 ? rock : wear < 9 ? CRACKED : BLACKSTONE);
                    b.put(x, Math.max(base, 6), z, wear < 5 ? CRACKED_NETHER : BRICKS);
                }
            }
        }
    }

    private static void entrances(Builder b) {
        // The south gate frames the long axis. Its eastern shoulder stops at the break.
        b.box(12, 4, 31, 14, 10, 33, BRICKS);
        b.box(20, 4, 31, 22, 8, 33, BLACKSTONE);
        b.box(13, 7, 31, 15, 10, 32, NETHER_BRICKS);
        b.box(15, 9, 31, 18, 10, 32, CRACKED_NETHER);
        b.put(19, 9, 31, NETHER_TRIM);
        b.put(20, 8, 31, BASALT);
        b.box(14, 4, 27, 14, 5, 28, SMOOTH_BASALT);
        b.box(20, 4, 28, 21, 4, 29, CRACKED);

        // The west approach passes a narrower, offset arch and broken column bases.
        b.box(1, 4, 8, 3, 9, 9, BLACKSTONE);
        b.box(1, 4, 15, 3, 8, 16, BRICKS);
        b.box(1, 7, 9, 2, 9, 10, NETHER_BRICKS);
        b.box(1, 8, 11, 2, 9, 13, CRACKED_NETHER);
        b.box(2, 7, 14, 3, 8, 14, NETHER_TRIM);
        b.box(5, 4, 15, 6, 5, 16, CRACKED);
        b.put(5, 6, 16, BASALT);
    }

    private static void altar(Builder b) {
        BlockState runes = BlockRegistry.RUNIC_TAINTED_ROCK_BRICKS.get().defaultBlockState();
        b.box(13, 3, 13, 21, 3, 21, BLACKSTONE);
        b.box(14, 3, 14, 20, 3, 20, BRICKS);
        b.box(15, 3, 15, 19, 3, 19, runes);

        // Three flush cracks leave the ritual floor passable in every direction.
        b.box(17, 3, 13, 17, 3, 16, CRACKED_NETHER);
        b.box(13, 3, 18, 15, 3, 18, CRACKED);
        b.box(19, 3, 19, 21, 3, 19, CRACKED);
        BlockState lip = Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState();
        b.box(16, 4, 15, 18, 4, 15, lip);
        b.put(15, 4, 17, lip);
        b.put(19, 4, 17, lip);
        b.put(13, 4, 13, CRACKED);
        b.put(21, 4, 13, lip);
        b.put(13, 4, 21, lip);
        b.put(21, 4, 21, SMOOTH_BASALT);
        b.put(17, 4, 17, MaledictBlocks.RELIQUARY_ALTAR.get().defaultBlockState());
    }

    private static void coffin(Builder b) {
        // Taper the red casing in plan and in section. The bottom seal faces the altar.
        b.box(17, 13, 15, 17, 13, 19, RED_NETHER_BRICKS);
        b.box(16, 14, 14, 18, 14, 20, RED_NETHER_BRICKS);
        b.box(16, 15, 12, 18, 17, 22, RED_NETHER_BRICKS);
        b.box(15, 15, 14, 19, 17, 20, RED_NETHER_BRICKS);
        b.box(16, 16, 14, 18, 17, 20, AIR);
        b.box(16, 18, 13, 18, 18, 21, BLACKSTONE);
        b.box(17, 15, 12, 17, 17, 12, NETHER_TRIM);
        b.box(17, 15, 22, 17, 17, 22, NETHER_TRIM);
        for (int z : new int[]{14, 20}) {
            b.box(15, 15, z, 15, 17, z, SMOOTH_BASALT);
            b.box(19, 15, z, 19, 17, z, SMOOTH_BASALT);
            b.put(16, 15, z, BONE_BEAM);
            b.put(18, 15, z, BONE_BEAM);
        }
        // Mine through the marked side seal to reach the burial pockets.
        b.box(19, 16, 19, 19, 17, 19, NETHER_TRIM);
        b.put(17, 13, 17, Blocks.CRYING_OBSIDIAN.defaultBlockState());
        b.put(17, 18, 12, CHAIN);
        b.put(17, 19, 12, BASALT);
        b.put(17, 19, 22, BASALT);
    }

    private static void niches(Builder b) {
        for (int x : new int[]{10, 17, 24}) {
            b.box(x - 2, 4, 3, x + 2, 8, 3, BLACKSTONE);
            b.box(x - 1, 5, 3, x + 1, 7, 3, AIR);
            b.box(x - 1, 4, 4, x + 1, 4, 4, CRACKED_NETHER);
            b.box(x - 2, 8, 4, x + 2, 8, 4, NETHER_TRIM);
            b.put(x, 5, 3, BONE);
        }
        b.put(6, 6, 16, Blocks.SOUL_LANTERN.defaultBlockState());
        b.put(20, 5, 28, Blocks.SOUL_LANTERN.defaultBlockState());
        b.put(28, 18, 17, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    private static void braziers(Builder b) {
        BlockState brazier = BlockRegistry.TAINTED_ETHER_BRAZIER.get().defaultBlockState();
        CompoundTag ether = new CompoundTag();
        ether.putString("id", BlockEntityType.getKey(BlockEntityRegistry.ETHER.get()).toString());
        // Structure placement skips the item callback that normally initializes the flame color.
        ether.putInt("firstColor", 0xA581CC);
        for (BlockPos pos : new BlockPos[]{
                new BlockPos(12, 4, 4), new BlockPos(22, 4, 4),
                new BlockPos(4, 4, 17), new BlockPos(30, 4, 17),
                new BlockPos(10, 4, 30), new BlockPos(24, 4, 30)
        }) {
            b.putBlockEntity(pos, brazier, ether);
        }
    }

    private static void column(Builder b, int x, int z, int height) {
        b.box(x - 1, 4, z - 1, x + 1, 4, z + 1, BRICKS);
        b.box(x, 5, z, x, height, z, BASALT);
        b.put(x, height, z, SMOOTH_BASALT);
        b.put(x, 5, z + 1, NETHER_TRIM);
    }

    private static void ribSide(Builder b, int z, boolean west) {
        int side = west ? 1 : -1;
        int edge = west ? 5 : 29;
        b.box(edge, 14, z, edge + 2 * side, 15, z, BRICKS);
        b.box(edge + 2 * side, 16, z, edge + 4 * side, 16, z, BASALT_BEAM);
        b.box(edge + 4 * side, 17, z, edge + 7 * side, 17, z, BASALT_BEAM);
        b.box(edge + 7 * side, 18, z, edge + 12 * side, 18, z, BASALT_BEAM);
    }

    private static void intact(Builder b) {
        column(b, 5, 9, 15);
        column(b, 30, 9, 15);
        column(b, 5, 22, 15);
        column(b, 30, 22, 15);
        ribSide(b, 9, true);
        ribSide(b, 9, false);
        ribSide(b, 22, true);
        ribSide(b, 22, false);
        b.box(17, 18, 9, 17, 18, 11, BASALT_BEAM);
        column(b, 5, 27, 7);
        column(b, 29, 29, 9);
        b.box(6, 4, 23, 6, 5, 25, CRACKED);
        b.put(6, 6, 24, BONE_BEAM);
    }

    private static void fractured(Builder b) {
        column(b, 5, 9, 7);
        column(b, 30, 9, 15);
        column(b, 5, 22, 15);
        column(b, 30, 22, 15);
        ribSide(b, 9, false);
        ribSide(b, 22, true);
        ribSide(b, 22, false);
        b.box(15, 18, 9, 17, 18, 11, BASALT_BEAM);
        b.put(15, 18, 11, CHAIN);
        b.put(15, 17, 11, CHAIN);
        b.put(15, 19, 11, BASALT);
        b.box(16, 16, 12, 16, 17, 12, AIR);
        b.box(15, 16, 11, 16, 16, 11, CRACKED_NETHER);
        b.put(16, 17, 11, BONE_BEAM);

        b.box(4, 4, 5, 9, 5, 7, CRACKED);
        b.box(5, 6, 5, 7, 7, 6, BASALT_BEAM);
        b.box(6, 8, 5, 7, 8, 5, SMOOTH_BASALT);
        b.put(8, 6, 6, BONE_BEAM);
        // The northwest collapse tears through the wall and into the first span of the vault.
        b.box(1, 13, 6, 3, 17, 8, AIR);
        b.box(3, 17, 6, 5, 19, 8, AIR);
        b.box(5, 18, 7, 8, 19, 9, AIR);
        b.box(8, 18, 9, 11, 18, 10, AIR);
        b.box(30, 4, 25, 31, 5, 26, CRACKED);
        b.box(30, 5, 24, 30, 7, 25, CRACKED);
    }

    private static void sunken(Builder b, BlockState rock) {
        column(b, 5, 9, 15);
        column(b, 30, 9, 15);
        column(b, 5, 22, 8);
        column(b, 30, 22, 15);
        ribSide(b, 9, true);
        ribSide(b, 9, false);
        ribSide(b, 22, false);
        b.box(17, 18, 9, 17, 18, 11, BASALT_BEAM);
        b.box(12, 18, 22, 16, 18, 22, BASALT_BEAM);
        b.put(13, 17, 22, CHAIN);

        // Sediment breaches the southwest wall, tapering before the arena and both gates.
        b.box(1, 14, 21, 3, 16, 26, AIR);
        b.box(2, 13, 22, 3, 13, 25, AIR);
        for (int x = 1; x <= 6; x++) {
            for (int z = 18; z <= 29; z++) {
                if (x + 34 - z < 7) continue;
                int height = Math.max(4, 11 - x - Math.abs(z - 24) / 2);
                b.box(x, 4, z, x, height, z, rock);
                if ((x + z) % 4 == 0) b.put(x, height, z, Blocks.NETHERRACK.defaultBlockState());
            }
        }
        b.box(5, 8, 21, 6, 8, 23, CRACKED);
        b.put(6, 6, 25, BONE_BEAM);
        b.put(7, 4, 26, CRACKED_NETHER);
        b.put(8, 4, 28, rock);
    }

    private static void cryingStone(Builder b, String variant) {
        BlockState tainted = MaledictBlocks.CRYING_TAINTED_ROCK.get().defaultBlockState();
        BlockState twisted = MaledictBlocks.CRYING_TWISTED_ROCK.get().defaultBlockState();

        b.put(16, 3, 17, tainted);
        b.put(18, 3, 17, twisted);
        b.put(16, 14, 17, tainted);
        b.put(18, 14, 17, twisted);
        switch (variant) {
            case "intact" -> {
                b.put(18, 17, 12, tainted);
                b.put(5, 14, 22, twisted);
            }
            case "fractured" -> {
                b.put(15, 16, 11, tainted);
                b.put(14, 18, 9, twisted);
            }
            case "sunken" -> {
                b.put(16, 18, 22, tainted);
                b.put(3, 8, 24, twisted);
            }
        }

        long count = b.blocks.values().stream()
                .filter(state -> state.is(tainted.getBlock()) || state.is(twisted.getBlock()))
                .count();
        if (count > MAX_CRYING_STONE) {
            throw new IllegalStateException(variant + " reliquary has " + count + " crying stones");
        }
    }

    private static final class Builder {
        private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        private final Map<BlockPos, BlockState> chests = new LinkedHashMap<>();
        private final Map<BlockPos, CompoundTag> blockEntityData = new LinkedHashMap<>();

        void put(int x, int y, int z, BlockState state) {
            blocks.put(new BlockPos(x, y, z), state);
        }

        void putBlockEntity(BlockPos pos, BlockState state, CompoundTag data) {
            blocks.put(pos, state);
            blockEntityData.put(pos, data);
        }

        void box(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(x1, y1, z1), new BlockPos(x2, y2, z2))) {
                blocks.put(pos.immutable(), state);
            }
        }

        void chest(int x, int y, int z, Direction facing) {
            BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing);
            BlockPos pos = new BlockPos(x, y, z);
            chests.put(pos, chest);
            CompoundTag data = new CompoundTag();
            data.putString("id", "minecraft:chest");
            data.putString("LootTable", MaledictRealmLoot.RELIQUARY.toString());
            putBlockEntity(pos, chest, data);
            put(x, y + 1, z, Blocks.AIR.defaultBlockState());
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
            tag.put("size", ints(SIZE, HEIGHT, SIZE));
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
                entry.putInt("state", chests.containsKey(pos) ? chestIds.get(pos) : palette.get(state));
                CompoundTag data = blockEntityData.get(pos);
                if (data != null) entry.put("nbt", data);
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

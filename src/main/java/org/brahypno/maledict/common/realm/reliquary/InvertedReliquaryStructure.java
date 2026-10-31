package org.brahypno.maledict.common.realm.reliquary;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.brahypno.maledict.registry.MaledictStructures;

import java.util.Optional;

/** Bury the roof below the lowest sampled surface; caves are deliberately allowed. */
public final class InvertedReliquaryStructure extends Structure {
    public static final Codec<InvertedReliquaryStructure> CODEC = simpleCodec(InvertedReliquaryStructure::new);
    public static final int SIZE = 35;
    public static final int HEIGHT = 20;
    public static final int CENTER = SIZE / 2;

    public InvertedReliquaryStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        int surface = context.heightAccessor().getMaxBuildHeight();
        for (int dx : new int[]{-CENTER, 0, CENTER}) {
            for (int dz : new int[]{-CENTER, 0, CENTER}) {
                surface = Math.min(surface, context.chunkGenerator().getBaseHeight(x + dx, z + dz,
                        Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState()));
            }
        }
        int floor = surface - HEIGHT - 45 - context.random().nextInt(16);
        if (floor < context.heightAccessor().getMinBuildHeight() + 8) return Optional.empty();
        // Only the central foundation needs support. Do not reject a site because a cave cuts the walls.
        var column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        if (!column.getBlock(floor).isSolid() || !column.getBlock(floor - 1).isSolid()) {
            return Optional.empty();
        }
        BlockPos origin = new BlockPos(x - CENTER, floor, z - CENTER);
        Rotation rotation = Rotation.getRandom(context.random());
        int roll = context.random().nextInt(4);
        String variant = roll == 0 ? "intact" : roll == 3 ? "sunken" : "fractured";
        return Optional.of(new GenerationStub(new BlockPos(x, floor, z), pieces -> pieces.addPiece(
                new InvertedReliquaryPiece(context.structureTemplateManager(), origin, rotation, variant))));
    }

    @Override
    public StructureType<?> type() {
        return MaledictStructures.INVERTED_RELIQUARY.get();
    }
}

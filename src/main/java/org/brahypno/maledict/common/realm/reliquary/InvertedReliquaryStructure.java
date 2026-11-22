package org.brahypno.maledict.common.realm.reliquary;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.brahypno.maledict.registry.MaledictStructures;

import java.util.Optional;

/**
 * 倒置圣物匣：把屋顶埋到地表以下的完整地宫。
 * <p>
 * 选址只采样本区块中心与四角（±CENTER）的洋底高度并取最低值，再从那里往下让 45~60 格；
 * 只要求中心基座下方两格是实心——洞穴切穿墙体是设计的一部分，不因为侧壁被挖空就弃址。
 */
public final class InvertedReliquaryStructure extends Structure {
    /**
     * 1.21 的 {@code Structure#simpleCodec} 返回 {@link MapCodec}（1.20.1 是 {@code Codec}），
     * {@code StructureType#codec()} 也改收 {@code MapCodec}，两边类型必须一起换。
     */
    public static final MapCodec<InvertedReliquaryStructure> CODEC = simpleCodec(InvertedReliquaryStructure::new);
    public static final int SIZE = 35;
    public static final int HEIGHT = 20;
    public static final int CENTER = SIZE / 2;

    public InvertedReliquaryStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    @SuppressWarnings("deprecation") // 世界生成期拿不到 Level，isSolid()（legacy solid 标记）仍是唯一可用的实心判定。
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
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
        // 只有中心地基需要支撑：墙被洞穴切掉不影响地宫成立，也不该因此丢掉整座结构。
        var column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        if (!column.getBlock(floor).isSolid() || !column.getBlock(floor - 1).isSolid()) {
            return Optional.empty();
        }
        BlockPos origin = new BlockPos(x - CENTER, floor, z - CENTER);
        Rotation rotation = Rotation.getRandom(context.random());
        // 四个 roll 里「完好」与「沉陷」各占一档，剩下两档都是「破裂」：残破才是常态。
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

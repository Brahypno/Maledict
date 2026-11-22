package org.brahypno.maledict.common.realm.reliquary;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictStructures;

/**
 * 单个整块模板的地宫：三份 35×20×35 的 {@code maledict:inverted_reliquary/<variant>} 各是一个 piece。
 * <p>
 * 旋转轴固定在模板中心 {@code (17,0,17)}，否则 90/270 度会把整座地宫绕原点到区块外去。
 */
public final class InvertedReliquaryPiece extends TemplateStructurePiece {
    public InvertedReliquaryPiece(StructureTemplateManager manager, BlockPos origin, Rotation rotation, String variant) {
        super(MaledictStructures.RELIQUARY_PIECE.get(), 0, manager,
                templateId(variant), templateId(variant).toString(), settings(rotation), origin);
    }

    public InvertedReliquaryPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        // 反序列化走不了玩家的调用点，旋转只能自己存一份：见 addAdditionalSaveData 的 "Rotation"。
        super(MaledictStructures.RELIQUARY_PIECE.get(), tag, context.structureTemplateManager(),
                id -> settings(Rotation.valueOf(tag.getString("Rotation"))));
    }

    public static ResourceLocation templateId(String variant) {
        return ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "inverted_reliquary/" + variant);
    }

    public static StructurePlaceSettings settings(Rotation rotation) {
        // 1.21 把 1.20.1 的 setKeepLiquids(false) 换成了 LiquidSettings：IGNORE_WATERLOGGING 即「不保留原地液体」。
        return new StructurePlaceSettings().setRotation(rotation)
                .setRotationPivot(new BlockPos(17, 0, 17)).setIgnoreEntities(true)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rotation", getRotation().name());
    }

    @Override
    protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level,
                                    RandomSource random, BoundingBox bounds) {
        // 模板里放的是「放下后自己排一次 tick」的祭坛方块，没有需要在这里处理的方块实体。
    }
}

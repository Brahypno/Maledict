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
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictStructures;

public final class InvertedReliquaryPiece extends TemplateStructurePiece {
    public InvertedReliquaryPiece(StructureTemplateManager manager, BlockPos origin, Rotation rotation, String variant) {
        super(MaledictStructures.RELIQUARY_PIECE.get(), 0, manager,
                templateId(variant), templateId(variant).toString(), settings(rotation), origin);
    }

    public InvertedReliquaryPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(MaledictStructures.RELIQUARY_PIECE.get(), tag, context.structureTemplateManager(),
                id -> settings(Rotation.valueOf(tag.getString("Rotation"))));
    }

    public static ResourceLocation templateId(String variant) {
        return ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "inverted_reliquary/" + variant);
    }

    public static StructurePlaceSettings settings(Rotation rotation) {
        return new StructurePlaceSettings().setRotation(rotation)
                .setRotationPivot(new BlockPos(17, 0, 17)).setIgnoreEntities(true).setKeepLiquids(false)
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
        // The template contains a scheduled one-use altar block, not a ticking block entity.
    }
}

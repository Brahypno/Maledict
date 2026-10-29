package org.brahypno.maledict.data;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictBlocks;

public final class MaledictBlockStates extends BlockStateProvider {
    public MaledictBlockStates(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Maledict.MODID, existingFiles);
    }

    @Override
    protected void registerStatesAndModels() {
        leadOre(MaledictBlocks.TAINTED_MALIGNANT_LEAD_ORE.get(), "tainted/tainted_rock");
        leadOre(MaledictBlocks.TWISTED_MALIGNANT_LEAD_ORE.get(), "twisted/twisted_rock");
        groundcoverModels();

        ModelFile mnemonic = models().getBuilder("mnemonic_obelisk")
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.fromNamespaceAndPath("malum", "block/runewood_obelisk")))
                .texture("obelisk", modLoc("block/mnemonic_obelisk"));
        ModelFile mnemonicComponent = models().getBuilder("mnemonic_obelisk_component")
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.fromNamespaceAndPath("malum", "block/runewood_obelisk_component")))
                .texture("obelisk", modLoc("block/mnemonic_obelisk"));
        simpleBlock(MaledictBlocks.MNEMONIC_OBELISK.get(), mnemonic);
        simpleBlock(MaledictBlocks.MNEMONIC_OBELISK_COMPONENT.get(), mnemonicComponent);

        ModelFile soulwood = models().getBuilder("soulwood_obelisk")
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.fromNamespaceAndPath("malum", "block/runewood_obelisk")))
                .texture("obelisk", modLoc("block/runewood_obelisk"));
        ModelFile soulwoodComponent = models().getBuilder("soulwood_obelisk_component")
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.fromNamespaceAndPath("malum", "block/runewood_obelisk_component")))
                .texture("obelisk", modLoc("block/runewood_obelisk"));
        simpleBlock(MaledictBlocks.SOULWOOD_OBELISK.get(), soulwood);
        simpleBlock(MaledictBlocks.SOULWOOD_OBELISK_COMPONENT.get(), soulwoodComponent);
    }

    private void groundcoverModels() {
        var leaves = models().getBuilder("withered_leaf_litter").renderType("cutout")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("block/block")))
                .texture("particle", "malum:block/soulwood/soulwood_leaves")
                .texture("cover", "malum:block/soulwood/soulwood_leaves");
        piece(leaves, 1, 0, 2, 10, 1, 9, "cover");
        piece(leaves, 6, 0, 7, 15, 1, 15, "cover");
        piece(leaves, 2, 0, 11, 6, 1, 15, "cover");
        cover(MaledictBlocks.WITHERED_LEAF_LITTER.get(), leaves);

        var roots = woodCover("exposed_soulwood_roots");
        piece(roots, 1, 0, 6, 15, 3, 9, "wood");
        piece(roots, 3, 0, 2, 6, 2, 14, "wood");
        piece(roots, 10, 0, 5, 12, 4, 13, "wood");
        cover(MaledictBlocks.EXPOSED_SOULWOOD_ROOTS.get(), roots);

        var branches = woodCover("fallen_soulwood_branches");
        piece(branches, 2, 0, 5, 14, 2, 7, "wood");
        piece(branches, 7, 0, 3, 9, 2, 14, "wood");
        piece(branches, 9, 0, 10, 14, 1, 12, "wood");
        cover(MaledictBlocks.FALLEN_SOULWOOD_BRANCHES.get(), branches);

        var mushroom = woodCover("witherlight_mushroom")
                .texture("cap", mcLoc("block/quartz_block_top"));
        piece(mushroom, 6, 0, 6, 8, 5, 8, "wood");
        piece(mushroom, 3, 4, 3, 11, 6, 11, "cap");
        piece(mushroom, 5, 6, 5, 9, 7, 9, "cap");
        piece(mushroom, 11, 0, 10, 12, 3, 11, "wood");
        piece(mushroom, 9, 2, 8, 14, 4, 13, "cap");
        cover(MaledictBlocks.WITHERLIGHT_MUSHROOM.get(), mushroom);
    }

    private BlockModelBuilder woodCover(String name) {
        return models().getBuilder(name).parent(new ModelFile.UncheckedModelFile(mcLoc("block/block")))
                .texture("particle", "malum:block/soulwood/soulwood_log")
                .texture("wood", "malum:block/soulwood/soulwood_log");
    }

    private void piece(BlockModelBuilder model, int x, int y, int z, int endX, int endY, int endZ, String texture) {
        model.element().from(x, y, z).to(endX, endY, endZ)
                .allFaces((direction, face) -> face.texture("#" + texture)).end();
    }

    private void cover(Block block, ModelFile model) {
        getVariantBuilder(block).partialState().setModels(
                new ConfiguredModel(model, 0, 0, false), new ConfiguredModel(model, 0, 90, false),
                new ConfiguredModel(model, 0, 180, false), new ConfiguredModel(model, 0, 270, false));
        simpleBlockItem(block, model);
    }

    private void leadOre(Block block, String host) {
        var model = models().getBuilder(ForgeRegistries.BLOCKS.getKey(block).getPath())
                .parent(new ModelFile.UncheckedModelFile(mcLoc("block/block")))
                .renderType("cutout_mipped")
                .texture("particle", ResourceLocation.fromNamespaceAndPath("malum", "block/arcane_rock/" + host))
                .texture("host", ResourceLocation.fromNamespaceAndPath("malum", "block/arcane_rock/" + host))
                .texture("ore", modLoc("block/malignant_lead_ore_overlay"));
        model.element().from(0, 0, 0).to(16, 16, 16)
                .allFaces((direction, face) -> face.texture("#host").cullface(direction)).end();
        model.element().from(0, 0, 0).to(16, 16, 16)
                .allFaces((direction, face) -> face.texture("#ore").cullface(direction)).end();
        simpleBlockWithItem(block, model);
    }
}

package org.brahypno.maledict.data;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.minecraft.core.registries.BuiltInRegistries;
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

    private void leadOre(Block block, String host) {
        var model = models().getBuilder(BuiltInRegistries.BLOCK.getKey(block).getPath())
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

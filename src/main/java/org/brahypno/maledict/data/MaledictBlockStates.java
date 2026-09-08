package org.brahypno.maledict.data;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictBlocks;

public final class MaledictBlockStates extends BlockStateProvider {
    public MaledictBlockStates(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Maledict.MODID, existingFiles);
    }

    @Override
    protected void registerStatesAndModels() {
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
}

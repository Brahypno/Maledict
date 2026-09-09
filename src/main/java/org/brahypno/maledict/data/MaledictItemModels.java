package org.brahypno.maledict.data;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.loaders.SeparateTransformsModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.SpiritArrowType;
import org.brahypno.maledict.registry.MaledictItems;

public final class MaledictItemModels extends ItemModelProvider {
    public MaledictItemModels(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Maledict.MODID, existingFiles);
    }

    @Override
    @SuppressWarnings({"removal"})
    protected void registerModels() {
        for (SpiritArrowType arrowType : SpiritArrowType.values()) {
            getBuilder(arrowType.name().toLowerCase() + "_spirit_arrow")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", mcLoc("item/arrow"));
        }

        ItemModelBuilder pulling0 = withExistingParent("remembrance_bow_pulling_0", mcLoc("item/bow"))
                .texture("layer0", modLoc("item/remembrance_bow_pulling_0"));
        ItemModelBuilder pulling1 = withExistingParent("remembrance_bow_pulling_1", mcLoc("item/bow"))
                .texture("layer0", modLoc("item/remembrance_bow_pulling_1"));
        ItemModelBuilder pulling2 = withExistingParent("remembrance_bow_pulling_2", mcLoc("item/bow"))
                .texture("layer0", modLoc("item/remembrance_bow_pulling_2"));
        withExistingParent("remembrance_bow", mcLoc("item/bow"))
                .texture("layer0", modLoc("item/remembrance_bow"))
                .override().predicate(mcLoc("pulling"), 1.0F).model(pulling0).end()
                .override().predicate(mcLoc("pulling"), 1.0F).predicate(mcLoc("pull"), 0.65F).model(pulling1).end()
                .override().predicate(mcLoc("pulling"), 1.0F).predicate(mcLoc("pull"), 0.9F).model(pulling2).end();

        ItemModelBuilder handheld = getBuilder("incursus_blade_handheld")
                .parent(new ModelFile.UncheckedModelFile(
                        new ResourceLocation("malum", "item/handheld_large")))
                .texture("layer0", modLoc("item/incursus_blade_huge"));
        ItemModelBuilder gui = withExistingParent("incursus_blade_gui", "item/handheld")
                .texture("layer0", modLoc("item/incursus_blade"));

        getBuilder("incursus_blade")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/handheld")))
                .customLoader(SeparateTransformsModelBuilder::begin)
                .base(handheld)
                .perspective(ItemDisplayContext.GUI, gui)
                .perspective(ItemDisplayContext.FIXED, gui);

        getBuilder("mnemonic_obelisk")
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.fromNamespaceAndPath("malum", "item/runewood_obelisk")))
                .texture("0", modLoc("block/mnemonic_obelisk"));
        getBuilder("soulwood_obelisk")
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.fromNamespaceAndPath("malum", "item/runewood_obelisk")))
                .texture("0", modLoc("block/runewood_obelisk"));
    }
}

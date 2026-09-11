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

        registerBowModel("remembrance_bow");
        registerSeparateBowModel("elegy_bow");

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

    private void registerBowModel(String name) {
        ItemModelBuilder pulling0 = withExistingParent(name + "_pulling_0", mcLoc("item/bow"))
                .texture("layer0", modLoc("item/" + name + "_pulling_0"));
        ItemModelBuilder pulling1 = withExistingParent(name + "_pulling_1", mcLoc("item/bow"))
                .texture("layer0", modLoc("item/" + name + "_pulling_1"));
        ItemModelBuilder pulling2 = withExistingParent(name + "_pulling_2", mcLoc("item/bow"))
                .texture("layer0", modLoc("item/" + name + "_pulling_2"));
        withExistingParent(name, mcLoc("item/bow"))
                .texture("layer0", modLoc("item/" + name))
                .override().predicate(mcLoc("pulling"), 1.0F).model(pulling0).end()
                .override().predicate(mcLoc("pulling"), 1.0F).predicate(mcLoc("pull"), 0.65F).model(pulling1).end()
                .override().predicate(mcLoc("pulling"), 1.0F).predicate(mcLoc("pull"), 0.9F).model(pulling2).end();
    }

    private void registerSeparateBowModel(String name) {
        ItemModelBuilder pulling0 = separateBowModel(name + "_pulling_0");
        ItemModelBuilder pulling1 = separateBowModel(name + "_pulling_1");
        ItemModelBuilder pulling2 = separateBowModel(name + "_pulling_2");
        separateBowModel(name)
                .override().predicate(mcLoc("pulling"), 1.0F).model(pulling0).end()
                .override().predicate(mcLoc("pulling"), 1.0F).predicate(mcLoc("pull"), 0.65F).model(pulling1).end()
                .override().predicate(mcLoc("pulling"), 1.0F).predicate(mcLoc("pull"), 0.9F).model(pulling2).end();
    }

    private ItemModelBuilder separateBowModel(String name) {
        ItemModelBuilder inventory = withExistingParent(name + "_inventory", mcLoc("item/bow"))
                .texture("layer0", modLoc("item/" + name));
        ItemModelBuilder handheld = withExistingParent(name + "_handheld", mcLoc("item/bow"))
                .texture("layer0", modLoc("item/" + name + "_huge"));
        ItemModelBuilder model = withExistingParent(name, mcLoc("item/bow"));
        model.customLoader(SeparateTransformsModelBuilder::begin)
                .base(handheld)
                .perspective(ItemDisplayContext.GUI, inventory)
                .perspective(ItemDisplayContext.FIXED, inventory);
        return model;
    }
}

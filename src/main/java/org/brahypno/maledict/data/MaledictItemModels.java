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
import org.brahypno.maledict.common.item.AgeOfEnlightenmentItem;
import org.brahypno.maledict.common.item.SpiritArrowType;
import org.brahypno.maledict.registry.MaledictItems;

public final class MaledictItemModels extends ItemModelProvider {
    public MaledictItemModels(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Maledict.MODID, existingFiles);
    }

    @Override
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
                        ResourceLocation.fromNamespaceAndPath("malum", "item/handheld_large")))
                .texture("layer0", modLoc("item/incursus_blade_huge"));
        ItemModelBuilder gui = withExistingParent("incursus_blade_gui", "item/handheld")
                .texture("layer0", modLoc("item/incursus_blade"));

        getBuilder("incursus_blade")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/handheld")))
                .customLoader(SeparateTransformsModelBuilder::begin)
                .base(handheld)
                .perspective(ItemDisplayContext.GUI, gui)
                .perspective(ItemDisplayContext.FIXED, gui);

        registerAgeOfEnlightenmentModel();

        getBuilder("curio_return_token")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/curio_return_token"));

        getBuilder("rune_of_satiation")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/runes/rune_of_satiation"));

        getBuilder("rune_of_decay")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/runes/rune_of_decay"));

        getBuilder("rune_of_thinning")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/runes/rune_of_thinning"));

        getBuilder("rune_of_ripening")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/runes/rune_of_ripening"));

        getBuilder("mnemonic_obelisk")
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.fromNamespaceAndPath("malum", "item/runewood_obelisk")))
                .texture("0", modLoc("block/mnemonic_obelisk"));
        getBuilder("soulwood_obelisk")
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.fromNamespaceAndPath("malum", "item/runewood_obelisk")))
                .texture("0", modLoc("block/runewood_obelisk"));
    }

    /**
     * 启蒙之年：两张贴图。
     *
     * <p>谓词 {@code maledict:enlightened} 由 {@code MaledictItemProperties} 注册，
     * 佩戴者身上有启蒙之年药水效果时置 1，切到 {@code _enlightened} 那张。
     * 谓词名和物品侧共用 {@link AgeOfEnlightenmentItem#ENLIGHTENED_PROPERTY}，
     * 免得两边各写一遍字符串、改了模型忘了改代码。
     */
    private void registerAgeOfEnlightenmentModel() {
        ItemModelBuilder enlightened = getBuilder("age_of_enlightenment_enlightened")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/age_of_enlightenment_enlightened"));

        getBuilder("age_of_enlightenment")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/age_of_enlightenment"))
                .override()
                .predicate(AgeOfEnlightenmentItem.ENLIGHTENED_PROPERTY, 1.0F)
                .model(enlightened)
                .end();
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

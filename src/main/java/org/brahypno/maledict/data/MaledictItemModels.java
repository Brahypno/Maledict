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
import org.brahypno.maledict.common.item.IncursusBladeItem;
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

        registerIncursusBladeModels();

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

        getBuilder("rune_of_the_pack")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/runes/rune_of_the_pack"));

        getBuilder("rune_of_ripening")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/runes/rune_of_ripening"));

        getBuilder("rune_of_stagnant_evolution")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/runes/rune_of_stagnant_evolution"));

        getBuilder("malignant_pewter_tablet")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/malignant_pewter_tablet"));

        getBuilder("mnemonic_obelisk")
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.fromNamespaceAndPath("malum", "item/runewood_obelisk")))
                .texture("0", modLoc("block/mnemonic_obelisk"));
        getBuilder("soulwood_obelisk")
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.fromNamespaceAndPath("malum", "item/runewood_obelisk")))
                .texture("0", modLoc("block/runewood_obelisk"));

        // 刷怪蛋用原版那张模板贴图，两种颜色由物品自己给。
        registerSpawnEggModel("first_vicissitude_phase_one_spawn_egg");
        registerSpawnEggModel("first_vicissitude_phase_two_spawn_egg");
    }

    /**
     * 神侵恶刃：手上用大贴图，物品栏里用 16x16 那张，并且会在物品栏里眨眼。
     *
     * <p>眨眼靠模型覆盖：谓词 {@code maledict:blinking}（{@code MaledictItemProperties} 注册，
     * 名字来自 {@link IncursusBladeItem#BLINK_PROPERTY}）在闭眼的那几个 tick 置 1，整份模型
     * 换成 {@code incursus_blade_blink}。那一份的 {@code base}（手上那张大贴图）与 {@code fixed}
     * （展示框）跟常态一模一样，只有 {@code gui} 换成闭眼贴图——所以「眨眼」只在物品栏里看得见，
     * 拿在手上和挂在展示框上都不受影响。
     */
    private void registerIncursusBladeModels() {
        ItemModelBuilder handheld = getBuilder("incursus_blade_handheld")
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.fromNamespaceAndPath("malum", "item/handheld_large")))
                .texture("layer0", modLoc("item/incursus_blade_huge"));
        ItemModelBuilder gui = withExistingParent("incursus_blade_gui", "item/handheld")
                .texture("layer0", modLoc("item/incursus_blade"));
        ItemModelBuilder blinkGui = withExistingParent("incursus_blade_gui_blink", "item/handheld")
                .texture("layer0", modLoc("item/incursus_blade_blink"));

        ItemModelBuilder blink = withExistingParent("incursus_blade_blink", mcLoc("item/handheld"));
        blink.customLoader(SeparateTransformsModelBuilder::begin)
                .base(handheld)
                .perspective(ItemDisplayContext.GUI, blinkGui)
                .perspective(ItemDisplayContext.FIXED, gui);

        ItemModelBuilder blade = withExistingParent("incursus_blade", mcLoc("item/handheld"));
        blade.customLoader(SeparateTransformsModelBuilder::begin)
                .base(handheld)
                .perspective(ItemDisplayContext.GUI, gui)
                .perspective(ItemDisplayContext.FIXED, gui);
        blade.override()
                .predicate(IncursusBladeItem.BLINK_PROPERTY, 0.5F)
                .model(blink)
                .end();
    }

    private void registerSpawnEggModel(String name) {
        getBuilder(name).parent(new ModelFile.UncheckedModelFile(mcLoc("item/template_spawn_egg")));
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

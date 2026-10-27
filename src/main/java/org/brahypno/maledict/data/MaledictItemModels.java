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
import org.brahypno.maledict.common.curio.enlightenment.AgeOfEnlightenmentItem;
import org.brahypno.maledict.common.curio.hangman.HangmanFoolItem;
import org.brahypno.maledict.common.weapon.incursus.IncursusBladeItem;
import org.brahypno.maledict.common.weapon.remembrance.SpiritArrowType;
import org.brahypno.maledict.registry.MaledictItems;

public final class MaledictItemModels extends ItemModelProvider {
    public MaledictItemModels(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Maledict.MODID, existingFiles);
    }

    @Override
    protected void registerModels() {
        getBuilder("invocation_sigil")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/invocation_sigil"));
        for (SpiritArrowType arrowType : SpiritArrowType.values()) {
            getBuilder(arrowType.name().toLowerCase() + "_spirit_arrow")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", mcLoc("item/arrow"));
        }

        registerBowModel("remembrance_bow");
        registerSeparateBowModel("elegy_bow");

        registerIncursusBladeModels();
        loverAxeModel("lover_axe", "lover_axe");
        loverAxeModel("meltfall_axe", "lover_axe_lavender");

        registerAgeOfEnlightenmentModel();
        registerHangmanFoolModels();
        basicItem(MaledictItems.EIDOLON_OF_THE_HIGH_PRIESTESS.get());

        basicItem(MaledictItems.BLEEDING_EYE.get());
        basicItem(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get());

        withExistingParent("raven_feather", mcLoc("item/generated"))
                .texture("layer0", modLoc("entity/raven_feather"));

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

        getBuilder("rune_of_rotten_bone")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/runes/rune_of_rotten_bone"));

        getBuilder("rune_of_melancholia")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/runes/rune_of_melancholia"));

        getBuilder("rune_of_bliss")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/runes/rune_of_bliss"));

        getBuilder("rune_of_the_fallen")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/runes/rune_of_the_fallen"));

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

        registerSpawnEggModel("first_vicissitude_phase_one_spawn_egg");
        registerSpawnEggModel("raven_spawn_egg");
        registerSpawnEggModel("first_vicissitude_phase_two_spawn_egg");
    }

    private void registerHangmanFoolModels() {
        // Each of the first three stages has its own texture.
        ItemModelBuilder second = getBuilder("hangman_fool_stage_1")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/hangman_fool_stage_1"));
        ItemModelBuilder third = getBuilder("hangman_fool_stage_2")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/hangman_fool_stage_2"));
        getBuilder("hangman_fool")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/hangman_fool"))
                .override().predicate(HangmanFoolItem.STAGE_PROPERTY, 0.5F).model(second).end()
                .override().predicate(HangmanFoolItem.STAGE_PROPERTY, 1.0F).model(third).end();
    }

    /** 神侵恶刃的眨眼：只有 GUI 那层换成闭眼贴图，base 与 fixed 与常态相同，所以只在物品栏里看得见。 */
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

    private ItemModelBuilder loverAxeModel(String name, String texture) {
        ItemModelBuilder handheld = getBuilder(name + "_handheld")
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.fromNamespaceAndPath("malum", "item/handheld_large")))
                .texture("layer0", modLoc("item/" + texture + "_huge"));
        ItemModelBuilder inventory = withExistingParent(name + "_gui", "item/handheld")
                .texture("layer0", modLoc("item/" + texture));
        ItemModelBuilder model = withExistingParent(name, "item/handheld");
        model.customLoader(SeparateTransformsModelBuilder::begin).base(handheld)
                .perspective(ItemDisplayContext.GUI, inventory)
                .perspective(ItemDisplayContext.FIXED, inventory);
        return model;
    }

    private void registerSpawnEggModel(String name) {
        getBuilder(name).parent(new ModelFile.UncheckedModelFile(mcLoc("item/template_spawn_egg")));
    }

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

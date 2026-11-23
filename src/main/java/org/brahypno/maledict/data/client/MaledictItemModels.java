package org.brahypno.maledict.data.client;

import com.google.gson.JsonObject;
import net.minecraft.data.PackOutput;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.ItemLayerModelBuilder;
import net.neoforged.neoforge.client.model.generators.loaders.ObjModelBuilder;
import net.neoforged.neoforge.client.model.generators.loaders.SeparateTransformsModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.curio.enlightenment.AgeOfEnlightenmentItem;
import org.brahypno.maledict.common.item.curio.hangman.HangmanFoolItem;
import org.brahypno.maledict.common.item.weapon.incursus.IncursusBladeItem;
import org.brahypno.maledict.common.item.weapon.remembrance.SpiritArrowType;
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
        basicItem(MaledictItems.BLIGHTED_STAR.get());
        registerLightFragmentModel();
        basicItem(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get());
        basicItem(MaledictItems.LIGHTFALL_CODEX.get());
        basicItem(MaledictItems.LUMENFALL_BISMUTH_INGOT.get());
        basicItem(MaledictItems.REFRACTED_LEAD_BISMUTH_EUTECTIC.get());

        withExistingParent("raven_feather", mcLoc("item/generated"))
                .texture("layer0", modLoc("entity/raven_feather"));

        getBuilder("curio_return_token")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/curio_return_token"));

        getBuilder("soulbinding_scroll")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/soulbinding_scroll"));
        getBuilder("soulbinding_seal")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/soulbinding_seal"));

        basicItem(MaledictItems.AUFHEBEN_LIGHTWARD_HELMET.get());
        basicItem(MaledictItems.AUFHEBEN_LIGHTWARD_CHESTPLATE.get());
        basicItem(MaledictItems.AUFHEBEN_LIGHTWARD_LEGGINGS.get());
        basicItem(MaledictItems.AUFHEBEN_LIGHTWARD_BOOTS.get());

        withExistingParent("baron_ceremonial_staff", mcLoc("item/handheld"))
                .texture("layer0", modLoc("item/baron_ceremonial_staff"))
                .transforms()
                // Match the raised arm pose (BaronStaffArmPose) and align the grip just above the
                // sprite's midpoint with the fist.
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(15, -75, 45).translation(0, -4, 0).scale(1.4F).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(15, -75, -45).translation(0, -4, 0).scale(1.4F).end()
                // The sprite runs at 45 degrees. Tilt the raised head outward from the crosshair in either hand.
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(-10, -30, 35).translation(-1.5F, 0, -1.75F).scale(0.9F).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(-10, -30, -55).translation(-1.5F, 0, -1.75F).scale(0.9F).end()
                .end();

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

        basicItem(MaledictItems.EMPEROR_IN_THE_EGGSHELL.get());

        getBuilder("mnemonic_obelisk")
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.fromNamespaceAndPath("malum", "item/runewood_obelisk")))
                .texture("0", modLoc("block/mnemonic_obelisk"));
        getBuilder("soulwood_obelisk")
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.fromNamespaceAndPath("malum", "item/runewood_obelisk")))
                .texture("0", modLoc("block/runewood_obelisk"));

        // 方块模型的物品：1.20.1 显式写了七个 ItemDisplayContext 变换，port 丢了，于是手持与展示框里
        // 只是一个未变换的方块。数值逐条照抄参考分支。
        getBuilder("blighted_wither_skull")
                .parent(new ModelFile.UncheckedModelFile(modLoc("block/blighted_wither_skull")))
                .transforms()
                .transform(ItemDisplayContext.GUI).rotation(30, 225, 0).translation(0, 3, 0).scale(1).end()
                .transform(ItemDisplayContext.FIXED).rotation(0, 0, 0).translation(0, 4, 0).scale(1).end()
                .transform(ItemDisplayContext.GROUND).rotation(0, 180, 0).translation(0, 3, 0).scale(0.5F).end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(45, 225, 0).translation(0, 3, 0).scale(0.5F).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(45, 225, 0).translation(0, 3, 0).scale(0.5F).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 180, 0).scale(1).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 180, 0).scale(1).end()
                .end();

        registerSpawnEggModel("first_vicissitude_phase_one_spawn_egg");
        registerSpawnEggModel("raven_spawn_egg");
        registerSpawnEggModel("first_vicissitude_phase_two_spawn_egg");
        registerSpawnEggModel("weeping_wither_spawn_egg");
        registerSpawnEggModel("weeping_wither_skeleton_spawn_egg");
        registerSpawnEggModel("demiurge_statue_spawn_egg");
        registerSpawnEggModel("demiurge_statue_awakened_spawn_egg");

        registerMaidBondSwordModels();
    }

    /**
     * 女仆羁绊的两把剑。眷恋之剑沿用 1.20.1 的 3D OBJ（{@code forge:obj} + {@code forge:separate_transforms}），
     * 1.21 的对应物是 {@code neoforge:obj} + {@code neoforge:separate_transforms}，两个 loader 与
     * ObjModelBuilder 的方法都还在，所以模型可以逐项照搬而不是降级成 2D 图标。
     *
     * <p>display 变换也照 1.20.1 生成物抄下来：OBJ 模型没有 parent，不写这几项的话手持姿态是单位矩阵，
     * 剑会插在手里原地不动。
     */
    private void registerMaidBondSwordModels() {
        withExistingParent("forsaken_vow", mcLoc("item/handheld"))
                .texture("layer0", modLoc("item/forsaken_vow"));

        ItemModelBuilder held = getBuilder("yearning_sword_held")
                .guiLight(BlockModel.GuiLight.FRONT)
                .texture("surface", modLoc("item/yearning_sword_3d"))
                .texture("particle", "#surface");
        held.customLoader(ObjModelBuilder::begin)
            .modelLocation(modLoc("models/item/yearning_sword.obj"))
            .automaticCulling(false)
            .flipV(true)
            .emissiveAmbient(false);

        ItemModelBuilder.TransformsBuilder transforms = held.transforms();
        transforms.transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                 .rotation(0, -52, 10).translation(0, 6.5F, 0.6F).scale(0.92F).end();
        transforms.transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                 .rotation(0, 52, -10).translation(0, 6.5F, 0.6F).scale(0.92F).end();
        transforms.transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                 .rotation(-45, -52, -20).translation(1.5F, 3.65F, -4.38F).scale(0.95F).end();
        transforms.transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                 .rotation(-45, 52, 20).translation(1.5F, 3.65F, -4.38F).scale(0.95F).end();
        transforms.transform(ItemDisplayContext.HEAD)
                 .rotation(0, 180, 45).translation(0, 12F, 7F).scale(0.72F).end();
        transforms.transform(ItemDisplayContext.GROUND)
                 .rotation(0, 0, -60).translation(0, 3.6F, 0).scale(0.43F).end();
        transforms.transform(ItemDisplayContext.FIXED)
                 .rotation(0, 180, 45).translation(-1.9F, -1.5F, 0).scale(0.67F).end();
        transforms.end();

        // 物品栏里退回 2D 图标：3D 模型在 GUI 那一格里看不清剑身。只有 GUI 一档退回——
        // Fixed（展示框）在 1.20.1 用的是 3D 模型自己的 display 变换，照旧保留。
        ItemModelBuilder gui = withExistingParent("yearning_sword_gui", mcLoc("item/handheld"))
                .texture("layer0", modLoc("item/yearning_sword"));
        ItemModelBuilder sword = withExistingParent("yearning_sword", mcLoc("item/handheld"))
                .texture("particle", modLoc("item/yearning_sword"));
        sword.customLoader(SeparateTransformsModelBuilder::begin)
             .base(held)
             .perspective(ItemDisplayContext.GUI, gui);
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

    /**
     * 发光碎片：底层贴图 + 自发光覆盖层。{@code emissive} 给出 15/15 亮度，但 builder 没有逐层 AO
     * 开关，所以照 1.20.1 的做法在 {@code toJson} 里补一条 {@code ambient_occlusion=false}；
     * NeoForge 的键是 {@code neoforge_data}（1.20.1 时代是 {@code forge_data}）。
     */
    private void registerLightFragmentModel() {
        getBuilder("light_fragment")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("item/light_fragment"))
                .texture("layer1", modLoc("item/light_fragment_glow"))
                .customLoader((parent, helper) -> new ItemLayerModelBuilder<ItemModelBuilder>(parent, helper) {
                    @Override
                    public JsonObject toJson(JsonObject json) {
                        JsonObject model = super.toJson(json);
                        model.getAsJsonObject("neoforge_data").getAsJsonObject("layers")
                             .getAsJsonObject("1").addProperty("ambient_occlusion", false);
                        return model;
                    }
                })
                .emissive(15, 15, 1);
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

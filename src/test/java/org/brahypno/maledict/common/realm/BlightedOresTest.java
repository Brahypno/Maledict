package org.brahypno.maledict.common.realm;

import com.sammy.malum.common.recipe.FavorOfTheVoidRecipe;
import com.sammy.malum.registry.common.item.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.MaledictTags;
import org.brahypno.maledict.data.MaledictWorldgen;
import org.brahypno.maledict.registry.MaledictBlocks;
import org.brahypno.maledict.registry.MaledictItems;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class BlightedOresTest {

    @GameTest(template = "test_empty", batch = "blighted_realm")
    public static void realmOresDropThemselvesAndCraftIntoStorage(GameTestHelper helper) {
        verifyLeadOre(helper);
        verifyBismuthOre(helper);
        helper.succeed();
    }

    private static void verifyLeadOre(GameTestHelper helper) {
        var level = helper.getLevel();
        var pick = new ItemStack(Items.IRON_PICKAXE);
        var silk = pick.copy();
        silk.enchant(Enchantments.SILK_TOUCH, 1);
        var fortune = pick.copy();
        fortune.enchant(Enchantments.BLOCK_FORTUNE, 3);
        for (Block ore : new Block[]{MaledictBlocks.TAINTED_MALIGNANT_LEAD_ORE.get(), MaledictBlocks.TWISTED_MALIGNANT_LEAD_ORE.get()}) {
            var state = ore.defaultBlockState();
            helper.assertTrue(pick.isCorrectToolForDrops(state) && !new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(state),
                    "Both ores require an iron-tier pickaxe or better");
            var drops = Block.getDrops(state, level, helper.absolutePos(BlockPos.ZERO), null, null, pick);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get())
                            && drops.get(0).getCount() >= 3 && drops.get(0).getCount() <= 4,
                    "Either host drops three or four fragments like a Cthonic Gold Ore");
            var silkDrops = Block.getDrops(state, level, helper.absolutePos(BlockPos.ZERO), null, null, silk);
            helper.assertTrue(silkDrops.size() == 1 && silkDrops.get(0).is(ore.asItem()),
                    "Silk Touch preserves the corresponding host variant");
            var params = new LootParams.Builder(level).withParameter(LootContextParams.BLOCK_STATE, state)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                    .withParameter(LootContextParams.TOOL, fortune).create(LootContextParamSets.BLOCK);
            var table = level.getServer().getLootData().getLootTable(ore.getLootTable());
            for (long seed = 1; seed <= 16; seed++) {
                var fortunateDrops = table.getRandomItems(params, seed);
                helper.assertTrue(fortunateDrops.size() == 1 && fortunateDrops.get(0).is(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get())
                                && fortunateDrops.get(0).getCount() >= 3 && fortunateDrops.get(0).getCount() <= 4,
                        "Fortune III must not raise the Cthonic-Gold yield of three to four fragments");
            }
        }
        var recipe = (CraftingRecipe) level.getRecipeManager().byKey(id("malignant_lead_from_fragments")).orElseThrow();
        var grid = new TransientCraftingContainer(FakePlayerFactory.getMinecraft(level).inventoryMenu, 3, 3);
        for (int slot = 0; slot < 9; slot++) grid.setItem(slot, new ItemStack(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get()));
        helper.assertTrue(recipe.matches(grid, level), "Nine fragments craft without a required arrangement");
        var result = recipe.assemble(grid, level.registryAccess());
        helper.assertTrue(result.is(ItemRegistry.MALIGNANT_LEAD.get()) && result.getCount() == 1,
                "The result is one existing Malum malignant lead, not a second resource");
        grid.setItem(8, ItemStack.EMPTY);
        helper.assertTrue(!recipe.matches(grid, level), "Eight fragments must not craft a lead");
        var favor = (FavorOfTheVoidRecipe) level.getRecipeManager().byKey(id("void_favor/malignant_lead_fragment")).orElseThrow();
        helper.assertTrue(favor.doesInputMatch(new ItemStack(ItemRegistry.CTHONIC_GOLD_FRAGMENT.get())),
                "The weeping well swallows a Cthonic Gold Fragment");
        helper.assertTrue(favor.output.is(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get()) && favor.output.getCount() == 1,
                "The well returns one malignant lead fragment per cthonic gold fragment");
        helper.assertTrue(!favor.doesInputMatch(new ItemStack(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get())),
                "The well does not swallow the fragment it produces");
    }

    private static void verifyBismuthOre(GameTestHelper helper) {
        var level = helper.getLevel();
        Block ore = MaledictBlocks.LUMENFALL_BISMUTH_ORE.get();
        Block storage = MaledictBlocks.LUMENFALL_BISMUTH_BLOCK.get();
        var oreStack = new ItemStack(ore);
        var storageStack = new ItemStack(storage);
        var ingot = MaledictItems.LUMENFALL_BISMUTH_INGOT.get();
        var ingotStack = new ItemStack(ingot);
        var diamond = new ItemStack(Items.DIAMOND_PICKAXE);
        var iron = new ItemStack(Items.IRON_PICKAXE);

        helper.assertTrue(diamond.isCorrectToolForDrops(ore.defaultBlockState())
                        && !iron.isCorrectToolForDrops(ore.defaultBlockState())
                        && diamond.isCorrectToolForDrops(storage.defaultBlockState())
                        && !iron.isCorrectToolForDrops(storage.defaultBlockState()),
                "The deep ore and its storage block require a diamond pickaxe");
        var oreDrops = Block.getDrops(ore.defaultBlockState(), level, helper.absolutePos(BlockPos.ZERO), null, null, diamond);
        helper.assertTrue(oreDrops.size() == 1 && oreDrops.get(0).is(MaledictItems.LUMENFALL_BISMUTH_ORE.get())
                        && oreDrops.get(0).getCount() == 1,
                "The ore drops itself once, without Silk Touch");
        var storageDrops = Block.getDrops(storage.defaultBlockState(), level, helper.absolutePos(BlockPos.ZERO), null, null, diamond);
        helper.assertTrue(storageDrops.size() == 1 && storageDrops.get(0).is(MaledictItems.LUMENFALL_BISMUTH_BLOCK.get()),
                "The storage block drops itself");
        var fortune = diamond.copy();
        fortune.enchant(Enchantments.BLOCK_FORTUNE, 3);
        var params = new LootParams.Builder(level).withParameter(LootContextParams.BLOCK_STATE, ore.defaultBlockState())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .withParameter(LootContextParams.TOOL, fortune).create(LootContextParamSets.BLOCK);
        var table = level.getServer().getLootData().getLootTable(ore.getLootTable());
        for (long seed = 1; seed <= 16; seed++) {
            var drops = table.getRandomItems(params, seed);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(MaledictItems.LUMENFALL_BISMUTH_ORE.get())
                            && drops.get(0).getCount() == 1,
                    "Fortune must not multiply the ore's self drop");
        }

        helper.assertTrue(ore.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)
                        && ore.defaultBlockState().is(BlockTags.NEEDS_DIAMOND_TOOL)
                        && storage.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)
                        && storage.defaultBlockState().is(BlockTags.NEEDS_DIAMOND_TOOL),
                "Both blocks carry the vanilla pickaxe and diamond-tier tags");
        helper.assertTrue(ore.defaultBlockState().is(Tags.Blocks.ORES)
                        && storage.defaultBlockState().is(Tags.Blocks.STORAGE_BLOCKS)
                        && ore.defaultBlockState().is(MaledictTags.BLIGHTED_REALM_ORES)
                        && MaledictBlocks.TAINTED_MALIGNANT_LEAD_ORE.get().defaultBlockState()
                        .is(MaledictTags.BLIGHTED_REALM_ORES)
                        && MaledictBlocks.TWISTED_MALIGNANT_LEAD_ORE.get().defaultBlockState()
                        .is(MaledictTags.BLIGHTED_REALM_ORES),
                "Forge tags mark the ore and storage block, and all realm ores share a dedicated block tag");
        helper.assertTrue(oreStack.is(Tags.Items.ORES)
                        && storageStack.is(Tags.Items.STORAGE_BLOCKS)
                        && ingotStack.is(Tags.Items.INGOTS)
                        && oreStack.is(MaledictTags.BLIGHTED_REALM_ORE_ITEMS)
                        && new ItemStack(MaledictItems.TAINTED_MALIGNANT_LEAD_ORE.get())
                        .is(MaledictTags.BLIGHTED_REALM_ORE_ITEMS)
                        && new ItemStack(MaledictItems.TWISTED_MALIGNANT_LEAD_ORE.get())
                        .is(MaledictTags.BLIGHTED_REALM_ORE_ITEMS),
                "The item forms carry the matching Forge tags and the realm-ore item tag");

        var recipes = level.getRecipeManager();
        var smelting = (AbstractCookingRecipe) recipes.byKey(
                id("lumenfall_bismuth_ingot_from_smelting_lumenfall_bismuth_ore")).orElseThrow();
        helper.assertTrue(smelting.getCookingTime() == 200 && smelting.getExperience() == 2.0F
                        && smelting.getResultItem(level.registryAccess()).is(ingot),
                "A furnace turns one ore block into one ingot in 200 ticks");
        var blasting = (AbstractCookingRecipe) recipes.byKey(
                id("lumenfall_bismuth_ingot_from_blasting_lumenfall_bismuth_ore")).orElseThrow();
        helper.assertTrue(blasting.getCookingTime() == 100 && blasting.getExperience() == 2.0F
                        && blasting.getResultItem(level.registryAccess()).is(ingot),
                "A blast furnace halves the time without changing the yield");

        var grid = new TransientCraftingContainer(FakePlayerFactory.getMinecraft(level).inventoryMenu, 3, 3);
        for (int slot = 0; slot < 9; slot++) grid.setItem(slot, ingotStack.copy());
        var storageRecipe = (CraftingRecipe) recipes.byKey(id("lumenfall_bismuth_block")).orElseThrow();
        var storageResult = storageRecipe.assemble(grid, level.registryAccess());
        helper.assertTrue(storageRecipe.matches(grid, level)
                        && storageResult.is(MaledictItems.LUMENFALL_BISMUTH_BLOCK.get()) && storageResult.getCount() == 1,
                "Nine ingots craft one storage block");
        grid.setItem(8, ItemStack.EMPTY);
        helper.assertTrue(!storageRecipe.matches(grid, level), "Eight ingots must not craft the storage block");
        grid.clearContent();
        grid.setItem(0, storageStack.copy());
        var ingotRecipe = (CraftingRecipe) recipes.byKey(id("lumenfall_bismuth_ingot_from_block")).orElseThrow();
        var ingotResult = ingotRecipe.assemble(grid, level.registryAccess());
        helper.assertTrue(ingotRecipe.matches(grid, level) && ingotResult.is(ingot) && ingotResult.getCount() == 9,
                "One storage block unpacks into nine ingots");
        grid.setItem(0, ItemStack.EMPTY);
        helper.assertTrue(!ingotRecipe.matches(grid, level), "An empty grid must not yield ingots");

        ServerLevel realm = level.getServer().getLevel(BlightedRealm.LEVEL);
        helper.assertTrue(realm != null, "The blighted realm loads on the game-test server");
        var settings = realm.getBiome(BlockPos.ZERO).value().getGenerationSettings();
        helper.assertTrue(settings.features().stream().flatMap(set -> set.stream())
                        .anyMatch(placed -> placed.is(MaledictWorldgen.PLACED_LUMENFALL_BISMUTH_ORE_LARGE))
                        && settings.features().stream().flatMap(set -> set.stream())
                        .anyMatch(placed -> placed.is(MaledictWorldgen.PLACED_LUMENFALL_BISMUTH_ORE_SMALL)),
                "Both deep ore veins are connected to natural biome generation");
        var configured = realm.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        var large = configured.getOrThrow(MaledictWorldgen.LUMENFALL_BISMUTH_ORE_LARGE);
        var largeConfig = (OreConfiguration) large.config();
        helper.assertTrue(large.feature() == Feature.SCATTERED_ORE && largeConfig.size == 3
                        && largeConfig.discardChanceOnAirExposure == 1.0F
                        && largeConfig.targetStates.size() == 1
                        && largeConfig.targetStates.get(0).state.is(ore),
                "The large vein copies ancient debris: three blocks, air exposure always discarded, twisted rock only");
        var small = configured.getOrThrow(MaledictWorldgen.LUMENFALL_BISMUTH_ORE_SMALL);
        var smallConfig = (OreConfiguration) small.config();
        helper.assertTrue(small.feature() == Feature.SCATTERED_ORE && smallConfig.size == 2
                        && smallConfig.discardChanceOnAirExposure == 1.0F
                        && smallConfig.targetStates.size() == 1
                        && smallConfig.targetStates.get(0).state.is(ore),
                "The small vein copies ancient debris: two blocks, air exposure always discarded, twisted rock only");
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("maledict", path);
    }
}

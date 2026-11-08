package org.brahypno.maledict.common.realm;

import com.sammy.malum.common.recipe.FavorOfTheVoidRecipe;
import com.sammy.malum.registry.common.item.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.registry.MaledictBlocks;
import org.brahypno.maledict.registry.MaledictItems;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class MalignantLeadOreTest {

    @GameTest(template = "test_empty", batch = "blighted_realm")
    public static void eitherHostDropsFragmentsAndNineCraftOneLead(GameTestHelper helper) {
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
        var recipe = (CraftingRecipe) level.getRecipeManager().byKey(
                ResourceLocation.fromNamespaceAndPath("maledict", "malignant_lead_from_fragments")).orElseThrow();
        var grid = new TransientCraftingContainer(FakePlayerFactory.getMinecraft(level).inventoryMenu, 3, 3);
        for (int slot = 0; slot < 9; slot++) grid.setItem(slot, new ItemStack(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get()));
        helper.assertTrue(recipe.matches(grid, level), "Nine fragments craft without a required arrangement");
        var result = recipe.assemble(grid, level.registryAccess());
        helper.assertTrue(result.is(ItemRegistry.MALIGNANT_LEAD.get()) && result.getCount() == 1,
                "The result is one existing Malum malignant lead, not a second resource");
        grid.setItem(8, ItemStack.EMPTY);
        helper.assertTrue(!recipe.matches(grid, level), "Eight fragments must not craft a lead");
        var favor = (FavorOfTheVoidRecipe) level.getRecipeManager().byKey(
                ResourceLocation.fromNamespaceAndPath("maledict", "void_favor/malignant_lead_fragment")).orElseThrow();
        helper.assertTrue(favor.doesInputMatch(new ItemStack(ItemRegistry.CTHONIC_GOLD_FRAGMENT.get())),
                "The weeping well swallows a Cthonic Gold Fragment");
        helper.assertTrue(favor.output.is(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get()) && favor.output.getCount() == 1,
                "The well returns one malignant lead fragment per cthonic gold fragment");
        helper.assertTrue(!favor.doesInputMatch(new ItemStack(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get())),
                "The well does not swallow the fragment it produces");
        helper.succeed();
    }
}

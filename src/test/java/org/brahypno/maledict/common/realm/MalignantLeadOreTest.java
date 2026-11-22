package org.brahypno.maledict.common.realm;

import com.sammy.malum.registry.common.item.MalumItems;
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
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
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
        silk.enchant(org.brahypno.maledict.TestSupport.enchantment(Enchantments.SILK_TOUCH), 1);
        var fortune = pick.copy();
        fortune.enchant(org.brahypno.maledict.TestSupport.enchantment(Enchantments.FORTUNE), 3);
        for (Block ore : new Block[]{MaledictBlocks.TAINTED_MALIGNANT_LEAD_ORE.get(), MaledictBlocks.TWISTED_MALIGNANT_LEAD_ORE.get()}) {
            var state = ore.defaultBlockState();
            helper.assertTrue(pick.isCorrectToolForDrops(state) && !new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(state),
                    "Both ores require an iron-tier pickaxe or better");
            var drops = Block.getDrops(state, level, helper.absolutePos(BlockPos.ZERO), null, null, pick);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get())
                            && drops.get(0).getCount() >= 3 && drops.get(0).getCount() <= 4,
                    "Either host drops a uniform 3-4 fragments, the 1.20.1 yield, got "
                            + (drops.isEmpty() ? "nothing" : drops.get(0).getCount()));
            var silkDrops = Block.getDrops(state, level, helper.absolutePos(BlockPos.ZERO), null, null, silk);
            helper.assertTrue(silkDrops.size() == 1 && silkDrops.get(0).is(ore.asItem()),
                    "Silk Touch preserves the corresponding host variant");
            var params = new LootParams.Builder(level).withParameter(LootContextParams.BLOCK_STATE, state)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                    .withParameter(LootContextParams.TOOL, fortune).create(LootContextParamSets.BLOCK);
            var table = level.getServer().reloadableRegistries().getLootTable(ore.getLootTable());
            for (long seed = 1; seed <= 16; seed++) {
                var fortunateDrops = table.getRandomItems(params, seed);
                // 1.20.1 的分支没有附魔加成函数：时运也只是同一种碎片、同样 3-4 个。
                helper.assertTrue(fortunateDrops.size() == 1
                                && fortunateDrops.get(0).is(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get())
                                && fortunateDrops.get(0).getCount() >= 3 && fortunateDrops.get(0).getCount() <= 4,
                        "Fortune must keep the fragment type and the 3-4 yield the reference table has");
            }
        }
        var recipe = (CraftingRecipe) level.getRecipeManager().byKey(
                ResourceLocation.fromNamespaceAndPath("maledict", "malignant_lead_from_fragments")).orElseThrow().value();
        var grid = new TransientCraftingContainer(FakePlayerFactory.getMinecraft(level).inventoryMenu, 3, 3);
        for (int slot = 0; slot < 8; slot++) grid.setItem(slot, new ItemStack(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get()));
        helper.assertTrue(!recipe.matches(grid.asCraftInput(), level), "Eight fragments must not craft a lead");
        grid.setItem(8, new ItemStack(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get()));
        helper.assertTrue(recipe.matches(grid.asCraftInput(), level),
                "A full ring of nine fragments crafts without a required arrangement");
        var result = recipe.assemble(grid.asCraftInput(), level.registryAccess());
        helper.assertTrue(result.is(MalumItems.MALIGNANT_LEAD.get()) && result.getCount() == 1,
                "The result is one existing Malum malignant lead, not a second resource");
        helper.succeed();
    }
}

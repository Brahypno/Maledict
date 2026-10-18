package org.brahypno.maledict.common.realm;

import com.sammy.malum.registry.common.block.MalumBlocks;
import com.sammy.malum.registry.common.item.MalumItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.data.MaledictWorldgen;
import org.brahypno.maledict.registry.MaledictBlocks;
import org.brahypno.maledict.registry.MaledictItems;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class MalignantLeadOreTest {
    @GameTest(template = "test_empty", batch = "blighted_realm", timeoutTicks = 300)
    public static void goldBecomesLeadOnlyInTheRealm(GameTestHelper helper) {
        var realm = helper.getLevel().getServer().getLevel(BlightedRealm.LEVEL);
        var biome = realm.registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(BlightedRealm.WASTES);
        var oreFeatures = biome.getGenerationSettings().features().stream().flatMap(HolderSet::stream).toList();
        helper.assertTrue(oreFeatures.stream().noneMatch(f -> f.is(OrePlacements.ORE_GOLD) || f.is(OrePlacements.ORE_GOLD_LOWER)),
                "The realm must not retain either vanilla gold placement");
        helper.assertTrue(oreFeatures.stream().anyMatch(f -> f.is(MaledictWorldgen.PLACED_LEAD_ORE))
                        && oreFeatures.stream().anyMatch(f -> f.is(MaledictWorldgen.PLACED_LEAD_ORE_LOWER)),
                "Both gold distributions must be replaced with malignant lead");
        var plains = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME)
                .getOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);
        helper.assertTrue(plains.getGenerationSettings().features().stream().flatMap(HolderSet::stream)
                        .anyMatch(f -> f.is(OrePlacements.ORE_GOLD)), "Overworld gold generation must remain intact");

        var configured = realm.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(MaledictWorldgen.LEAD_ORE);
        var config = (OreConfiguration) configured.config();
        helper.assertTrue(config.size == 9 && config.discardChanceOnAirExposure == 0.5F,
                "The replacement retains vanilla buried-gold size and air-exposure restrictions");
        for (boolean deep : new boolean[]{false, true}) {
            BlockPos center = new BlockPos(4600, deep ? -24 : 24, 4600);
            var host = deep ? MalumBlocks.TWISTED_ROCK.get() : MalumBlocks.TAINTED_ROCK.get();
            var ore = deep ? MaledictBlocks.TWISTED_MALIGNANT_LEAD_ORE.get() : MaledictBlocks.TAINTED_MALIGNANT_LEAD_ORE.get();
            for (var pos : BlockPos.betweenClosed(center.offset(-5, -5, -5), center.offset(5, 5, 5))) {
                realm.setBlockAndUpdate(pos, host.defaultBlockState());
            }
            configured.place(realm, realm.getChunkSource().getGenerator(), RandomSource.create(73), center);
            int count = 0;
            for (var pos : BlockPos.betweenClosed(center.offset(-5, -5, -5), center.offset(5, 5, 5))) {
                if (realm.getBlockState(pos).is(ore)) count++;
            }
            helper.assertTrue(count > 0, "The registered ore feature must replace its " + (deep ? "twisted" : "tainted") + " host");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "blighted_realm")
    public static void eitherHostDropsFragmentsAndEightCraftOneLead(GameTestHelper helper) {
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
                            && drops.get(0).getCount() == 1, "Either host drops exactly one fragment without Fortune");
            var silkDrops = Block.getDrops(state, level, helper.absolutePos(BlockPos.ZERO), null, null, silk);
            helper.assertTrue(silkDrops.size() == 1 && silkDrops.get(0).is(ore.asItem()),
                    "Silk Touch preserves the corresponding host variant");
            var params = new LootParams.Builder(level).withParameter(LootContextParams.BLOCK_STATE, state)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                    .withParameter(LootContextParams.TOOL, fortune).create(LootContextParamSets.BLOCK);
            var table = level.getServer().reloadableRegistries().getLootTable(ore.getLootTable());
            int max = 0;
            for (long seed = 1; seed <= 16; seed++) {
                var fortunateDrops = table.getRandomItems(params, seed);
                helper.assertTrue(fortunateDrops.size() == 1 && fortunateDrops.get(0).is(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get())
                                && fortunateDrops.get(0).getCount() >= 1 && fortunateDrops.get(0).getCount() <= 4,
                        "Fortune III must retain the same fragment type with vanilla ore yield bounds");
                max = Math.max(max, fortunateDrops.get(0).getCount());
            }
            helper.assertTrue(max > 1, "Fortune must be able to increase the fragment yield");
        }
        var recipe = (CraftingRecipe) level.getRecipeManager().byKey(
                ResourceLocation.fromNamespaceAndPath("maledict", "malignant_lead_from_fragments")).orElseThrow().value();
        var grid = new TransientCraftingContainer(FakePlayerFactory.getMinecraft(level).inventoryMenu, 3, 3);
        for (int slot = 0; slot < 8; slot++) grid.setItem(slot, new ItemStack(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get()));
        helper.assertTrue(recipe.matches(grid.asCraftInput(), level), "Eight fragments craft without a required arrangement");
        var result = recipe.assemble(grid.asCraftInput(), level.registryAccess());
        helper.assertTrue(result.is(MalumItems.MALIGNANT_LEAD.get()) && result.getCount() == 1,
                "The result is one existing Malum malignant lead, not a second resource");
        grid.setItem(7, ItemStack.EMPTY);
        helper.assertTrue(!recipe.matches(grid.asCraftInput(), level), "Seven fragments must not craft a lead");
        helper.succeed();
    }
}

package org.brahypno.maledict.common.realm;

import com.sammy.malum.registry.common.block.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.data.MaledictWorldgen;
import org.brahypno.maledict.registry.MaledictBlocks;
import org.brahypno.maledict.registry.MaledictItems;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class BlightedDetailsTest {

    // Replaces the former forest-floor smoke case: both suites remain at 50 cases.
    @GameTest(template = "test_empty", batch = "blighted_details", timeoutTicks = 400)
    public static void wellStationHasAnAccessibleStoreroomAndUsableLightingSupplies(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(BlightedRealm.LEVEL);
        helper.assertTrue(level != null, "The realm loads on the game-test server");
        BlockPos center = new BlockPos(24150, 280, 24000);
        var feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(MaledictWorldgen.WELL_STATION);
        var settings = level.getBiome(center).value().getGenerationSettings();
        helper.assertTrue(settings.features().stream().flatMap(set -> set.stream())
                        .anyMatch(placed -> placed.is(MaledictWorldgen.PLACED_WELL_STATION)),
                "The station is connected to natural biome generation");
        prepare(level, center);
        BlockPos occupied = center.offset(7, -11, 5);
        level.setBlockAndUpdate(occupied, Blocks.CHEST.defaultBlockState());
        helper.assertTrue(!feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(8), center)
                        && level.getBlockState(center).isAir() && level.getBlockState(center.below()).is(BlockRegistry.TAINTED_ROCK.get())
                        && level.getBlockEntity(occupied) instanceof ChestBlockEntity,
                "A rejected station preserves the container and leaves no partial construction");
        level.setBlockAndUpdate(occupied, BlockRegistry.TAINTED_ROCK.get().defaultBlockState());
        helper.assertTrue(feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(8), center),
                "A configured station places on supported dry terrain");
        Direction facing = null;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            var ladder = level.getBlockState(center.relative(direction.getOpposite()));
            if (ladder.is(Blocks.LADDER)) facing = ladder.getValue(LadderBlock.FACING);
        }
        helper.assertTrue(facing != null, "The dry well has a ladder at its mouth");
        for (int y = -12; y <= 0; y++) {
            var rung = center.relative(facing.getOpposite()).above(y);
            helper.assertTrue(level.getBlockState(rung).is(Blocks.LADDER)
                            && level.getBlockState(rung).canSurvive(level, rung),
                    "Every ladder segment has a backing block, from the room floor to the rim");
        }
        for (int z = 0; z <= 3; z++) checkWalkable(helper, level, at(center, facing, 0, -12, z));
        for (int x = 0; x <= 5; x++) checkWalkable(helper, level, at(center, facing, x, -12, 0));
        helper.assertTrue(level.getBlockState(at(center, facing, -3, -4, 3)).is(BlockRegistry.TAINTED_ROCK.get()),
                "The room moves down rather than leaving an upper chamber beside the longer shaft");
        var chest = (ChestBlockEntity) level.getBlockEntity(at(center, facing, 5, -12, 2));
        helper.assertTrue(chest != null, "The reachable side room contains a supply chest");
        chest.unpackLootTable(null);
        int fragments = 0;
        ItemStack notes = ItemStack.EMPTY;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            var stack = chest.getItem(slot);
            if (stack.is(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get())) fragments += stack.getCount();
            if (stack.is(Items.WRITTEN_BOOK)) notes = stack;
        }
        helper.assertTrue(fragments >= 3 && fragments <= 7 && notes.hasTag()
                        && notes.getTag().getList("pages", 8).size() == 4,
                "Real station loot includes lead fragments and all four bilingual note pages");

        var grid = new TransientCraftingContainer(FakePlayerFactory.getMinecraft(level).inventoryMenu, 2, 2);
        grid.setItem(0, new ItemStack(MaledictItems.FALLEN_SOULWOOD_BRANCHES.get()));
        var sticks = recipe(level, "sticks_from_fallen_branches");
        helper.assertTrue(sticks.matches(grid, level) && sticks.assemble(grid, level.registryAccess()).is(Items.STICK)
                        && sticks.assemble(grid, level.registryAccess()).getCount() == 2,
                "One collected branch crafts two sticks in the inventory grid");
        grid.setItem(3, new ItemStack(MaledictItems.WITHERLIGHT_MUSHROOM.get()));
        var lampRecipe = recipe(level, "witherlight_lamp");
        helper.assertTrue(lampRecipe.matches(grid, level)
                        && lampRecipe.assemble(grid, level.registryAccess()).is(MaledictItems.WITHERLIGHT_LAMP.get()),
                "A branch and a mushroom craft a lamp without a required arrangement");
        grid.setItem(0, ItemStack.EMPTY);
        helper.assertTrue(!lampRecipe.matches(grid, level), "A mushroom alone must not craft a lamp");
        var lampPos = at(center, facing, -3, 0, -3);
        var lamp = level.getBlockState(lampPos);
        helper.assertTrue(lamp.is(MaledictBlocks.WITHERLIGHT_LAMP.get()) && lamp.getLightEmission(level, lampPos) == 14,
                "The station uses the same collectible, bright branch lamp as the recipe");
        var drops = Block.getDrops(lamp, level, lampPos, null);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(MaledictItems.WITHERLIGHT_LAMP.get()),
                "Breaking a branch lamp returns the lamp itself");
        level.destroyBlock(lampPos.below(), false);
        helper.assertTrue(level.getBlockState(lampPos).isAir(), "The lamp drops away when its support is removed");

        var branches = new ItemStack(MaledictItems.FALLEN_SOULWOOD_BRANCHES.get(), 2);
        helper.assertTrue(ForgeHooks.getBurnTime(branches, RecipeType.SMELTING) == ForgeHooks.getBurnTime(new ItemStack(Items.STICK), RecipeType.SMELTING),
                "A branch has exactly the fuel value of one vanilla stick");
        var furnacePos = helper.absolutePos(new BlockPos(1, 1, 1));
        var furnaceLevel = helper.getLevel();
        furnaceLevel.setBlockAndUpdate(furnacePos, Blocks.FURNACE.defaultBlockState());
        var furnace = (FurnaceBlockEntity) furnaceLevel.getBlockEntity(furnacePos);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, branches);
        for (int tick = 0; tick < 200; tick++) {
            FurnaceBlockEntity.serverTick(furnaceLevel, furnacePos, furnaceLevel.getBlockState(furnacePos), furnace);
        }
        helper.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT) && furnace.getItem(1).isEmpty(),
                "A real furnace consumes two branches to smelt one iron ingot");
        helper.succeed();
    }

    private static CraftingRecipe recipe(ServerLevel level, String path) {
        return (CraftingRecipe) level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("maledict", path)).orElseThrow();
    }

    private static void checkWalkable(GameTestHelper helper, ServerLevel level, BlockPos feet) {
        helper.assertTrue(level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
                        && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP),
                "The ladder, chamber and storage passage connect with standing headroom at " + feet);
    }

    private static BlockPos at(BlockPos center, Direction facing, int x, int y, int z) {
        return center.relative(facing.getClockWise(), x).relative(facing, z).above(y);
    }

    private static void prepare(ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-9, -15, -9), center.offset(9, 7, 9))) {
            level.setBlockAndUpdate(pos, pos.getY() < center.getY()
                    ? BlockRegistry.TAINTED_ROCK.get().defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
    }
}

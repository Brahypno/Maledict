package org.brahypno.maledict.common.entity.vicissitude.rite;

import org.junit.jupiter.api.Test;
import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.common.block.curiosities.totem.TotemPoleBlockEntity;
import com.sammy.malum.registry.common.magic.MalumSpiritTypes;
import com.sammy.malum.registry.common.block.MalumBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.registry.MaledictEntities;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public class VicissitudeSummoningTest {

    @Test
    void everyRecipeConsumesItsBaseBeforeArrivalEvenWhenSummoningTicksAreSkipped() {
        for (int layers = 4; layers <= 6; layers++) {
            int previous = 0;
            for (int tick : new int[]{0, 12, 31, 67, 90, 130, 160, 10000}) {
                int consumed = VicissitudeSummoning.totemLayersConsumed(tick, layers);
                assertTrue(consumed >= previous && consumed <= layers);
                previous = consumed;
            }
            assertEquals(layers, VicissitudeSummoning.totemLayersConsumed(90, layers));
        }
    }

    @GameTest(template = "test_empty", batch = "rite_clearance")
    public static void fullEntranceRequiresClearCeilingWingsAndRift(GameTestHelper helper) {
        var base = totem(helper);
        BlockPos pos = base.getBlockPos();
        var type = MaledictEntities.FIRST_VICISSITUDE.get();
        helper.assertTrue(pos.above(3).equals(SummoningRite.findLanding(helper.getLevel(), pos, type)),
                "Open terrain must allow the original preferred landing, ignoring consumed poles/logs");
        var space = SummoningRite.clearance(pos.above(3), type);
        helper.assertTrue(space.getXsize() >= 9 && space.getYsize() >= 14.5 && space.minY < pos.getY() + 3,
                "Clearance must include the full nine-block entrance and the visual wings");
        for (BlockPos obstacle : List.of(pos.above(10), pos.offset(4, 9, 0), pos.offset(4, 12, 0))) {
            helper.getLevel().setBlockAndUpdate(obstacle, Blocks.STONE.defaultBlockState());
            helper.assertTrue(SummoningRite.findLanding(helper.getLevel(), pos, type) == null,
                    "Ceiling, wing or rift obstruction must reject every landing, never shorten the animation");
            helper.getLevel().setBlockAndUpdate(obstacle, Blocks.AIR.defaultBlockState());
        }
        BlockPos water = pos.offset(4, 12, 0);
        helper.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        helper.assertTrue(!SummoningRite.hasSummoningSpace(helper.getLevel(), pos),
                "Non-solid fluid must not obscure the animation");
        helper.getLevel().setBlockAndUpdate(water, Blocks.AIR.defaultBlockState());
        helper.assertTrue(SummoningRite.hasSummoningSpace(helper.getLevel(), pos),
                "Removing the obstruction must restore eligibility");
        helper.succeed();
    }

    private static TotemBaseBlockEntity totem(GameTestHelper helper) {
        // Above other test arenas, in their already loaded chunks.
        BlockPos pos = helper.absolutePos(new BlockPos(2, 129, 2));
        for (BlockPos clear : BlockPos.betweenClosed(pos.offset(-5, 1, -5), pos.offset(5, 24, 5))) {
            helper.getLevel().setBlockAndUpdate(clear, Blocks.AIR.defaultBlockState());
        }
        helper.getLevel().setBlockAndUpdate(pos, MalumBlocks.RUNEWOOD_TOTEM_BASE.get().defaultBlockState());
        var base = (TotemBaseBlockEntity) helper.getLevel().getBlockEntity(pos);
        base.setState(helper.getLevel(), TotemBaseBlockEntity.TotemBaseState.INACTIVE);
        for (int i = 1; i <= 5; i++) {
            helper.getLevel().setBlockAndUpdate(pos.above(i), com.sammy.malum.core.systems.spirit.SpiritTypeProperty.setSpiritType(
                    MalumBlocks.RUNEWOOD_TOTEM_POLE.get().defaultBlockState(),
                    (i <= 2 ? MalumSpiritTypes.ELDRITCH_SPIRIT : MalumSpiritTypes.ARCANE_SPIRIT).get()));
        }
        for (int i = 6; i <= 7; i++) {
            helper.getLevel().setBlockAndUpdate(pos.above(i), MalumBlocks.RUNEWOOD_LOG.get().defaultBlockState());
        }
        return base;
    }

    private static void click(GameTestHelper helper, FakePlayer player, TotemBaseBlockEntity base) {
        var hit = new BlockHitResult(Vec3.atCenterOf(base.getBlockPos()), Direction.NORTH, base.getBlockPos(), false);
        player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
    }

    private static void idle(GameTestHelper helper, TotemBaseBlockEntity base) {
        helper.assertTrue(!base.isActiveOrAssembling() && base.getTotemHeight() == 0,
                "Rejected/warning interaction must not start animation");
        for (int i = 1; i <= 5; i++) {
            helper.assertTrue(helper.getLevel().getBlockEntity(base.getBlockPos().above(i)) instanceof TotemPoleBlockEntity,
                    "Rejected/warning interaction must not consume ritual materials");
        }
    }
}

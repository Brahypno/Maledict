package org.brahypno.maledict.common.vicissitude.rite;

import org.junit.jupiter.api.Test;
import com.mojang.authlib.GameProfile;
import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.common.block.curiosities.totem.TotemPoleBlockEntity;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import com.sammy.malum.registry.common.block.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.registry.MaledictEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public class VicissitudeSummoningTest {
    @Test
    void threePolesDisappearTopDownBeforeTheBaseWithoutRepeatingLayers() {
        int[] ticks = {0, 9, 10, 24, 25, 39, 40, 54, 55, 130, 160};
        int[] expected = {0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 4};
        for (int i = 0; i < ticks.length; i++) {
            assertEquals(expected[i], VicissitudeSummoning.totemLayersConsumed(ticks[i], 4));
        }
    }

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

    @Test
    void wingsFinishOpeningAndHandOverToTheUnfoldedIdlePose() {
        assertEquals(1, VicissitudeSummoning.wingFold(85));
        float previous = 1;
        for (float tick = 85; tick <= VicissitudeSummoning.ARRIVAL_TICK; tick += .25F) {
            float fold = VicissitudeSummoning.wingFold(tick);
            assertTrue(fold >= 0 && fold <= previous);
            previous = fold;
        }
        for (int tick : new int[]{VicissitudeSummoning.ARRIVAL_TICK, 159, 160, 10000}) {
            assertEquals(0, VicissitudeSummoning.wingFold(tick));
        }
    }

    @Test
    void bossWaitsForTheOpeningAndArrivesBeforeTheRiftCloses() {
        for (int tick = 0; tick <= VicissitudeSummoning.OPEN_TICKS; tick++) {
            assertEquals(0, VicissitudeSummoning.descent(tick));
        }
        assertEquals(1, VicissitudeSummoning.aperture(VicissitudeSummoning.OPEN_TICKS));
        for (int tick = VicissitudeSummoning.OPEN_TICKS; tick <= VicissitudeSummoning.ARRIVAL_TICK; tick++) {
            assertEquals(1, VicissitudeSummoning.aperture(tick));
        }
        assertEquals(1, VicissitudeSummoning.descent(VicissitudeSummoning.ARRIVAL_TICK));
        // Include skipped-tick progression and the closing/color handoff in this timing scenario.
        float previous = 0;
        for (float tick = -20; tick <= 200; tick += 0.37F) {
            float descent = VicissitudeSummoning.descent(tick);
            assertTrue(descent >= previous && descent >= 0 && descent <= 1);
            previous = descent;
        }
        assertEquals(0, VicissitudeSummoning.grayscale(0));
        assertEquals(1, VicissitudeSummoning.grayscale(VicissitudeSummoning.OPEN_TICKS));
        assertTrue(VicissitudeSummoning.grayscale(145) > 0);
        assertTrue(VicissitudeSummoning.grayscale(145) < 1);
        for (int tick : new int[]{VicissitudeSummoning.DURATION, 180, 10000}) {
            assertEquals(0, VicissitudeSummoning.grayscale(tick));
            assertEquals(0, VicissitudeSummoning.aperture(tick));
            assertEquals(1, VicissitudeSummoning.descent(tick));
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

    @GameTest(template = "test_empty", batch = "rite_clearance")
    public static void crampedClicksRejectBeforeConsumptionAndClearConfirmation(GameTestHelper helper) {
        var base = totem(helper);
        List<String> messages = new ArrayList<>();
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "RiteSpace")) {
            @Override public void displayClientMessage(Component message, boolean overlay) {
                helper.assertTrue(overlay, "Rite feedback belongs on screen, not in chat");
                if (message.getContents() instanceof TranslatableContents translation) messages.add(translation.getKey());
            }
        };
        BlockPos ceiling = base.getBlockPos().above(10);
        helper.getLevel().setBlockAndUpdate(ceiling, Blocks.STONE.defaultBlockState());
        click(helper, player, base);
        idle(helper, base);
        helper.assertTrue(messages.get(0).endsWith(".cramped"), "First cramped click must reject immediately");
        helper.getLevel().setBlockAndUpdate(ceiling, Blocks.AIR.defaultBlockState());
        click(helper, player, base);
        idle(helper, base);
        helper.assertTrue(messages.get(1).endsWith(".warning"), "Cleared space must require a first warning");
        helper.runAfterDelay(2, () -> {
            helper.getLevel().setBlockAndUpdate(ceiling, Blocks.STONE.defaultBlockState());
            click(helper, player, base);
            idle(helper, base);
            helper.assertTrue(messages.get(2).endsWith(".cramped"), "Confirmation must recheck changed terrain");
            helper.getLevel().setBlockAndUpdate(ceiling, Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(2, () -> {
                click(helper, player, base);
                idle(helper, base);
                helper.assertTrue(messages.get(3).endsWith(".warning"), "Rejection must clear the old confirmation");
                helper.runAfterDelay(2, () -> {
                    click(helper, player, base);
                    helper.assertTrue(base.isActiveOrAssembling(), "A fresh second click in open space must start");
                    base.setState(TotemBaseBlockEntity.TotemRiteState.IDLE);
                    helper.succeed();
                });
            });
        });
    }

    private static TotemBaseBlockEntity totem(GameTestHelper helper) {
        // Above other test arenas, in their already loaded chunks.
        BlockPos pos = helper.absolutePos(new BlockPos(2, 129, 2));
        for (BlockPos clear : BlockPos.betweenClosed(pos.offset(-5, 1, -5), pos.offset(5, 24, 5))) {
            helper.getLevel().setBlockAndUpdate(clear, Blocks.AIR.defaultBlockState());
        }
        helper.getLevel().setBlockAndUpdate(pos, BlockRegistry.RUNEWOOD_TOTEM_BASE.get().defaultBlockState());
        var base = (TotemBaseBlockEntity) helper.getLevel().getBlockEntity(pos);
        base.setState(TotemBaseBlockEntity.TotemRiteState.IDLE);
        for (int i = 1; i <= 5; i++) {
            helper.getLevel().setBlockAndUpdate(pos.above(i), BlockRegistry.RUNEWOOD_TOTEM_POLE.get().defaultBlockState());
            ((TotemPoleBlockEntity) helper.getLevel().getBlockEntity(pos.above(i))).type =
                    i <= 2 ? SpiritTypeRegistry.ELDRITCH_SPIRIT : SpiritTypeRegistry.ARCANE_SPIRIT;
        }
        for (int i = 6; i <= 7; i++) {
            helper.getLevel().setBlockAndUpdate(pos.above(i), BlockRegistry.RUNEWOOD_LOG.get().defaultBlockState());
        }
        return base;
    }

    private static void click(GameTestHelper helper, FakePlayer player, TotemBaseBlockEntity base) {
        var hit = new BlockHitResult(Vec3.atCenterOf(base.getBlockPos()), Direction.NORTH, base.getBlockPos(), false);
        player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
    }

    private static void idle(GameTestHelper helper, TotemBaseBlockEntity base) {
        helper.assertTrue(!base.isActiveOrAssembling() && base.totemPolePositions.isEmpty(),
                "Rejected/warning interaction must not start animation");
        for (int i = 1; i <= 5; i++) {
            helper.assertTrue(helper.getLevel().getBlockEntity(base.getBlockPos().above(i)) instanceof TotemPoleBlockEntity,
                    "Rejected/warning interaction must not consume ritual materials");
        }
    }
}

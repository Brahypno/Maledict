package org.brahypno.maledict.common.vicissitude.rite;

import org.junit.jupiter.api.Test;
import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.common.block.curiosities.totem.TotemPoleBlockEntity;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import com.sammy.malum.registry.common.block.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.registry.MaledictEntities;
import org.brahypno.maledict.registry.MaledictItems;
import org.brahypno.maledict.common.vicissitude.FirstVicissitudeBossEntity;
import java.util.List;
import java.util.UUID;

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
        var level = helper.getLevel();
        Difficulty originalDifficulty = level.getDifficulty();
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "PeacefulSummon"));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        try {
            level.getServer().getWorldData().setDifficulty(Difficulty.PEACEFUL);
            var egg = new ItemStack(MaledictItems.FIRST_VICISSITUDE_PHASE_ONE_SPAWN_EGG.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, egg);
            helper.assertTrue(egg.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)) == InteractionResult.FAIL
                            && egg.use(level, player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.FAIL
                            && egg.getCount() == 1,
                    "Peaceful must reject the spawn egg without consuming it");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            var interaction = player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(interaction == InteractionResult.FAIL && !base.isActiveOrAssembling(),
                    "Peaceful must reject the rite before assembly or consumption starts");
            SummoningRite.summon(base, level, FirstVicissitudeBossEntity.BossDifficulty.SIMPLE);
            helper.assertTrue(!SummoningRite.hasNearbySummon(level, pos)
                            && level.getBlockEntity(pos) == base,
                    "The rite execution must also reject Peaceful while preserving the totem");
        } finally {
            level.getServer().getWorldData().setDifficulty(originalDifficulty);
        }
        helper.succeed();
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

}

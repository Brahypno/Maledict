package org.brahypno.maledict.common.rite;

import com.mojang.authlib.GameProfile;
import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.core.systems.rite.SpiritRiteType;
import com.sammy.malum.core.systems.spirit.SpiritTypeProperty;
import com.sammy.malum.registry.common.block.MalumBlocks;
import com.sammy.malum.registry.common.item.MalumItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.effect.SacrificeSchedule;
import org.brahypno.maledict.registry.MaledictMobEffects;

import java.util.UUID;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class TotemRiteRecognitionTest {
    @GameTest(template = "test_empty")
    public static void sacrificeActivationMarksNearbySwordBearer(GameTestHelper helper) {
        var level = helper.getLevel();
        var rite = SacrificeRite.rite();
        TotemBaseBlockEntity base = buildTotem(helper, rite, true);
        GameProfile profile = new GameProfile(UUID.randomUUID(), "SacrificeRiteTest");
        ServerPlayer player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        player.connection = new FakePlayer(level, profile).connection;
        player.moveTo(base.getBlockPos().getX() + 2.5, base.getBlockPos().getY(), base.getBlockPos().getZ() + 0.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MalumItems.TYRVING.get()));
        level.addNewPlayer(player);
        try {
            helper.assertTrue(!WisdomSacrifice.hasEarned(player), "The sword bearer must not already have the reward");
            base.onUseWithItem(player, player.getMainHandItem(), InteractionHand.MAIN_HAND);
            for (int tick = 0; tick < 110; tick++) {
                base.serverTick(level);
            }
            var mark = player.getEffect(MaledictMobEffects.SACRIFICE);
            helper.assertTrue(mark != null && mark.getDuration() == SacrificeSchedule.DURATION_TICKS,
                    "Assembling the real five-spirit totem must apply the full sacrifice effect");
            helper.assertTrue(player.getPersistentData().getBoolean("maledict:sacrifice_offered"),
                    "The activated rite must record the offering for the eventual reward");
            helper.assertTrue(base.getState() == TotemBaseBlockEntity.TotemBaseState.INACTIVE,
                    "The one-shot rite must stop after applying the offering");
        } finally {
            player.discard();
            clearTotem(helper, rite);
        }
        helper.succeed();
    }

    private static TotemBaseBlockEntity buildTotem(GameTestHelper helper, SpiritRiteType rite, boolean corrupted) {
        BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(base, (corrupted ? MalumBlocks.SOULWOOD_TOTEM_BASE
                : MalumBlocks.RUNEWOOD_TOTEM_BASE).get().defaultBlockState());
        for (int i = 0; i < rite.getSpirits().size(); i++) {
            var pole = (corrupted ? MalumBlocks.SOULWOOD_TOTEM_POLE : MalumBlocks.RUNEWOOD_TOTEM_POLE)
                    .get().defaultBlockState();
            helper.getLevel().setBlockAndUpdate(base.above(i + 1),
                    SpiritTypeProperty.setSpiritType(pole, rite.getSpirits().get(i).get()));
        }
        return (TotemBaseBlockEntity) helper.getLevel().getBlockEntity(base);
    }

    private static void clearTotem(GameTestHelper helper, SpiritRiteType rite) {
        BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
        for (int i = rite.getSpirits().size(); i >= 0; i--) {
            helper.getLevel().setBlockAndUpdate(base.above(i), Blocks.AIR.defaultBlockState());
        }
    }
}

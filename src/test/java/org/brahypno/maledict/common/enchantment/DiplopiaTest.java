package org.brahypno.maledict.common.enchantment;

import com.mojang.authlib.GameProfile;
import com.sammy.malum.common.entity.scythe.ScytheBoomerangEntity;
import com.sammy.malum.common.item.curiosities.TemporarilyDisabledItem;
import com.sammy.malum.core.handlers.enchantment.ReboundHandler;
import com.sammy.malum.registry.common.enchantment.EnchantmentKeys;
import com.sammy.malum.registry.common.item.MalumItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.TestSupport;
import org.brahypno.maledict.registry.MaledictEnchantments;

import java.util.List;
import java.util.UUID;

/**
 * 钉住投掷归属模型：原投掷（回旋）生成唯一一把可以复原武器槽的镰刀，每一把复视幻影都在返程路上
 * 自己消失。
 *
 * <p>1.20.1 的那条 GameTest 原样搬过来，只换了三处 1.8.2 的调用面：投掷入口从
 * {@code ReboundEnchantment#throwScythe} 换成 Malum 的 {@code ReboundHandler#throwScythe}（幻影由
 * {@code ReboundHandlerMixin} 在它的 {@code disable} 之前补出）、回旋附魔改从
 * {@code EnchantmentKeys.REBOUND} 取、禁用占位改调三参 {@code TemporarilyDisabledItem#disable}
 * （1.20.1 是两参，占位物品写死在里面；1.8.2 由调用方给）。
 */
@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class DiplopiaTest {
    @GameTest(template = "test_empty", batch = "diplopia", timeoutTicks = 120)
    public static void reboundDiplopiaThrowsPhantomsAndKeepsOneScythe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "DiplopiaTest"));
        player.setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());

        ItemStack scythe = new ItemStack(MalumItems.CRUDE_SCYTHE.get());
        scythe.enchant(TestSupport.enchantment(EnchantmentKeys.REBOUND), 1);
        scythe.enchant(TestSupport.enchantment(MaledictEnchantments.DIPLOPIA), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, scythe);

        ReboundHandler.throwScythe(level, player, InteractionHand.MAIN_HAND, scythe);

        List<ScytheBoomerangEntity> thrown = thrown(level, player);
        long phantoms = thrown.stream()
                              .filter(entity -> entity instanceof DiplopiaScytheBoomerangEntity)
                              .count();
        helper.assertTrue(thrown.size() == 4,
                "Rebound I + Diplopia III must throw 4 scythes, got " + thrown.size());
        helper.assertTrue(phantoms == 3, "3 phantoms expected, got " + phantoms);
        helper.assertTrue(thrown.stream()
                                .filter(entity -> entity instanceof DiplopiaScytheBoomerangEntity)
                                .allMatch(DiplopiaThrows::isEcho),
                "Every phantom must carry the client render marker");
        helper.assertTrue(thrown.stream()
                                .filter(entity -> !(entity instanceof DiplopiaScytheBoomerangEntity))
                                .noneMatch(DiplopiaThrows::isEcho),
                "The original scythe must not be marked as a phantom");

        helper.runAtTickTime(80, () -> {
            helper.assertTrue(thrown(level, player).isEmpty(),
                    "Every thrown scythe must be gone after its return");
            helper.assertTrue(countScythes(player) == 1,
                    "Exactly one scythe must remain, got " + countScythes(player));
            helper.assertTrue(player.getMainHandItem().getItem() == MalumItems.CRUDE_SCYTHE.get(),
                    "The original slot must be restored");

            // 幻影在 super.tick() 移动之后才轮到 Malum 的接取分支；把一枚带着标记的幻影摆在 3 格外、
            // 让这一 tick 的位移跨过边界，验证返程重定向没有把 Malum 的接取（以及它的冷却）放进来。
            int slot = player.getInventory().selected;
            player.getCooldowns().removeCooldown(scythe.getItem());
            TemporarilyDisabledItem.disable(player, slot, () -> MalumItems.SOUL_OF_A_SCYTHE.get());
            ItemStack marked = scythe.copy();
            CustomData.update(DataComponents.CUSTOM_DATA, marked, tag -> tag.putBoolean(DiplopiaThrows.ECHO_TAG, true));
            DiplopiaScytheBoomerangEntity phantom = new DiplopiaScytheBoomerangEntity(
                    level, player.getX() + 3.5, player.getY(), player.getZ());
            phantom.setData(player, 1.0F, 0.0F, slot, 0);
            phantom.setItem(marked);
            phantom.setDeltaMovement(-1.0, 0.0, 0.0);
            phantom.tick();

            helper.assertTrue(player.getInventory().getItem(slot).is(MalumItems.SOUL_OF_A_SCYTHE.get()),
                    "A phantom must not re-enable the weapon slot");
            helper.assertTrue(!player.getCooldowns().isOnCooldown(scythe.getItem()),
                    "A phantom must not add the Rebound cooldown");
            helper.assertTrue(phantom.isRemoved(), "The phantom must still be removed by its return");
            helper.succeed();
        });
    }

    private static List<ScytheBoomerangEntity> thrown(ServerLevel level, FakePlayer player) {
        return level.getEntitiesOfClass(
                ScytheBoomerangEntity.class,
                player.getBoundingBox().inflate(64.0),
                entity -> entity.getOwner() == player);
    }

    private static int countScythes(FakePlayer player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == MalumItems.CRUDE_SCYTHE.get()) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private DiplopiaTest() {
    }
}

package org.brahypno.maledict.common.enchantment;

import com.mojang.authlib.GameProfile;
import com.sammy.malum.common.enchantment.scythe.ReboundEnchantment;
import com.sammy.malum.common.entity.scythe.ScytheBoomerangEntity;
import com.sammy.malum.common.item.curiosities.TemporarilyDisabledItem;
import com.sammy.malum.registry.common.item.EnchantmentRegistry;
import com.sammy.malum.registry.common.item.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.registry.MaledictEnchantments;

import java.util.List;
import java.util.UUID;

/**
 * Pins the throw-ownership model: the original Rebound throw creates the only scythe that may
 * restore the weapon slot, and every Diplopia phantom discards itself on the return leg.
 */
@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class DiplopiaTest {
    @GameTest(template = "test_empty", batch = "diplopia", timeoutTicks = 120)
    public static void reboundDiplopiaThrowsPhantomsAndKeepsOneScythe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = new FakePlayer(
                level, new GameProfile(UUID.randomUUID(), "DiplopiaTest"));
        player.setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());

        ItemStack scythe = new ItemStack(ItemRegistry.CRUDE_SCYTHE.get());
        scythe.enchant(EnchantmentRegistry.REBOUND.get(), 1);
        scythe.enchant(MaledictEnchantments.DIPLOPIA.get(), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, scythe);

        ReboundEnchantment.throwScythe(level, player, InteractionHand.MAIN_HAND, scythe);

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
            helper.assertTrue(player.getMainHandItem().getItem() == ItemRegistry.CRUDE_SCYTHE.get(),
                    "The original slot must be restored");

            // The base tick moves the projectile before checking its pickup radius. Place a marked
            // phantom just outside 3 blocks, let that movement cross the boundary, and verify the
            // redirect in ScytheBoomerangEntityMixin keeps Malum's pickup (and its cooldown) away.
            int slot = player.getInventory().selected;
            player.getCooldowns().removeCooldown(scythe.getItem());
            TemporarilyDisabledItem.disable(player, slot);
            ItemStack marked = scythe.copy();
            marked.getOrCreateTag().putBoolean(DiplopiaThrows.ECHO_TAG, true);
            DiplopiaScytheBoomerangEntity phantom = new DiplopiaScytheBoomerangEntity(
                    level, player.getX() + 3.5, player.getY(), player.getZ());
            phantom.setData(player, 1.0f, 0.0f, slot, 0);
            phantom.setItem(marked);
            phantom.setDeltaMovement(-1.0, 0.0, 0.0);
            phantom.tick();

            helper.assertTrue(player.getInventory().getItem(slot).getItem()
                            == ItemRegistry.SOUL_OF_A_SCYTHE.get(),
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
            if (stack.getItem() == ItemRegistry.CRUDE_SCYTHE.get()){
                count += stack.getCount();
            }
        }
        return count;
    }

    private DiplopiaTest() {
    }
}

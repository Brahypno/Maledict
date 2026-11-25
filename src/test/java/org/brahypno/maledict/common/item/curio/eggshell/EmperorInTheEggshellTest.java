package org.brahypno.maledict.common.item.curio.eggshell;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.changelib.HealthHelper.CombatProtection;
import org.brahypno.maledict.registry.MaledictItems;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class EmperorInTheEggshellTest {

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void secondHitWithinWindowIsScaled(GameTestHelper helper) {
        TestPlayer player = new TestPlayer(helper.getLevel());
        player.setHealth(20.0F);
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new);
        inventory.setCurios(Map.of("charm", new CurioStacksHandler(
                inventory, "charm", 1, true, true, true, ICurio.DropRule.DEFAULT)));
        inventory.setEquippedCurio("charm", 0,
                new ItemStack(MaledictItems.EMPEROR_IN_THE_EGGSHELL.get()));
        // A scripted equip skips Curios' own onEquip callback, which is what installs the layer.
        EmperorInTheEggshellItem.installWard(player);
        helper.getLevel().addNewPlayer(player);

        player.receive(helper.getLevel().damageSources().generic(), 10.0F);
        helper.assertTrue(Math.abs(player.getHealth() - 10.0F) < 1.0E-4F,
                "The opening hit must be full, health=" + player.getHealth());

        helper.runAtTickTime(20, () -> {
            player.receive(helper.getLevel().damageSources().generic(), 10.0F);
            float health = player.getHealth();
            helper.assertTrue(Math.abs(health - 5.0F) < 0.6F,
                    "A hit 20 ticks later must be scaled to about 50%, health=" + health);
            verifyKillFeedback(helper, player);
            player.discard();
            helper.succeed();
        });
    }

    private static void verifyKillFeedback(GameTestHelper helper, TestPlayer player) {
        player.setHealth(player.getMaxHealth());
        var settled = new AtomicReference<CombatProtection.HitReport>();
        CombatProtection.HitFeedback listener = report -> {
            if (report.victim() == player) {
                settled.set(report);
            }
        };
        CombatProtection.onHitSettled(listener);
        try {
            var server = helper.getLevel().getServer();
            server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), "/kill @s");
        } finally {
            CombatProtection.removeHitFeedback(listener);
        }
        var report = settled.get();
        helper.assertTrue(report != null, "/kill must produce settled guard feedback");
        helper.assertTrue(report.guardSources().contains(ResourceLocation.tryParse(EmperorInTheEggshellItem.WARD_KEY)),
                "The feedback must identify the Eggshell guard");
        float blockedShare = 1.0F - report.dealt() / report.requested();
        helper.assertTrue(blockedShare == 1.0F && player.isAlive(),
                "A prevented /kill must reach the maximum blocked share at visual precision");
    }

    /** FakePlayer is permanently invulnerable; expose the real Player damage pipeline. */
    private static final class TestPlayer extends FakePlayer {
        TestPlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "EggshellTest"));
        }

        @Override
        public boolean isInvulnerableTo(DamageSource source) {
            return false;
        }

        void receive(DamageSource source, float amount) {
            actuallyHurt(source, amount);
        }
    }

    private EmperorInTheEggshellTest() {
    }
}

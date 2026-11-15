package org.brahypno.maledict.common.weapon.lover;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.changelib.DamageHelper.DamageRequestEvent;
import org.brahypno.maledict.registry.MaledictAttributes;
import org.brahypno.maledict.registry.MaledictItems;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class LoverAxeTest {
    @GameTest(template = "test_empty", batch = "lover_axe")
    public static void leftClickAppliesSharpnessAndDurabilityAndResetsCooldown(GameTestHelper helper) {
        ChargedPlayer player = player(helper);
        ItemStack weapon = player.getMainHandItem();
        weapon.enchant(Enchantments.SHARPNESS, 5);
        Cow victim = cow(helper);
        player.cooldownReset = false;
        List<DamageRequestEvent> magicRequests = new ArrayList<>();
        Consumer<DamageRequestEvent> listener = event -> {
            if (event.target() == victim
                    && event.attribute() == LodestoneAttributeRegistry.MAGIC_DAMAGE.get()) {
                magicRequests.add(event);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(listener);
        try {
            player.attack(victim);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
        }
        equal(helper, 85, victim.getHealth(), "Eleven melee damage plus Sharpness V and one magic damage");
        helper.assertTrue(magicRequests.size() == 1,
                "Maledict must dispatch exactly one Lodestone magic attribute request, got " + magicRequests.size());
        helper.assertTrue(magicRequests.get(0).source() != null
                                && magicRequests.get(0).source().is(DamageTypes.MAGIC),
                "The magic request must execute as minecraft:magic");
        helper.assertTrue(magicRequests.get(0).originalSource() != null
                                && magicRequests.get(0).originalSource().is(DamageTypes.PLAYER_ATTACK),
                "The magic request must keep the original player_attack source");
        equal(helper, 98, victim.getMaxHealth(), "Enchanted hit wounds once");
        equal(helper, 2, descent(player), "Enchanted hit rewards once");
        equal(helper, 2, weapon.getDamageValue(), "Axe melee consumes normal durability");
        helper.assertTrue(player.cooldownReset, "Left-click replacement resets attack cooldown");
        ChargedPlayer falling = player(helper);
        falling.getMainHandItem().enchant(Enchantments.SHARPNESS, 5);
        falling.fallDistance = 1;
        falling.setOnGround(false);
        Cow criticalTarget = cow(helper);
        falling.attack(criticalTarget);
        equal(helper, 79.5, criticalTarget.getHealth(), "Falling crit multiplies melee damage before Sharpness and magic");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "lover_axe")
    public static void confirmedSequenceAndPreHitDescent(GameTestHelper helper) {
        ChargedPlayer player = player(helper);
        Cow victim = cow(helper);
        player.attack(victim);
        equal(helper, 98, victim.getMaxHealth(), "First wound");
        equal(helper, 88, victim.getHealth(), "First hit uses Descent zero with eleven melee and one magic damage");
        equal(helper, 2, descent(player), "First full hit rewards once");
        victim.invulnerableTime = 0;
        player.attack(victim);
        equal(helper, 96.04, victim.getMaxHealth(), "Second wound multiplies remaining maximum");
        equal(helper, 88 - 12 * (1 + 3 * (1 - Math.exp(-0.2))), victim.getHealth(),
                "Second hit uses previous Descent two");
        equal(helper, 4, descent(player), "Second full hit rewards once");
        victim.heal(100);
        equal(helper, 96.04, victim.getHealth(), "Healing cannot restore lost maximum");
        Cow restored = cow(helper);
        restored.load(victim.saveWithoutId(new CompoundTag()));
        equal(helper, 96.04, restored.getMaxHealth(), "Wound survives save/load");
        helper.succeed();
    }

    private static Cow cow(GameTestHelper helper) {
        // ChangeLib's guard scan invokes lavaHurt; isolate melee amplification from that incidental hit.
        Cow cow = new Cow(EntityType.COW, helper.getLevel()) {
            @Override
            public boolean fireImmune() { return true; }
        };
        cow.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        cow.setHealth(100);
        return cow;
    }

    private static ChargedPlayer player(GameTestHelper helper) {
        return player(helper, new ItemStack(MaledictItems.LOVER_AXE.get()));
    }

    private static ChargedPlayer player(GameTestHelper helper, ItemStack axe) {
        ChargedPlayer player = new ChargedPlayer(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, axe);
        player.getAttributes().addTransientAttributeModifiers(axe.getAttributeModifiers(EquipmentSlot.MAINHAND));
        return player;
    }

    private static double descent(LivingEntity entity) {
        return entity.getAttribute(MaledictAttributes.DESCENT.get()).getBaseValue();
    }

    private static void equal(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 0.0001,
                message + ": expected " + expected + ", actual " + actual);
    }

    private static final class ChargedPlayer extends FakePlayer {
        float charge = 1.0F;
        boolean cooldownReset;
        ChargedPlayer(GameTestHelper helper) {
            super(helper.getLevel(), new GameProfile(UUID.randomUUID(), "LoverAxeTest"));
            setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());
        }
        @Override
        public float getAttackStrengthScale(float partialTick) { return charge; }
        @Override
        public void resetAttackStrengthTicker() {
            super.resetAttackStrengthTicker();
            cooldownReset = true;
        }
    }
}

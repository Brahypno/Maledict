package org.brahypno.maledict.common.item.weapon.incursus;

import com.sammy.malum.registry.common.MalumDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.registry.MaledictItems;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class IncursusAbsorptionTest {
    @GameTest(template = "test_empty", batch = "incursus_absorption")
    public static void equippedCapacityAllowsNativeDamageGainAndUnequippingClampsIt(GameTestHelper helper) {
        var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        attacker.setNoAi(true);
        var victim = helper.spawn(EntityType.COW, new BlockPos(3, 2, 1));
        victim.setNoAi(true);
        victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        victim.setHealth(200);
        ItemStack blade = MaledictItems.INCURSUS_BLADE.get().getDefaultInstance();
        IncursusBladeItem.setStat(blade, IncursusBladeItem.ELDRITCH_ABSORPTION, 20);
        attacker.setItemSlot(EquipmentSlot.MAINHAND, blade);
        attacker.tick();
        helper.assertTrue(attacker.getMaxAbsorption() == 1000, "The held blade provides capacity through native equipment attributes");
        var damageType = helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(MalumDamageTypes.SCYTHE_MELEE);
        var source = new DamageSource(damageType, attacker);
        victim.hurt(source, 50);
        helper.assertTrue(Math.abs(attacker.getAbsorptionAmount() - 10) < 0.001F,
                "The native Pre callback converts 20% of 50 damage into ten absorption points");
        victim.invulnerableTime = 0;
        victim.hurt(source, 100);
        helper.assertTrue(attacker.getAbsorptionAmount() == 20, "Capacity 1000 must not bypass the blade's own twenty-point cap");
        attacker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        attacker.tick();
        helper.assertTrue(attacker.getMaxAbsorption() == 0 && attacker.getAbsorptionAmount() == 0,
                "Unequipping removes capacity and vanilla clamps existing absorption");
        attacker.discard();
        victim.discard();
        helper.succeed();
    }
}

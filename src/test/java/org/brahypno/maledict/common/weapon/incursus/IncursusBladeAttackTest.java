package org.brahypno.maledict.common.weapon.incursus;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class IncursusBladeAttackTest {
    @GameTest(template = "test_empty")
    public static void slashGrowsLinearlyAndTheNextUnchargedSwingStartsSmall(GameTestHelper helper) {
        float[] charge = {0.0f, 0.5f, 1.0f, 0.0f};
        float[] expected = {0.3f, 0.65f, 1.0f, 0.3f};
        for (int i = 0; i < charge.length; i++) {
            helper.assertTrue(Math.abs(IncursusBladeAttack.slashScale(charge[i]) - expected[i]) < 1.0e-6f,
                    "Charge " + charge[i] + " must give visual scale " + expected[i]);
        }
        helper.succeed();
    }

}

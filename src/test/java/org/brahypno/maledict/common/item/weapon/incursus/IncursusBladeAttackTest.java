package org.brahypno.maledict.common.item.weapon.incursus;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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

    /** 服务端也必须能编解码特效包，不加载客户端渲染类；保留腰部坐标与本次蓄力倍率。 */

}

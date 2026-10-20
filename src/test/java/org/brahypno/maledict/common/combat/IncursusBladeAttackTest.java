package org.brahypno.maledict.common.combat;

import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.network.IncursusSlashPacket;

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
    @GameTest(template = "test_empty")
    public static void slashNotificationPreservesWorldCenterAndChargeScale(GameTestHelper helper) {
        Vec3 center = new Vec3(-17.25, 64.9, 1234.5);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            for (float charge : new float[]{0.0f, 0.5f, 1.0f}) {
                var sent = new IncursusSlashPacket(center, IncursusBladeAttack.slashScale(charge));
                IncursusSlashPacket.encode(sent, buffer);
                helper.assertTrue(sent.equals(IncursusSlashPacket.decode(buffer)),
                        "Slash notification changed its world center or charge scale");
                helper.assertTrue(!buffer.isReadable(), "Slash notification left unread data");
                buffer.clear();
            }
        } finally {
            buffer.release();
        }
        helper.succeed();
    }
}

package org.brahypno.maledict.common.entity.vicissitude.rite;

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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.ritual.wisdom.SacrificeRite;
import org.brahypno.maledict.common.ritual.wisdom.SacrificeSchedule;
import org.brahypno.maledict.common.ritual.wisdom.WisdomSacrifice;
import org.brahypno.maledict.registry.MaledictMobEffects;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.UUID;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class TotemRiteRecognitionTest {

    /**
     * 端到端：真图腾、真提尔锋、真 Curios 库存——从仪式发动一直走到两枚栏位到手。
     *
     * <p>断言用的是**库存自己的**基础尺寸，而不是写死的数字：Malum 的 {@code rune} 与
     * {@code delusion} 两枚槽在 {@code curios/slots} 里都是 size 0，仪式各加一。手工塞一份
     * {@code CurioStacksHandler} 会绕开「槽类型没挂在玩家身上」这条唯一的静默失败路径，所以这里
     * 一律用 {@code CuriosApi} 给出的真实库存。
     */
    @GameTest(template = "test_empty", batch = "wisdom_sacrifice")
    public static void sacrificeRiteMarksBearerThenAwardsBothCurioSlots(GameTestHelper helper) {
        var level = helper.getLevel();
        var rite = SacrificeRite.rite();
        TotemBaseBlockEntity base = buildTotem(helper, rite, true);
        GameProfile profile = new GameProfile(UUID.randomUUID(), "SacrificeRiteTest");
        ServerPlayer player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        player.connection = new FakePlayer(level, profile).connection;
        player.moveTo(base.getBlockPos().getX() + 2.5, base.getBlockPos().getY(), base.getBlockPos().getZ() + 0.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MalumItems.TYRVING.get()));
        // The offering charges all 1080 voodoo damage, so the bearer needs a body that survives it.
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024.0D);
        player.setHealth(player.getMaxHealth());
        player.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(1000.0D);
        player.setAbsorptionAmount(1000.0F);
        level.addNewPlayer(player);

        ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing real Curios capability"));
        int runesBefore = slotCount(helper, inventory, WisdomSacrifice.RUNE_SLOT);
        int visionsBefore = slotCount(helper, inventory, WisdomSacrifice.DELUSION_SLOT);

        helper.assertTrue(!WisdomSacrifice.hasEarned(player), "The sword bearer must not already have the reward");
        base.onUseWithItem(player, player.getMainHandItem(), InteractionHand.MAIN_HAND);
        for (int tick = 0; tick < 110; tick++) {
            base.serverTick(level);
        }
        MobEffectInstance mark = player.getEffect(MaledictMobEffects.SACRIFICE);
        if (mark == null) {
            helper.fail("Assembling the real five-spirit totem must apply the sacrifice effect");
            return;
        }
        helper.assertTrue(mark.getDuration() == SacrificeSchedule.DURATION_TICKS,
                "The rite must apply the full nine-day offering, got " + mark.getDuration());
        helper.assertTrue(player.getPersistentData().getBoolean("maledict:sacrifice_offered"),
                "The activated rite must record the offering for the eventual reward");
        helper.assertTrue(base.getState() == TotemBaseBlockEntity.TotemBaseState.INACTIVE,
                "The one-shot rite must stop after applying the offering");

        // 1.21 dropped MobEffectInstance#setDuration, so the real duration is spent one tick at a
        // time; tick() also runs the effect's own pulse, so all 180 hits are dealt on the way down.
        for (int tick = 0; tick < SacrificeSchedule.DURATION_TICKS; tick++) {
            mark.tick(player, () -> {
            });
        }
        // tickEffects posts Expired once the duration is spent, which is the only path that awards.
        player.doTick();
        helper.assertTrue(WisdomSacrifice.hasEarned(player),
                "Surviving the natural expiry must award Wisdom's Sacrifice");

        helper.runAfterDelay(1, () -> {
            // Curios recomputes slot sizes lazily, and skips the tick it deserialises on.
            helper.assertTrue(slotCount(helper, inventory, WisdomSacrifice.RUNE_SLOT) == runesBefore + 1,
                    "The rune slot must grow by exactly one, from " + runesBefore);
            helper.assertTrue(slotCount(helper, inventory, WisdomSacrifice.DELUSION_SLOT) == visionsBefore + 1,
                    "The vision slot must grow by exactly one, from " + visionsBefore);

            // Re-granting must not stack: the same modifier id has to stay a single entry.
            var wisdom = player.server.getAdvancements().get(WisdomSacrifice.ID);
            helper.assertTrue(wisdom != null, "Wisdom's Sacrifice must be loaded");
            player.getAdvancements().revoke(wisdom, "impossible");
            player.getAdvancements().award(wisdom, "impossible");
            WisdomSacrifice.ensureSlots(player); // the same call the login reconciliation makes
            helper.assertTrue(slotCount(helper, inventory, WisdomSacrifice.RUNE_SLOT) == runesBefore + 1,
                    "A second grant must not stack another rune slot");
            helper.assertTrue(slotCount(helper, inventory, WisdomSacrifice.DELUSION_SLOT) == visionsBefore + 1,
                    "A second grant must not stack another vision slot");

            player.discard();
            clearTotem(helper, rite);
            helper.succeed();
        });
    }

    /** Curios 对没有挂到玩家身上的槽位是静默跳过的，所以先确认这个槽位真的存在。 */
    private static int slotCount(GameTestHelper helper, ICuriosItemHandler inventory, String identifier) {
        ICurioStacksHandler handler = inventory.getCurios().get(identifier);
        if (handler == null) {
            helper.fail("The real player inventory must expose the '" + identifier
                    + "' slot type, otherwise the wisdom modifier is dropped without a word");
            return -1;
        }
        return handler.getStacks().getSlots();
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

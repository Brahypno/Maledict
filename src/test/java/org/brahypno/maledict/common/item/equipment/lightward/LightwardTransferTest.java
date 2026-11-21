package org.brahypno.maledict.common.item.equipment.lightward;

import com.mojang.authlib.GameProfile;
import com.sammy.malum.registry.common.MalumDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.changelib.DamageHelper.DamageRequestEvent;
import org.brahypno.maledict.common.item.weapon.incursus.IncursusBladeAttack;
import org.brahypno.maledict.common.item.weapon.incursus.IncursusBladeItem;
import org.brahypno.maledict.common.item.weapon.incursus.IncursusSwingReach;
import org.brahypno.maledict.registry.MaledictAttributes;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 扬弃逐光 5 级的连带：主手神侵恶刃打中认主的生物，主人也吃下同一刀的等额伤害。
 *
 * <p>钉三件事：件数不到五件时主人一点不疼（门禁）；够五件时主人与那只看家的掉血完全相等（等额）；
 * 主人挨的那一记就是这把刀的 {@code SCYTHE_MELEE} 主伤害，同一个伤害方法、同一个数值 —— 不是另起
 * 一套伤害类型或另一条派发路径。
 *
 * <p>「主人」是一个不动的僵尸，被打的是一只认它为主的狼：认主只按 {@code getOwnerUUID()} 认。
 * 1.21 侧只换了事件名与属性句柄（{@code LivingIncomingDamageEvent}、{@code ENTITY_INTERACTION_RANGE}、
 * {@code NeoForge.EVENT_BUS}），断言与 1.20.1 逐条相同。
 */
@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class LightwardTransferTest {

    @GameTest(template = "test_empty", batch = "lightward_transfer")
    public static void theFifthPieceCarriesOneSwingToTheMaster(GameTestHelper helper) {
        Vec3 arena = freeArena(helper);

        Fixture bare = fixture(helper, arena, false);
        IncursusBladeAttack.perform(bare.player());
        float barePetLoss = bare.petBefore() - bare.pet().getHealth();
        float bareMasterLoss = bare.masterBefore() - bare.master().getHealth();
        helper.assertTrue(barePetLoss > 0.0F, "The swing itself must wound the pet: loss=" + barePetLoss);
        helper.assertTrue(bareMasterLoss == 0.0F,
                          "One blade alone is not five pieces: the master must not be touched, loss=" + bareMasterLoss);
        bare.remove();

        Fixture armed = fixture(helper, arena, true);
        List<LivingIncomingDamageEvent> petHits = new ArrayList<>();
        List<LivingIncomingDamageEvent> masterHits = new ArrayList<>();
        List<DamageRequestEvent> petChannels = new ArrayList<>();
        List<DamageRequestEvent> masterChannels = new ArrayList<>();
        Consumer<LivingIncomingDamageEvent> hurtListener = event -> {
            if (event.getEntity() == armed.pet()){
                petHits.add(event);
            }else if (event.getEntity() == armed.master()){
                masterHits.add(event);
            }
        };
        Consumer<DamageRequestEvent> channelListener = event -> {
            if (event.attribute() == null){
                return;
            }
            if (event.target() == armed.pet()){
                petChannels.add(event);
            }else if (event.target() == armed.master()){
                masterChannels.add(event);
            }
        };
        NeoForge.EVENT_BUS.addListener(hurtListener);
        NeoForge.EVENT_BUS.addListener(channelListener);
        try {
            IncursusBladeAttack.perform(armed.player());
        }
        finally {
            NeoForge.EVENT_BUS.unregister(hurtListener);
            NeoForge.EVENT_BUS.unregister(channelListener);
        }
        float petLoss = armed.petBefore() - armed.pet().getHealth();
        float masterLoss = armed.masterBefore() - armed.master().getHealth();
        helper.assertTrue(petLoss > 0.0F, "The swing itself must wound the pet: loss=" + petLoss);
        helper.assertTrue(masterLoss > 0.0F,
                          "Four armour pieces plus the blade must carry the wound to the master, loss=" + masterLoss);
        helper.assertTrue(Math.abs(petLoss - masterLoss) < 0.01F,
                          "The master must take the same amount as the pet: pet=" + petLoss + ", master=" + masterLoss);
        helper.assertTrue(!masterHits.isEmpty(),
                          "The master must really be hit by the blade, not just lose health: hits=" + masterHits.size());
        // 第一记是主伤害，后面的冻结尾随（刀上喂了精魂）。
        helper.assertTrue(masterHits.get(0).getSource().is(MalumDamageTypes.SCYTHE_MELEE),
                          "The master's main hit must be the blade's own SCYTHE_MELEE hit: "
                          + masterHits.get(0).getSource().getMsgId());
        helper.assertTrue(!petHits.isEmpty() && petHits.get(0).getAmount() == masterHits.get(0).getAmount(),
                          "Both must be asked for the same damage value: pet=" + amounts(petHits)
                          + ", master=" + amounts(masterHits));
        helper.assertTrue(petChannels.size() == masterChannels.size(),
                          "The blade's channels must be dispatched by the same method for both targets: pet="
                          + petChannels.size() + ", master=" + masterChannels.size());
        // 碧水（冻结）是属性通道：每次落地的主伤害之后，ChangeLib 各派发一笔，主人也一样。
        // 这一条钉的是「属性口径」本身 —— 移植时它曾被降级成显式 freeze 探针，于是通道表里查不到它。
        long petFreeze = freezeRequests(petChannels);
        long masterFreeze = freezeRequests(masterChannels);
        helper.assertTrue(petFreeze == 1 && masterFreeze == 1,
                          "The aqueous channel must be exactly one attribute request per target: pet="
                          + petFreeze + ", master=" + masterFreeze);
        float petFreezeAmount = freezeAmount(petChannels);
        helper.assertTrue(Math.abs(petFreezeAmount - 8.0F) < 0.01F,
                          "The aqueous request must carry the blade's powder_snow_damage stat: " + petFreezeAmount);
        helper.assertTrue(Math.abs(petFreezeAmount - freezeAmount(masterChannels)) < 0.01F,
                          "Both targets must be asked for the same aqueous amount: pet=" + petFreezeAmount
                          + ", master=" + freezeAmount(masterChannels));
        armed.remove();
        helper.succeed();
    }

    private static long freezeRequests(List<DamageRequestEvent> channels) {
        return channels.stream()
                       .filter(event -> event.attribute() != null
                                        && event.attribute().value()
                                           == MaledictAttributes.POWDER_SNOW_DAMAGE.get())
                       .count();
    }

    private static float freezeAmount(List<DamageRequestEvent> channels) {
        return channels.stream()
                       .filter(event -> event.attribute() != null
                                        && event.attribute().value()
                                           == MaledictAttributes.POWDER_SNOW_DAMAGE.get())
                       .map(DamageRequestEvent::amount)
                       .findFirst()
                       .orElse(0.0F);
    }

    private static List<Float> amounts(List<LivingIncomingDamageEvent> hits) {
        return hits.stream().map(LivingIncomingDamageEvent::getAmount).toList();
    }

    /**
     * 结构本身可能埋在石头里，挥砍要求视线，所以沿这一列往上找第一处「三人都有地方站、镜头到狼眼睛
     * 一路都是空气」的位置；顺手把区块同步出来，否则 {@code addFreshEntity} 只是排队，目标根本不在世界里。
     */
    private static Vec3 freeArena(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 base = helper.absolutePos(new BlockPos(2, 2, 2)).getCenter();
        for (int dy = 2; dy <= 384; dy += 2) {
            Vec3 spot = base.add(0, dy, 0);
            if (spot.y > level.getMaxBuildHeight() - 8){
                break;
            }
            Vec3 petSpot = spot.add(0, 0, 2.5);
            Vec3 masterSpot = spot.add(0, 0, -8);
            if (clearColumn(level, spot) && clearColumn(level, petSpot) && clearColumn(level, masterSpot)
                && clearRay(level, spot.add(0, 1.62, 0), petSpot.add(0, 0.85, 0))){
                BlockPos pos = BlockPos.containing(spot);
                level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                return spot;
            }
        }
        throw new IllegalStateException("No clear air above the test structure for the lightward fixture");
    }

    private static boolean clearColumn(ServerLevel level, Vec3 spot) {
        BlockPos feet = BlockPos.containing(spot);
        return level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir();
    }

    private static boolean clearRay(ServerLevel level, Vec3 from, Vec3 to) {
        for (int step = 0; step <= 8; step++) {
            if (!level.getBlockState(BlockPos.containing(from.lerp(to, step / 8.0D))).isAir()){
                return false;
            }
        }
        return true;
    }

    private static Fixture fixture(GameTestHelper helper, Vec3 arena, boolean fullSet) {
        ServerLevel level = helper.getLevel();
        Zombie master = new Zombie(EntityType.ZOMBIE, level) {
            @Override
            public boolean fireImmune() {
                return true;
            }
        };
        master.setPos(arena.add(0, 0, -8));
        master.setNoGravity(true);
        master.setNoAi(true);
        // 僵尸自带 2 点护甲（20 点伤害会掉成 19.68），抹掉它才能用「掉血量完全相等」来钉等额本身。
        master.getAttribute(Attributes.ARMOR).setBaseValue(0);
        master.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        master.setHealth(200);
        helper.assertTrue(level.addFreshEntity(master), "The master stand-in must join the world");

        Wolf pet = new Wolf(EntityType.WOLF, level);
        pet.setPos(arena.add(0, 0, 2.5));
        pet.setNoGravity(true);
        pet.setNoAi(true);
        // 驯服会把最大生命改回 20，所以先驯服再抬高血量，保证这一刀打死不了它、掉的量能逐分比较。
        pet.setTame(true, true);
        pet.setOwnerUUID(master.getUUID());
        pet.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        pet.setHealth(200);
        helper.assertTrue(level.addFreshEntity(pet), "The owned pet must join the world");

        SwingPlayer player = new SwingPlayer(level);
        player.setPos(arena);
        player.setNoGravity(true);
        player.setYRot(0);
        player.setXRot(0);
        ItemStack blade = new ItemStack(MaledictItems.INCURSUS_BLADE.get());
        IncursusBladeItem.setStat(blade, IncursusBladeItem.ATTACK_DAMAGE, 19);
        // 喂一点精魂，让刀上的两条属性通道也真的会派发 —— 它们必须跟着同一记主伤害走。
        IncursusBladeItem.setStat(blade, IncursusBladeItem.MAGIC_DAMAGE, 8);
        IncursusBladeItem.setStat(blade, IncursusBladeItem.POWDER_SNOW_DAMAGE, 8);
        player.setItemSlot(EquipmentSlot.MAINHAND, blade);
        // 1.21 的属性在组件里：把主手那一组直接挂成临时修饰符，玩家的攻击力才会是这把刀的。
        blade.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null){
                instance.addTransientModifier(modifier);
            }
        });
        if (fullSet){
            player.setItemSlot(EquipmentSlot.HEAD,
                               new ItemStack(MaledictItems.AUFHEBEN_LIGHTWARD_HELMET.get()));
            player.setItemSlot(EquipmentSlot.CHEST,
                               new ItemStack(MaledictItems.AUFHEBEN_LIGHTWARD_CHESTPLATE.get()));
            player.setItemSlot(EquipmentSlot.LEGS,
                               new ItemStack(MaledictItems.AUFHEBEN_LIGHTWARD_LEGGINGS.get()));
            player.setItemSlot(EquipmentSlot.FEET,
                               new ItemStack(MaledictItems.AUFHEBEN_LIGHTWARD_BOOTS.get()));
        }
        // 场地或区块出问题时先在这里炸，省得后面把「没打中」误读成「连带没生效」。
        // 判法必须与挥砍本身同一个函数，否则夹具会在检查一件已经不成立的事。
        double reach = IncursusSwingReach.radiusOf(
                player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE), 0);
        Vec3 swingCenter = IncursusSwingReach.centerOf(player);
        double distance = Math.sqrt(IncursusSwingReach.distanceSqrToBox(pet.getBoundingBox(), swingCenter));
        helper.assertTrue(player.hasLineOfSight(pet) && !pet.skipAttackInteraction(player) && distance <= reach,
                          "The fixture must start from a hittable pet: sight=" + player.hasLineOfSight(pet)
                          + ", skip=" + pet.skipAttackInteraction(player) + ", distance=" + distance
                          + ", reach=" + reach + ", pet=" + pet.position() + ", player=" + player.position());
        return new Fixture(player, pet, master);
    }

    private record Fixture(SwingPlayer player, Wolf pet, Zombie master, float petBefore, float masterBefore) {
        Fixture(SwingPlayer player, Wolf pet, Zombie master) {
            this(player, pet, master, pet.getHealth(), master.getHealth());
        }

        void remove() {
            pet.discard();
            master.discard();
        }
    }

    private static final class SwingPlayer extends FakePlayer {
        SwingPlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "LightwardTransferTest"));
        }

        @Override
        public float getAttackStrengthScale(float partialTick) {
            return 1.0F;
        }
    }

    private LightwardTransferTest() {
    }
}

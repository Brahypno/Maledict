package org.brahypno.maledict.common.enchantment;

import com.sammy.malum.common.entity.scythe.ScytheBoomerangEntity;
import com.sammy.malum.common.item.curiosities.weapons.scythe.MalumScytheItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.common.item.weapon.incursus.IncursusBladeItem;
import org.brahypno.maledict.common.item.weapon.incursus.IncursusScytheBoomerangEntity;
import org.brahypno.maledict.registry.MaledictEnchantments;
import team.lodestar.lodestone.registry.common.LodestoneAttributes;

/**
 * 复视：给 Malum 的回旋投掷补出若干把幻影镰刀。
 *
 * <p><b>1.21 没有 {@code DiplopiaEnchantment} 这个类。</b>1.8.2 的附魔是数据驱动的
 * （{@code data/maledict/enchantment/diplopia.json} + {@code MaledictEnchantments.DIPLOPIA}），
 * 1.20.1 那个 {@code extends Enchantment} 的类只当「参数常量表」用，所以常量全部收在这里，
 * 不再造一个假的 Enchantment 子类。
 *
 * <p>只有被 Malum 真正投出的那一把拥有武器本体。这里的每一把都是回声：它带一份镰刀堆叠的
 * 副本用于渲染与附魔结算，但绝不调用 {@code TemporarilyDisabledItem#disable} 或
 * {@code ReboundHandler#pickupScythe}，返程到家门口时自己 {@code discard()}
 * （{@link DiplopiaScytheBoomerangEntity}、{@link IncursusScytheBoomerangEntity}）。
 * 因此玩家手里永远只回来一把镰刀。
 */
public final class DiplopiaThrows {
    /**
     * 幻影堆叠副本上的标记（{@link DataComponents#CUSTOM_DATA}）。客户端渲染器从同步过来的
     * 物品堆叠上读它，真正的武器永远不会带上。
     *
     * <p>1.21 的物品根 NBT 没了，1.20.1 的 {@code getOrCreateTag().putBoolean(...)} 换成
     * {@code CustomData.update(...)} / {@code getOrDefault(...).copyTag()}。
     */
    public static final String ECHO_TAG = "maledict:diplopia_echo";

    /**
     * 与 {@code diplopia.json} 的 {@code max_level} 同步；Java 侧只用来查等级，不参与注册。
     */
    public static final int MAX_LEVEL = 3;

    /**
     * 每把幻影按原投掷伤害的这个比例结算。
     */
    public static final float ECHO_DAMAGE_MULTIPLIER = 0.5f;

    /**
     * 相对原投掷方向的最大随机偏航，单位度。
     */
    public static final float ECHO_YAW_SPREAD = 50.0f;

    /**
     * 相对原投掷方向的最大随机俯仰，单位度。
     */
    public static final float ECHO_PITCH_SPREAD = 12.0f;

    /**
     * 每把幻影初速的随机浮动比例（上下浮动同值）。
     */
    public static final float ECHO_SPEED_JITTER = 0.15f;

    /**
     * 每把幻影额外掷出的去程 tick 上限，免得整排镰刀同一 tick 掉头。
     */
    public static final int ECHO_RETURN_TIMER_JITTER = 4;

    /**
     * 1.8.2 {@code ReboundHandler#throwScythe} 自己用的强化伤害倍率（javap：掷出前
     * {@code damage *= 1.3f; magicDamage *= 1.3f}，1.20.1 的 Malum 是 1.5）。幻影要跟原投掷同口径，
     * 所以这里照抄 1.3 而不是 1.20.1 的 1.5。
     */
    private static final float NARROW_DAMAGE_MULTIPLIER = 1.3f;

    private static final float NORMAL_VELOCITY = 1.75f;
    private static final float NARROW_VELOCITY = 3.0f;

    private static final int RETURN_TIMER = 8;

    /**
     * Malum 的回旋镰刀在 {@code super.tick()} 里先移动、再判 3 格接取半径。幻影必须在那次移动
     * 越过边界之前消散，所以守卫用的是「下一 tick 的预测位置」加一点余量。
     */
    private static final double ECHO_PICKUP_GUARD = 3.5;

    /**
     * 在 Malum 自己的回旋投掷之后运行（mixin 锚点见 {@code ReboundHandlerMixin}）。伤害与出膛点
     * 按 {@code ReboundHandler#throwScythe} 的同一套公式重算：那次静态调用里没有任何东西会移动
     * 玩家或改属性，所以幻影与原投掷同点同状态出发。
     */
    public static void afterReboundThrow(
            Level level, ServerPlayer player, InteractionHand hand, ItemStack scythe) {
        int extra = MaledictEnchantments.level(scythe, MaledictEnchantments.DIPLOPIA);
        if (extra <= 0){
            return;
        }

        // 玩家侧仍然是「这把镰刀不能横扫」= 强化；弹体侧的字段已改成 setNarrow/isNarrow。
        boolean enhanced = MalumScytheItem.isEnhanced(player);
        float attackDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float magicDamage = (float) player.getAttributeValue(LodestoneAttributes.MAGIC_DAMAGE);
        float velocity = enhanced ? NARROW_VELOCITY : NORMAL_VELOCITY;
        Vec3 position = throwPosition(player, hand, enhanced);
        if (enhanced){
            attackDamage *= NARROW_DAMAGE_MULTIPLIER;
            magicDamage *= NARROW_DAMAGE_MULTIPLIER;
        }
        spawnEchoes(level, player, hand, scythe, attackDamage, magicDamage,
                    enhanced, position, velocity);
    }

    /**
     * 每级复视生成一把幻影。上层两处调用：上面的回旋注入，以及神侵恶刃自己的投掷
     * （{@code IncursusBladeEnchantments#throwScythe}，它手里已经有精确的三条伤害通道）。
     */
    public static void spawnEchoes(
            Level level,
            ServerPlayer player,
            InteractionHand hand,
            ItemStack scythe,
            float attackDamage,
            float magicDamage,
            boolean enhanced,
            Vec3 position,
            float velocity) {
        int extra = MaledictEnchantments.level(scythe, MaledictEnchantments.DIPLOPIA);
        if (extra <= 0){
            return;
        }

        int slot = hand == InteractionHand.OFF_HAND
                   ? player.getInventory().getContainerSize() - 1
                   : player.getInventory().selected;
        float multiplier = ECHO_DAMAGE_MULTIPLIER;
        for (int i = 0; i < extra; i++) {
            float yaw = player.getYRot() + Mth.nextFloat(
                    player.getRandom(), -ECHO_YAW_SPREAD, ECHO_YAW_SPREAD);
            float pitch = Mth.clamp(
                    player.getXRot() + Mth.nextFloat(
                            player.getRandom(), -ECHO_PITCH_SPREAD, ECHO_PITCH_SPREAD),
                    -90.0f, 90.0f);
            float speed = velocity * Mth.nextFloat(
                    player.getRandom(), 1.0f - ECHO_SPEED_JITTER, 1.0f + ECHO_SPEED_JITTER);
            int returnTimer = RETURN_TIMER + player.getRandom().nextInt(ECHO_RETURN_TIMER_JITTER + 1);

            ItemStack echoStack = scythe.copy();
            CustomData.update(
                    DataComponents.CUSTOM_DATA, echoStack, tag -> tag.putBoolean(ECHO_TAG, true));

            ScytheBoomerangEntity echo = scythe.getItem() instanceof IncursusBladeItem
                                         ? new IncursusScytheBoomerangEntity(
                    level, position.x, position.y, position.z, true)
                                         : new DiplopiaScytheBoomerangEntity(
                    level, position.x, position.y, position.z);
            echo.setData(player, attackDamage * multiplier, magicDamage * multiplier, slot, returnTimer);
            echo.setItem(echoStack);
            echo.setNarrow(enhanced);
            echo.shootFromRotation(player, pitch, yaw, 0.0f, speed, 0.0f);
            level.addFreshEntity(echo);
        }
    }

    /**
     * 返程的下一 tick 会不会进入 Malum 的接取半径。检查跑在 {@code super.tick()} 移动实体之前，
     * 所以幻影永远到不了基类的接取分支（也就不会加回旋冷却、不会复原武器槽）。
     */
    public static boolean reachedOwnerOnReturn(ScytheBoomerangEntity echo) {
        Entity owner = echo.getOwner();
        if (owner == null || echo.returnTimer > 0){
            return false;
        }
        Vec3 nextPosition = echo.position().add(echo.getDeltaMovement());
        return nextPosition.distanceTo(owner.position()) < ECHO_PICKUP_GUARD;
    }

    /**
     * 同步过来的物品堆叠上是否带着 {@link #spawnEchoes} 写下的幻影标记。
     */
    public static boolean isEcho(ScytheBoomerangEntity entity) {
        CustomData data = entity.getItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return !data.isEmpty() && data.copyTag().getBoolean(ECHO_TAG);
    }

    /**
     * 复刻 {@code ReboundHandler#throwScythe} 里的出膛点。
     */
    private static Vec3 throwPosition(ServerPlayer player, InteractionHand hand, boolean enhanced) {
        Vec3 position = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        if (enhanced){
            int angle = hand == InteractionHand.MAIN_HAND ? 225 : 90;
            double radians = Math.toRadians(angle - player.yHeadRot);
            position = player.position()
                             .add(player.getLookAngle().scale(0.5))
                             .add(0.75 * Math.sin(radians), player.getBbHeight() * 0.9,
                                  0.75 * Math.cos(radians));
        }
        return position;
    }

    private DiplopiaThrows() {
    }
}

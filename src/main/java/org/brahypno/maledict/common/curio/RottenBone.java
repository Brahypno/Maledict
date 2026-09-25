package org.brahypno.maledict.common.curio;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 「朽骨符文」的账：替佩戴者去死，代价是从生命上限里抽走骨头。
 *
 * <h2>四分之一怎么扣</h2>
 * 每次触发挂一条 {@code MULTIPLY_TOTAL} 修饰符，数值是 {@link #modifierAmount()}——负的四分之一，
 * 也就是把当前上限乘以 0.75（基础上限 20 的玩家）：
 * <pre>
 *   触发次数   0      1       2        3         4
 *   生命上限   20 →  15  →  11.25 →  8.4375 →  6.328125   四条用完为止
 * </pre>
 * 乘算永远到不了 1，所以「最多四次」由 {@link #nextStack} 的四条 id 兜底，不靠原版夹取；
 * 需求里那句「上限大于 1 才触发」照写，只是几乎不会先撞上它。
 *
 * <h2>债跟着玩家走</h2>
 * 修饰符由 {@link RottenBoneEvents} 挂在玩家属性上，是<b>永久</b>修饰符而不是 Curios 的槽位加成：
 * 摘下符文不会把骨头还回来，它随玩家存档一起落盘（原版 {@code Attributes} 标签）。
 * 还清的方式只有一种——真正死一次。
 *
 * <h2>为什么按条数记账</h2>
 * 四条各有自己的 id，属性上还挂着几条就是已经抽了几次，不必另存一份计数：
 * {@link #nextStack(Collection)} 找出第一条还没挂上的，四条都在就是「抽干了」。
 *
 * <p>这个类只碰两种 Minecraft 类型：{@link AttributeModifier} 与它里面的 {@code Operation}，
 * 两个都是纯数据（不查注册表、不需要 Bootstrap）。
 */
public final class RottenBone {

    /** 每次抽走的比例，正数；挂到属性上时要取负，见 {@link #modifierAmount()}。 */
    public static final double LOSS_FRACTION = 0.25D;

    /** 一共四条：四条各抽掉当前上限的四分之一。 */
    public static final int MAX_TRIGGERS = 4;

    /** 接住一次死亡之后回多少血；上限比这还低时由原版夹到上限。 */
    public static final float HEAL = 7.0F;

    /** 接住之后给多久的无敌帧。 */
    public static final int INVULNERABLE_TICKS = 70;

    /**
     * 上限被抽到这儿就不再接了：需求写的是「上限大于 1 才触发」。
     * 乘算下这条门槛对玩家几乎不会生效（四刀之后还有三成多），兜底的是四条 id。
     */
    public static final double SPENT_MAX_HEALTH = 1.0D;

    /** 修饰符 id 的种子前缀；改它等于把旧存档上的账作废，别改。 */
    private static final String MODIFIER_ID_PREFIX = "maledict:rune_of_rotten_bone/";

    /**
     * 修饰符的名字，只有 {@code /attribute} 与调试界面看得见，不翻译——
     * 与 {@code AttributeRuneItem} 里那几条同一处理。末尾带上第几条，方便读属性时对账。
     */
    private static final String MODIFIER_NAME = "Rune of Rotten Bone";

    private static final List<UUID> MODIFIER_IDS = buildModifierIds();

    /** 四条修饰符的 id，第一条对应第一次触发。 */
    public static List<UUID> modifierIds() {
        return MODIFIER_IDS;
    }

    /** 第 {@code stack} 条（从 0 数起）修饰符的 id。 */
    public static UUID modifierId(int stack) {
        return MODIFIER_IDS.get(stack);
    }

    /** 第 {@code stack} 条修饰符的名字，见 {@link #MODIFIER_NAME}。 */
    public static String modifierName(int stack) {
        return MODIFIER_NAME + " " + (stack + 1) + "/" + MAX_TRIGGERS;
    }

    /**
     * 这一次该挂第几条修饰符；四条都挂上了就是 {@link OptionalInt#empty()}。
     *
     * @param applied 属性上现有的修饰符 id
     */
    public static OptionalInt nextStack(Collection<UUID> applied) {
        for (int stack = 0; stack < MAX_TRIGGERS; stack++) {
            if (!applied.contains(MODIFIER_IDS.get(stack))) {
                return OptionalInt.of(stack);
            }
        }
        return OptionalInt.empty();
    }

    /**
     * 第 {@code stack} 条修饰符：{@code MULTIPLY_TOTAL} 配 {@link #modifierAmount()}。
     *
     * <p>运算方式与数值都从这里出，调用点只负责挂上去，不会两边对不上。
     */
    public static AttributeModifier modifier(int stack) {
        return new AttributeModifier(modifierId(stack), modifierName(stack), modifierAmount(),
                                     AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    /**
     * 这一条修饰符挂到 {@code generic.max_health} 上的数值：{@link #LOSS_FRACTION} 取负，
     * 也就是 {@code MULTIPLY_TOTAL} 的 {@code -0.25}（当前上限 × 0.75）。
     *
     * <p>必须是负数：正数会把上限加上去。
     */
    public static double modifierAmount() {
        return -LOSS_FRACTION;
    }

    /** 需求里的门槛：上限大于 1 才轮得到免死。乘算下这条几乎不会生效，兜底的是四条 id。 */
    public static boolean canSave(double maxHealth) {
        return maxHealth > SPENT_MAX_HEALTH;
    }

    private static List<UUID> buildModifierIds() {
        List<UUID> ids = new ArrayList<>(MAX_TRIGGERS);
        for (int stack = 0; stack < MAX_TRIGGERS; stack++) {
            ids.add(UUID.nameUUIDFromBytes(
                    (MODIFIER_ID_PREFIX + stack).getBytes(StandardCharsets.UTF_8)));
        }
        return List.copyOf(ids);
    }

    private RottenBone() {
    }
}

package org.brahypno.maledict.common.curio;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 「朽骨符文」的账：替佩戴者去死，每触发一次挂一条 -25% 的 {@code MULTIPLY_TOTAL} 修饰符。
 * 修饰符是永久的——摘下符文不还骨头，只有真正死一次才还清；抽了几次靠四条 modifier id 记账。
 */
public final class RottenBone {

    /** 每次抽走的比例；挂到属性上时取负，见 {@link #modifierAmount()}。 */
    public static final double LOSS_FRACTION = 0.25D;

    public static final int MAX_TRIGGERS = 4;

    /** 接住一次死亡之后回多少血；上限比这还低时由原版夹到上限。 */
    public static final float HEAL = 7.0F;

    public static final int INVULNERABLE_TICKS = 70;

    /** 上限被抽到这儿就不再接了。 */
    public static final double SPENT_MAX_HEALTH = 1.0D;

    /** 修饰符 id 的种子前缀；改它等于把旧存档上的账作废，别改。 */
    private static final String MODIFIER_ID_PREFIX = "maledict:rune_of_rotten_bone/";

    private static final String MODIFIER_NAME = "Rune of Rotten Bone";

    private static final List<UUID> MODIFIER_IDS = buildModifierIds();

    public static List<UUID> modifierIds() {
        return MODIFIER_IDS;
    }

    public static UUID modifierId(int stack) {
        return MODIFIER_IDS.get(stack);
    }

    public static String modifierName(int stack) {
        return MODIFIER_NAME + " " + (stack + 1) + "/" + MAX_TRIGGERS;
    }

    /** 这一次该挂第几条修饰符；四条都挂上了就是 {@link OptionalInt#empty()}。 */
    public static OptionalInt nextStack(Collection<UUID> applied) {
        for (int stack = 0; stack < MAX_TRIGGERS; stack++) {
            if (!applied.contains(MODIFIER_IDS.get(stack))) {
                return OptionalInt.of(stack);
            }
        }
        return OptionalInt.empty();
    }

    public static AttributeModifier modifier(int stack) {
        return new AttributeModifier(modifierId(stack), modifierName(stack), modifierAmount(),
                                     AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    /** 这一条修饰符的数值：{@link #LOSS_FRACTION} 取负，{@code MULTIPLY_TOTAL} 下即当前上限 × 0.75。 */
    public static double modifierAmount() {
        return -LOSS_FRACTION;
    }

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

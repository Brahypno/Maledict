package org.brahypno.maledict.common.curio;

import com.sammy.malum.registry.common.SoundRegistry;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.RuneOfRottenBoneItem;
import team.lodestar.lodestone.helpers.SoundHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * 「朽骨符文」唯一的出手点：{@code LivingDeathEvent}。
 *
 * <h2>一次死亡事件里的两条路</h2>
 * <ol>
 *   <li><b>接住</b>：戴着符文、上限大于 1、四条还没抽干，且这一击不是穿无敌的那一类
 *       （{@code /kill}、虚空——照原版图腾的写法）。</li>
 *   <li><b>放行</b>：接不住就让它死，并且把账清掉——「玩家死了就去掉这个 modifier」。</li>
 * </ol>
 * 顺序不能反：先清账再接住的话，每一次免死都会把之前的欠账一笔勾销，四次上限也就不存在了。
 *
 * <h2>为什么钩在 LivingDeathEvent 上</h2>
 * Forge 这个事件是在 {@code LivingEntity#die} 的<b>最开头</b>发的（玩家那边是
 * {@code ServerPlayer#die} 的第一句），取消掉就一路 return：死亡播报、掉落、死亡计数、
 * {@code dead = true} 全都不会发生。反过来，血量掉到 0 这件事本身已经发生了，
 * 所以接住之后必须自己把血设回去（{@link RottenBone#HEAL}）。
 *
 * <p>原版的图腾保护（{@code checkTotemDeathProtection}）走在 {@code die()} <em>之前</em>，
 * 所以手持图腾的玩家根本轮不到这枚符文——图腾先救命，账不用还。
 *
 * <h2>顺序照需求原文</h2>
 * 先扣上限、再给无敌帧、最后回血。扣完的上限才是回血的天花板：四条用完之后（基础上限 20 的玩家
 * 还剩 {@code 20 × 0.75⁴ ≈ 6.33}）回血落在 6.33 而不是 7，这是「四分之一扣四次」的自然结果。
 *
 * <p>修饰符是 {@code MULTIPLY_TOTAL} 的 {@link RottenBone#modifierAmount()}（负数），
 * 每次把<em>当前</em>上限乘 0.75；构造在 {@link RottenBone#modifier(int)} 里，运算方式与数值不会在调用点走散。
 *
 * <h2>四次上限从哪来</h2>
 * 乘算永远到不了 1，所以「最多四次免死」不是原版夹出来的，而是四条修饰符 id 用尽
 * （{@link RottenBone#nextStack}）——需求里那句「上限大于 1 才触发」仍然照写，只是它几乎不生效。
 *
 * <p>无敌帧就是 {@link net.minecraft.world.entity.Entity#invulnerableTime}：原版在
 * {@code > 10} 期间把不大于上一次的伤害整口吃掉，所以 70 tick 是实打实的 3.5 秒。
 * 玩家这一侧的递减在 {@code ServerPlayer#tick} 里（{@code LivingEntity#tick} 会跳过玩家）。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RottenBoneEvents {

    /** 接住一次死亡时的提示音：Malum 灵魂护盾被打空的那一声。 */
    private static final float SOUND_VOLUME = 1.0F;
    private static final float SOUND_PITCH = 1.0F;

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        if (catchDeath(event, victim)) {
            return;
        }
        clearDebt(victim);
    }

    /**
     * 试着用一条骨头接住这一死。
     *
     * @return {@code true} 表示接住了（事件已取消、上限已扣、血已回）；{@code false} 表示这一死放行
     */
    private static boolean catchDeath(LivingDeathEvent event, LivingEntity victim) {
        // 穿无敌的伤害（/kill、掉出世界）不接：原版图腾同样让路，否则管理员命令与虚空都会被这枚符文顶掉。
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (!RuneOfRottenBoneItem.isEquipped(victim)) {
            return false;
        }

        AttributeInstance maximumHealth = victim.getAttribute(Attributes.MAX_HEALTH);
        if (maximumHealth == null || !RottenBone.canSave(victim.getMaxHealth())) {
            return false;
        }
        OptionalInt stack = RottenBone.nextStack(appliedModifierIds(maximumHealth));
        if (stack.isEmpty()) {
            return false;
        }

        maximumHealth.addPermanentModifier(RottenBone.modifier(stack.getAsInt()));
        victim.invulnerableTime = RottenBone.INVULNERABLE_TICKS;
        victim.setHealth(RottenBone.HEAL);

        event.setCanceled(true);
        SoundHelper.playSound(victim, SoundRegistry.SOUL_WARD_DEPLETE.get(), SOUND_VOLUME, SOUND_PITCH);
        return true;
    }

    /**
     * 还清这笔账：摘下符文不算数，真正死一次才算。
     *
     * <p>死亡那一侧只管摘掉自己挂的修饰符——玩家复活走的是新实体，属性本来就不会跟过去，
     * 所以这里做的是「死的时候把上限还给他」这件事本身（死亡界面上、以及别的模组插手尸体时，
     * 看到的都是干净的上限）。
     */
    private static void clearDebt(LivingEntity victim) {
        AttributeInstance maximumHealth = victim.getAttribute(Attributes.MAX_HEALTH);
        if (maximumHealth == null) {
            return;
        }
        for (UUID id : RottenBone.modifierIds()) {
            maximumHealth.removeModifier(id);
        }
    }

    /** 属性上现有的修饰符 id；{@link RottenBone} 只认 id，看不到别的模组挂了什么。 */
    private static List<UUID> appliedModifierIds(AttributeInstance instance) {
        List<UUID> ids = new ArrayList<>();
        for (AttributeModifier modifier : instance.getModifiers()) {
            ids.add(modifier.getId());
        }
        return ids;
    }

    private RottenBoneEvents() {
    }
}

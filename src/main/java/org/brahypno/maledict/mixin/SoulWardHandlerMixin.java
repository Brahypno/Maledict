package org.brahypno.maledict.mixin;

import com.sammy.malum.core.handlers.SoulWardHandler;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.brahypno.maledict.common.refulgence.RefulgencePending;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 只观察 Malum 灵魂护盾在 {@code LivingHurtEvent} 里那次 {@code setAmount}，不替换它的任何逻辑。
 *
 * <p><b>为什么非得上 Mixin：</b>Malum 没有对外暴露「这一记护盾吸收了多少」。源码里的
 * {@code absorbed = amount - result} 只是局部变量，唯一的旁路 {@code IMalumEventResponderItem#onSoulwardAbsorbDamage}
 * 只发给 Malum 自己识别出的事件响应物品，普通 Tetra 镶嵌武器永远收不到。若不在这里取值，就只能在整个
 * 事件前后做差，而那会把 Lodestone 追加伤害、别的 {@code LivingHurtEvent} 监听器一起算成护盾吸收 ——
 * 这正是规范第 3.2 节点名要避免的误记账。
 *
 * <p>取的是「这一次调用本身让事件量减少了多少」：{@code setAmount} 之前与之后各看一眼事件。
 * 这与 Malum 源码里的 {@code absorbed} 等价，前提是没有别的响应物品在 Malum 方法内部先改过同一个
 * 事件（Malum 自带的实现只改倍率，不改事件）。如实测发现不等价，就改成捕获局部变量。
 *
 * <p>这里只登记待确认记录，真正充能发生在 {@code LivingHurtEvent} 的 {@code MONITOR}：事件若被取消，
 * 那一笔记账（以及已经发生的护盾扣减）就丢弃，不充能。
 */
@Mixin(SoulWardHandler.class)
public abstract class SoulWardHandlerMixin {
    private static final String SET_AMOUNT = "Lnet/minecraftforge/event/entity/living/LivingHurtEvent;setAmount(F)V";

    @Inject(method = "livingHurt",
            at = @At(value = "INVOKE", target = SET_AMOUNT, shift = At.Shift.BEFORE, remap = false),
            remap = false)
    private static void maledict$beforeSoulWardSetAmount(LivingHurtEvent event, CallbackInfo callback) {
        RefulgencePending.observeWardBefore(event);
    }

    @Inject(method = "livingHurt",
            at = @At(value = "INVOKE", target = SET_AMOUNT, shift = At.Shift.AFTER, remap = false),
            remap = false)
    private static void maledict$afterSoulWardSetAmount(LivingHurtEvent event, CallbackInfo callback) {
        RefulgencePending.observeWardAfter(event);
    }
}

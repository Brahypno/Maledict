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
 * 「朽骨符文」唯一的出手点：{@code LivingDeathEvent} 在 {@code die()} 的最开头发出，取消即整条 return，
 * 此时血量已经归零、必须自己设回去；只有接不住才清账，顺序反了的话每次免死都会把欠账一笔勾销。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RottenBoneEvents {

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

    private static boolean catchDeath(LivingDeathEvent event, LivingEntity victim) {
        // 穿无敌的伤害（/kill、掉出世界）不接：原版图腾同样让路。
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

    /** 真正死一次才算还清；只摘掉自己挂的那几条修饰符。 */
    private static void clearDebt(LivingEntity victim) {
        AttributeInstance maximumHealth = victim.getAttribute(Attributes.MAX_HEALTH);
        if (maximumHealth == null) {
            return;
        }
        for (UUID id : RottenBone.modifierIds()) {
            maximumHealth.removeModifier(id);
        }
    }

    /** 属性上现有的修饰符 id；{@link RottenBone} 只认 id，不关心别的模组挂了什么。 */
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

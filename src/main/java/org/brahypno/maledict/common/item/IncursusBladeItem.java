package org.brahypno.maledict.common.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.sammy.malum.common.item.curiosities.weapons.scythe.MagicScytheItem;
import com.sammy.malum.registry.common.DamageTypeRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.brahypno.changelib.DamageHelper.DamageProbe;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;

/** A Malum magic scythe whose per-stack combat values are stored in NBT. */
public final class IncursusBladeItem extends MagicScytheItem {
    public static final String STATS_TAG = "IncursusBladeStats";
    public static final String ATTACK_DAMAGE = "attack_damage";
    public static final String MAGIC_DAMAGE = "magic_damage";
    public static final String POWDER_SNOW_DAMAGE = "powder_snow_damage";
    public static final String STAT_4 = "stat_4";
    public static final String STAT_5 = "stat_5";
    public static final String STAT_6 = "stat_6";
    public static final String STAT_7 = "stat_7";
    public static final String STAT_8 = "stat_8";

    private static final String[] STAT_KEYS = {
            ATTACK_DAMAGE, MAGIC_DAMAGE, POWDER_SNOW_DAMAGE, STAT_4, STAT_5, STAT_6, STAT_7, STAT_8
    };
    private static final double[] STAT_DEFAULTS = {4.0, 4.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    public IncursusBladeItem(Tier tier, Item.Properties properties) {
        super(tier, -2.5f, 0.1f, 4.0f, properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getDefaultAttributeModifiers(slot);
        }

        ImmutableMultimap.Builder<Attribute, AttributeModifier> attributes = ImmutableMultimap.builder();
        attributes.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                BASE_ATTACK_DAMAGE_UUID, "Incursus blade damage", getStat(stack, ATTACK_DAMAGE) - 1.0,
                AttributeModifier.Operation.ADDITION));
        attributes.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                BASE_ATTACK_SPEED_UUID, "Incursus blade speed", -3.1,
                AttributeModifier.Operation.ADDITION));
        attributes.put(LodestoneAttributeRegistry.MAGIC_DAMAGE.get(), new AttributeModifier(
                LodestoneAttributeRegistry.UUIDS.get(LodestoneAttributeRegistry.MAGIC_DAMAGE),
                "Incursus blade magic damage", getStat(stack, MAGIC_DAMAGE),
                AttributeModifier.Operation.ADDITION));
        return attributes.build();
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        initializeStats(stack);
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        initializeStats(stack);
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    @Override
    public void hurtEvent(LivingHurtEvent event, LivingEntity attacker, LivingEntity target, ItemStack stack) {
        if (!attacker.level().isClientSide() && event.getSource().is(DamageTypeRegistry.SCYTHE_MELEE)) {
            double damage = getStat(stack, POWDER_SNOW_DAMAGE);
            if (damage > 0.0) {
                DamageProbe.mediumDamageMethod(
                        target,
                        DamageTypeHelper.create(attacker.level(), DamageTypes.FREEZE, attacker),
                        (float) damage);
            }
        }
    }

    public static void initializeStats(ItemStack stack) {
        CompoundTag stats = stack.getOrCreateTagElement(STATS_TAG);
        for (int i = 0; i < STAT_KEYS.length; i++) {
            if (!stats.contains(STAT_KEYS[i], Tag.TAG_ANY_NUMERIC)) {
                stats.putDouble(STAT_KEYS[i], STAT_DEFAULTS[i]);
            }
        }
    }

    public static double getStat(ItemStack stack, String key) {
        CompoundTag tag = stack.getTagElement(STATS_TAG);
        if (tag != null && tag.contains(key, Tag.TAG_ANY_NUMERIC)) {
            return tag.getDouble(key);
        }
        for (int i = 0; i < STAT_KEYS.length; i++) {
            if (STAT_KEYS[i].equals(key)) {
                return STAT_DEFAULTS[i];
            }
        }
        throw new IllegalArgumentException("Unknown Incursus Blade stat: " + key);
    }

    public static void setStat(ItemStack stack, String key, double value) {
        getStat(stack, key);
        stack.getOrCreateTagElement(STATS_TAG).putDouble(key, value);
    }
}

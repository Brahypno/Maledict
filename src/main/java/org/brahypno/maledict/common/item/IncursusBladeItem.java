package org.brahypno.maledict.common.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.sammy.malum.common.capability.MalumPlayerDataCapability;
import com.sammy.malum.common.item.curiosities.weapons.scythe.MagicScytheItem;
import com.sammy.malum.common.item.spirit.SpiritShardItem;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import com.sammy.malum.registry.common.AttributeRegistry;
import com.sammy.malum.registry.common.DamageTypeRegistry;
import com.sammy.malum.registry.common.DamageTypeTagRegistry;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.brahypno.changelib.DamageHelper.DamageProbe;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.network.MaledictNetwork;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;
import team.lodestar.lodestone.registry.common.tag.LodestoneDamageTypeTags;

/**
 * A Malum magic scythe whose per-stack combat values are stored in NBT.
 */
public final class IncursusBladeItem extends MagicScytheItem {
    public static final String STATS_TAG = "IncursusBladeStats";
    public static final String UPGRADE_PROGRESS_TAG = "IncursusBladeUpgradeProgress";
    public static final String ATTACK_DAMAGE = "attack_damage";
    public static final String MAGIC_DAMAGE = "magic_damage";
    public static final String POWDER_SNOW_DAMAGE = "powder_snow_damage";
    public static final String AERIAL_PROGRESS = "aerial_progress";
    public static final String SACRED_POWER = "sacred_power";
    public static final String INFERNAL_POWER = "infernal_power";
    public static final String ELDRITCH_ABSORPTION = "eldritch_absorption";
    public static final String WICKED_CRITICAL_DAMAGE = "wicked_critical_damage";

    private static final String[] STAT_KEYS = {
            ATTACK_DAMAGE, POWDER_SNOW_DAMAGE, MAGIC_DAMAGE, AERIAL_PROGRESS,
            SACRED_POWER, INFERNAL_POWER, ELDRITCH_ABSORPTION, WICKED_CRITICAL_DAMAGE
    };
    private static final double[] STAT_DEFAULTS = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

    public IncursusBladeItem(Tier tier, Item.Properties properties) {
        super(tier, -3.0f - tier.getAttackDamageBonus(), 0.1f, 0.0f, properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND){
            return super.getDefaultAttributeModifiers(slot);
        }

        ImmutableMultimap.Builder<Attribute, AttributeModifier> attributes = ImmutableMultimap.builder();
        attributes.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                BASE_ATTACK_DAMAGE_UUID, "Incursus blade damage", getStat(stack, ATTACK_DAMAGE),
                AttributeModifier.Operation.ADDITION));
        attributes.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                BASE_ATTACK_SPEED_UUID, "Incursus blade speed", -3.3,
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
    public boolean overrideOtherStackedOnMe(ItemStack blade, ItemStack carried, Slot slot, ClickAction action, Player player, SlotAccess carriedSlot) {
        if (action != ClickAction.SECONDARY || !(carried.getItem() instanceof SpiritShardItem spirit)){
            return false;
        }

        String statKey = getSpiritStatKey(spirit.type.identifier);
        if (statKey == null){
            return false;
        }
        if (player.level().isClientSide()){
            if (slot.container != player.getInventory()){
                return false;
            }
            if (player.isCreative()){
                absorbSpiritStack(blade, carried.copy());
                slot.setChanged();
            }
            MaledictNetwork.sendInfuseSpirit(
                    player.containerMenu.containerId,
                    slot.getContainerSlot(),
                    carried.copy());
        }
        return true;
    }

    public static boolean absorbSpiritStack(ItemStack blade, ItemStack carried) {
        if (!(blade.getItem() instanceof IncursusBladeItem)
            || !(carried.getItem() instanceof SpiritShardItem spirit)
            || carried.isEmpty()){
            return false;
        }
        String statKey = getSpiritStatKey(spirit.type.identifier);
        if (statKey == null){
            return false;
        }
        addUpgradeProgress(blade, statKey, carried.getCount());
        carried.setCount(0);
        return true;
    }

    @Override
    public void hurtEvent(LivingHurtEvent event, LivingEntity attacker, LivingEntity target, ItemStack stack) {
        if (attacker.level().isClientSide() || attacker.getMainHandItem() != stack){
            return;
        }

        boolean magicDamage = event.getSource().is(LodestoneDamageTypeTags.IS_MAGIC);
        boolean scytheDamage = event.getSource().is(DamageTypeTagRegistry.IS_SCYTHE);
        boolean powderSnowDamage = event.getSource().is(DamageTypes.FREEZE);
        if (!magicDamage && !scytheDamage && !powderSnowDamage){
            return;
        }

        if (magicDamage){
            event.setAmount(event.getAmount() * (float) attacker.getAttributeValue(AttributeRegistry.SCYTHE_PROFICIENCY.get()));
        }

        if (scytheDamage){
            double magicProficiency = attacker.getAttributeValue(
                    LodestoneAttributeRegistry.MAGIC_PROFICIENCY.get());
            double magicResistance = Math.max(0.01, target.getAttributeValue(
                    LodestoneAttributeRegistry.MAGIC_RESISTANCE.get()));
            event.setAmount(event.getAmount() * (float) (magicProficiency / magicResistance));
        }

        if (event.getSource().is(DamageTypeRegistry.SCYTHE_MELEE)){
            double damage = getStat(stack, POWDER_SNOW_DAMAGE);
            if (damage > 0.0){
                DamageProbe.mediumDamageMethod(
                        target,
                        DamageTypeHelper.create(attacker.level(), DamageTypes.FREEZE, attacker),
                        (float) damage);
            }
        }

        addAbsorptionFromDamage(attacker, stack, event.getAmount());
    }

    private static void addAbsorptionFromDamage(
            LivingEntity attacker,
            ItemStack stack,
            float damage) {
        double absorptionPercent = getStat(stack, ELDRITCH_ABSORPTION);
        if (absorptionPercent <= 0.0 || damage <= 0.0f){
            return;
        }
        float absorptionCap = (float) absorptionPercent;
        float gainedAbsorption = damage * (float) (absorptionPercent / 100.0);
        float currentAbsorption = attacker.getAbsorptionAmount();
        float newAbsorption = Math.min(absorptionCap, currentAbsorption + gainedAbsorption);
        if (newAbsorption > currentAbsorption){
            attacker.setAbsorptionAmount(newAbsorption);
        }
        if (attacker instanceof Player player){
            MalumPlayerDataCapability.getCapability(player).soulWardHandler.soulWardProgress = 0.0;
        }
    }

    private static String getSpiritStatKey(String spiritType) {
        return switch (spiritType) {
            case "earthen" -> ATTACK_DAMAGE;
            case "aqueous" -> POWDER_SNOW_DAMAGE;
            case "arcane" -> MAGIC_DAMAGE;
            case "aerial" -> AERIAL_PROGRESS;
            case "sacred" -> SACRED_POWER;
            case "infernal" -> INFERNAL_POWER;
            case "eldritch" -> ELDRITCH_ABSORPTION;
            case "wicked" -> WICKED_CRITICAL_DAMAGE;
            default -> null;
        };
    }

    public static void initializeStats(ItemStack stack) {
        CompoundTag stats = stack.getOrCreateTagElement(STATS_TAG);
        for (int i = 0; i < STAT_KEYS.length; i++) {
            if (!stats.contains(STAT_KEYS[i], Tag.TAG_ANY_NUMERIC)){
                stats.putDouble(STAT_KEYS[i], STAT_DEFAULTS[i]);
            }
        }
    }

    public static double getStat(ItemStack stack, String key) {
        CompoundTag tag = stack.getTagElement(STATS_TAG);
        if (tag != null && tag.contains(key, Tag.TAG_ANY_NUMERIC)){
            return tag.getDouble(key);
        }
        for (int i = 0; i < STAT_KEYS.length; i++) {
            if (STAT_KEYS[i].equals(key)){
                return STAT_DEFAULTS[i];
            }
        }
        throw new IllegalArgumentException("Unknown Incursus Blade stat: " + key);
    }

    public static void setStat(ItemStack stack, String key, double value) {
        getStat(stack, key);
        stack.getOrCreateTagElement(STATS_TAG).putDouble(key, value);
    }

    public static int getUpgradeProgress(ItemStack stack, String key) {
        getStat(stack, key);
        CompoundTag progress = stack.getTagElement(UPGRADE_PROGRESS_TAG);
        return progress == null ? 0 : progress.getInt(key);
    }

    public static int getNextUpgradeCost(ItemStack stack, String key) {
        double coefficient = switch (key) {
            case ATTACK_DAMAGE -> MaledictConfig.EARTHEN_UPGRADE_COST_COEFFICIENT.get();
            case POWDER_SNOW_DAMAGE -> MaledictConfig.AQUEOUS_UPGRADE_COST_COEFFICIENT.get();
            case MAGIC_DAMAGE -> MaledictConfig.ARCANE_UPGRADE_COST_COEFFICIENT.get();
            case AERIAL_PROGRESS -> MaledictConfig.AERIAL_UPGRADE_COST_COEFFICIENT.get();
            case SACRED_POWER -> MaledictConfig.SACRED_UPGRADE_COST_COEFFICIENT.get();
            case INFERNAL_POWER -> MaledictConfig.INFERNAL_UPGRADE_COST_COEFFICIENT.get();
            case ELDRITCH_ABSORPTION -> MaledictConfig.ELDRITCH_UPGRADE_COST_COEFFICIENT.get();
            case WICKED_CRITICAL_DAMAGE -> MaledictConfig.WICKED_UPGRADE_COST_COEFFICIENT.get();
            default -> throw new IllegalArgumentException("Unknown Incursus Blade stat: " + key);
        };
        return (int) (coefficient * Math.max(1, (int) Math.floor(getStat(stack, key)) + 1));
    }

    private static void addUpgradeProgress(ItemStack stack, String key, int amount) {
        int progress = getUpgradeProgress(stack, key) + amount;
        int cost = getNextUpgradeCost(stack, key);
        while (progress >= cost) {
            progress -= cost;
            setStat(stack, key, getStat(stack, key) + 1.0);
            cost = getNextUpgradeCost(stack, key);
        }
        stack.getOrCreateTagElement(UPGRADE_PROGRESS_TAG).putInt(key, progress);
    }

    @Override
    public MalumSpiritType getDefiningSpiritType() {
        return SpiritTypeRegistry.UMBRAL_SPIRIT;
    }
}

package org.brahypno.maledict.common.item.weapon.incursus;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.sammy.malum.common.capability.MalumPlayerDataCapability;
import com.sammy.malum.common.item.curiosities.weapons.scythe.MagicScytheItem;
import com.sammy.malum.common.item.spirit.SpiritShardItem;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import com.sammy.malum.registry.common.AttributeRegistry;
import com.sammy.malum.registry.common.DamageTypeTagRegistry;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import com.sammy.malum.registry.common.item.EnchantmentRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.brahypno.changelib.DamageHelper.DamageProbe;
import org.brahypno.changelib.DamageHelper.DamageProbeResult;
import org.brahypno.changelib.DamageHelper.DamagePolicy;
import org.brahypno.changelib.DamageHelper.DamageStage;
import org.brahypno.changelib.HealthHelper.CombatProtection;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.equipment.lightward.LightwardSet;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.network.MaledictNetwork;
import org.brahypno.maledict.registry.MaledictAttributes;
import org.jetbrains.annotations.Nullable;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;
import team.lodestar.lodestone.registry.common.tag.LodestoneDamageTypeTags;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A Malum magic scythe whose per-stack combat values are stored in NBT.
 */
public final class IncursusBladeItem extends MagicScytheItem {
    public static final double MEDIUM_DAMAGE_LEVEL = 7.0;
    public static final double FINAL_DAMAGE_LEVEL = 11.0;
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

    /**
     * 主手冻结属性修饰符的稳定 id；刀 NBT 的成长值只是这个修饰符的来源。
     */
    public static final UUID POWDER_SNOW_DAMAGE_UUID =
            UUID.fromString("e07eecfa-4019-4c60-8e61-49597f30b3fc");

    /**
     * 物品栏里眨眼用的模型覆盖谓词：{@code 1.0} 表示这一帧闭眼。
     */
    public static final ResourceLocation BLINK_PROPERTY =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "blinking");

    private static final String[] STAT_KEYS = {
            ATTACK_DAMAGE, POWDER_SNOW_DAMAGE, MAGIC_DAMAGE, AERIAL_PROGRESS,
            SACRED_POWER, INFERNAL_POWER, ELDRITCH_ABSORPTION, WICKED_CRITICAL_DAMAGE
    };
    private static final double[] STAT_DEFAULTS = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

    public IncursusBladeItem(Tier tier, Item.Properties properties) {
        super(tier, -3.0f - tier.getAttackDamageBonus(), 0.1f, 0.0f, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getEnchantmentLevel(EnchantmentRegistry.REBOUND.get()) > 0){
            IncursusBladeEnchantments.throwScythe(level, player, hand, stack);
            return InteractionResultHolder.success(stack);
        }
        if (stack.getEnchantmentLevel(EnchantmentRegistry.ASCENSION.get()) > 0){
            IncursusBladeEnchantments.triggerAscension(level, player, hand, stack);
            return InteractionResultHolder.success(stack);
        }
        return super.use(level, player, hand);
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
        attributes.put(MaledictAttributes.POWDER_SNOW_DAMAGE.get(), new AttributeModifier(
                POWDER_SNOW_DAMAGE_UUID, "Incursus blade freezing damage",
                getStat(stack, POWDER_SNOW_DAMAGE), AttributeModifier.Operation.ADDITION));
        return attributes.build();
    }

    @Override
    public int getEnchantmentLevel(ItemStack stack, Enchantment enchantment) {
        return LightwardSet.boostedEnchantmentLevel(stack, enchantment);
    }

    @Override
    public Map<Enchantment, Integer> getAllEnchantments(ItemStack stack) {
        return LightwardSet.boostedEnchantments(stack);
    }

    @Override
    public int getDefaultTooltipHideFlags(ItemStack stack) {
        return ItemStack.TooltipPart.ENCHANTMENTS.getMask();
    }

    @Override
    public void appendHoverText(
            ItemStack stack, @Nullable Level level, List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // 套装文字只在护甲上；这里只补附魔行。
        LightwardSet.appendEnchantments(stack, tooltip);
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

        // getAttributeValue() 在属性没注册到该实体时返回 0，而熟练度当乘数、抗性当除数，0 都是灾难；
        // 所以这里取 getAttribute() 判空：null 才表示「这个实体没有这条属性」，缺了就跳过该倍率。

        if (magicDamage){
            event.setAmount(event.getAmount() * (float) scytheProficiencyOrNeutral(
                    attacker.getAttributeValue(AttributeRegistry.SCYTHE_PROFICIENCY.get())));
        }

        if (scytheDamage){
            AttributeInstance proficiency = attacker.getAttribute(LodestoneAttributeRegistry.MAGIC_PROFICIENCY.get());
            if (proficiency != null){
                event.setAmount(event.getAmount() * (float) proficiency.getValue());
            }
            AttributeInstance resistance = target.getAttribute(LodestoneAttributeRegistry.MAGIC_RESISTANCE.get());
            if (resistance != null){
                event.setAmount(event.getAmount() / (float) Math.max(0.01, resistance.getValue()));
            }
        }

        addAbsorptionFromDamage(attacker, stack, event.getAmount());
    }

    /**
     * 镰刀熟练度缺省时当中性 1：它走 {@code getAttributeValue}，取不到给的是 0，而它当乘数用。
     */
    private static double scytheProficiencyOrNeutral(double value) {
        return value > 0.0 ? value : 1.0;
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

    public static boolean hasAllStatsAtLeast(ItemStack stack, double level) {
        for (String key : STAT_KEYS) {
            if (getStat(stack, key) < level){
                return false;
            }
        }
        return true;
    }

    /**
     * 神侵恶刃的三档伤害：轻 → 中 → 终。随喂进去的精魂成长，见 {@link #MEDIUM_DAMAGE_LEVEL}
     * 与 {@link #FINAL_DAMAGE_LEVEL}（提示语就是「精魂之力到达神圣之数会解锁更本征的力量」）。
     */
    public enum DamageTier {
        /**
         * 未满 {@link #MEDIUM_DAMAGE_LEVEL}：探针只试必要的那几步，拿到真实掉血就收手。
         */
        LIGHT,
        /**
         * 满 {@link #MEDIUM_DAMAGE_LEVEL}：会为凑够数值继续往下找路。
         */
        MEDIUM,
        /**
         * 满 {@link #FINAL_DAMAGE_LEVEL}：终结档。
         */
        FINAL;

        /**
         * 本档在普通目标上的 ChangeLib 探针上限与穿透。
         *
         * <p>LIGHT 与 MEDIUM 在探针层是同一档（medium 梯级、D3、尊重防御）；两者的差别只在
         * 自有血池的 {@link #internalAttackGrade}。FINAL 才是 final 梯级（D4、忽略声明）。
         * 冻结属性请求也按这张表起步，再由无忧符文的事件监听抬一级。
         */
        public DamageStage probeStage() {
            return this == FINAL ? DamageStage.D4_CONSISTENCY_WRITE : DamageStage.D3_STORAGE_WRITE;
        }

        public DamageProbeResult.Penetration probeStrength() {
            return this == FINAL ? DamageProbeResult.Penetration.IGNORE_DECLARATION
                                 : DamageProbeResult.Penetration.RESPECT;
        }

        /**
         * 按本档打一记。
         */
        public DamageProbeResult deal(Entity target, DamageSource source, float damage) {
            return deal(target, source, damage, false);
        }

        /**
         * 以本档的常规探针结算。
         */
        public DamageProbeResult deal(Entity target, DamageSource source, float damage, boolean compensateDamage) {
            return switch (this) {
                case FINAL -> DamageProbe.finalDamageMethod(target, source, damage, compensateDamage);
                case MEDIUM -> DamageProbe.mediumDamageMethod(target, source, damage, compensateDamage);
                case LIGHT -> DamageProbe.lighterDamageMethod(target, source, damage, compensateDamage);
            };
        }

        /**
         * 独立指定对 Maledict 自有血池可尝试的最高 ChangeLib 等级。
         */
        public DamageProbeResult deal(
                Entity target, DamageSource source, float damage, boolean compensateDamage,
                DamageStage internalGrade) {
            if (target instanceof LivingEntity living && living instanceof CombatProtection.OwnedHealthEntity
                && target.getType().builtInRegistryHolder().key().location().getNamespace().equals(Maledict.MODID)){
                DamageStage defense = CombatProtection.defenseGrade(living);
                if (defense != null && internalGrade.penetrates(defense)){
                    return DamageProbe.damage(target, source, damage, DamagePolicy.through(internalGrade));
                }
            }
            return deal(target, source, damage, compensateDamage);
        }
    }

    /**
     * 神侵恶刃各成长档的内部等级配置。
     */
    private static DamageStage internalAttackGrade(DamageTier tier) {
        return switch (tier) {
            case LIGHT -> DamageStage.D3_STORAGE_WRITE;
            case MEDIUM -> DamageStage.D4_CONSISTENCY_WRITE;
            case FINAL -> DamageStage.D5_RECOVERY_SOURCE;
        };
    }

    /**
     * 刀自己的精魂档。无忧符文不在这里判断 —— 它的抬档统一挂在
     * {@code DamageRequestEvent} 上，所以这把刀不需要认识符文。
     */
    public static DamageTier damageTier(ItemStack stack) {
        return hasAllStatsAtLeast(stack, FINAL_DAMAGE_LEVEL) ? DamageTier.FINAL
                                                             : hasAllStatsAtLeast(stack, MEDIUM_DAMAGE_LEVEL) ? DamageTier.MEDIUM
                                                                                                              : DamageTier.LIGHT;
    }

    /**
     * 按当前档位打一记，需要知道走了哪一档的调用方用这个。
     */
    public static DamageProbeResult dealTieredDamage(ItemStack stack, Entity target, DamageSource source, float damage) {
        return dealTieredDamage(stack, target, source, damage, false);
    }

    public static DamageProbeResult dealTieredDamage(
            ItemStack stack, Entity target, DamageSource source,
            float damage, boolean compensateDamage) {
        DamageTier tier = damageTier(stack);
        return tier.deal(target, source, damage, compensateDamage, internalAttackGrade(tier));
    }

    /**
     * 投掷返程 / 飞升横扫写死 medium；无忧符文的抬档由探针事件统一处理。
     */
    static DamageProbeResult dealSkillDamage(Entity target, DamageSource source, float damage) {
        return DamageTier.MEDIUM.deal(target, source, damage, false,
                                      internalAttackGrade(DamageTier.MEDIUM));
    }

    /**
     * 按成长档结算一次镰刀伤害。
     */
    public static void applyTieredDamage(ItemStack stack, Entity target, DamageSource source, float damage) {
        applyTieredDamage(stack, target, source, damage, false);
    }

    public static void applyTieredDamage(
            ItemStack stack, Entity target, DamageSource source, float damage,
            boolean compensateDamage) {
        if (damage <= 0.0f || !canTakeDamage(target))
            return;
        dealTieredDamage(stack, target, source, damage, compensateDamage);
    }

    private static boolean canTakeDamage(Entity target) {
        return !(target instanceof LivingEntity livingTarget) || !livingTarget.isDeadOrDying();
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

    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {return true;}

    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
        return Math.min(amount, 20);
    }
}

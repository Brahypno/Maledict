package org.brahypno.maledict.common.curio.eggshell;

import com.google.common.collect.Multimap;
import com.sammy.malum.common.item.curiosities.curios.MalumCurioItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeMod;
import org.brahypno.changelib.DamageHelper.DamageStage;
import org.brahypno.changelib.HealthHelper.CombatProtection;
import org.brahypno.maledict.config.MaledictConfig;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * 「蛋壳中的皇帝」：佩戴时挂一层 ChangeLib 的限伤 ward，受击后另开一段渐变的蛋壳窗口。
 *
 * <p>限伤：{@link CombatProtection#guard} 的 D4 层（{@link DamageStage#D4_CONSISTENCY_WRITE}），
 * 单次受击最多扣 {@link MaledictConfig#EMPEROR_EGGSHELL_GUARD_PERCENTAGE} × 最大生命值。它在
 * {@code actuallyHurt} 入口生效，也就是原版无敌帧之后、护甲/吸收/写血之前。
 *
 * <p>窗口：不替换原版无敌帧，原版 20 tick 照旧在前，窗口在 {@code LivingHurtEvent} 里对漏进来的
 * 伤害按 {@code elapsed / N} 缩放。{@code N} 由 {@link MaledictConfig#EMPEROR_EGGSHELL_WINDOW_TICKS}
 * 配置，且 {@code N <= 20} 时门会被原版帧盖住，所以配置下限是 21。两者互不替代：限伤是单次上限，
 * 窗口是一条时间衰减曲线，同一次命中会先后经过两边。
 *
 * <p>负面：佩戴时实体和方块交互距离都按基础值 -50%。固定 UUID 让多枚同饰品不会叠加到零。
 *
 * <p><b>ward 按实体存放，不能是物品字段。</b>物品是全局单例，一个字段会在第二个穿戴者装备时被覆盖，
 * 让第一个人的层永远关不掉（撤下装备后依旧减伤），而第二个人的层会被第一个人的 unequip 提前关掉。
 */
public final class EmperorInTheEggshellItem extends MalumCurioItem {
    private static final String EFFECT = "maledict.emperor_in_the_eggshell.window";
    private static final String GUARD_EFFECT = "maledict.emperor_in_the_eggshell.guard";
    private static final double REACH_PENALTY = -0.5D;
    private static final UUID BLOCK_REACH_MODIFIER = UUID.nameUUIDFromBytes(
            "maledict:emperor_in_the_eggshell/block_reach".getBytes(StandardCharsets.UTF_8));
    private static final UUID ENTITY_REACH_MODIFIER = UUID.nameUUIDFromBytes(
            "maledict:emperor_in_the_eggshell/entity_reach".getBytes(StandardCharsets.UTF_8));

    /**
     * 每具穿戴者一层的限伤 ward，只在服务端线程读写。弱键：实体没了，条目自己消失。
     */
    private static final Map<LivingEntity, CombatProtection.Guard> WARDS = new WeakHashMap<>();

    public EmperorInTheEggshellItem() {
        // Charm slot. VOID also gives the endgame trinket equip sound; the slot itself comes from
        // the curios:charm item tag in MaledictItemTags.
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), MalumTrinketType.VOID);
    }

    @Override
    public void addAttributeModifiers(
            Multimap<Attribute, AttributeModifier> attributeMap,
            SlotContext slotContext, ItemStack stack) {
        attributeMap.put(ForgeMod.BLOCK_REACH.get(), new AttributeModifier(
                BLOCK_REACH_MODIFIER, "maledict:emperor_in_the_eggshell.block_reach",
                REACH_PENALTY, AttributeModifier.Operation.MULTIPLY_TOTAL));
        attributeMap.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(
                ENTITY_REACH_MODIFIER, "maledict:emperor_in_the_eggshell.entity_reach",
                REACH_PENALTY, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    @Override
    public void addExtraTooltipLines(Consumer<Component> tooltip) {
        tooltip.accept(positiveEffect(EFFECT, MaledictConfig.EMPEROR_EGGSHELL_WINDOW_TICKS.get()));
        tooltip.accept(positiveEffect(GUARD_EFFECT, MaledictConfig.EMPEROR_EGGSHELL_GUARD_PERCENTAGE.get() * 100f));
    }

    /**
     * Queried on demand, like the other passive curios; the window keeps no per-wearer state of its own.
     */
    public static boolean isEquipped(@Nullable LivingEntity entity) {
        if (entity == null){
            return false;
        }
        return CuriosApi.getCuriosInventory(entity)
                        .map(handler -> handler.isEquipped(stack -> stack.getItem() instanceof EmperorInTheEggshellItem))
                        .orElse(false);
    }

    /**
     * Installs this wearer's damage cap. A slot move fires equip again without an unequip in between,
     * so the previous layer is closed first; two layers would stack into a smaller effective cap.
     */
    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        if (wearer == null || wearer.level().isClientSide) {
            return;
        }
        releaseWard(wearer);
        float cap = damageCap(wearer);
        // A zero cap would refuse damage entirely, which is not what a 0 in the config means.
        if (!(cap > 0.0F) || !Float.isFinite(cap)) {
            return;
        }
        WARDS.put(wearer, CombatProtection.guard(wearer, DamageStage.D4_CONSISTENCY_WRITE).cap(cap));
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        if (wearer == null || wearer.level().isClientSide) {
            return;
        }
        releaseWard(wearer);
    }

    /** Closes every layer this item installed, for a server shutdown; guards do not survive a world. */
    static void clearWards() {
        for (CombatProtection.Guard ward : WARDS.values()) {
            ward.close();
        }
        WARDS.clear();
    }

    /** The health one hit may remove: the configured share of the wearer's own maximum health. */
    private static float damageCap(LivingEntity wearer) {
        return MaledictConfig.EMPEROR_EGGSHELL_GUARD_PERCENTAGE.get().floatValue() * wearer.getMaxHealth();
    }

    private static void releaseWard(LivingEntity wearer) {
        CombatProtection.Guard ward = WARDS.remove(wearer);
        if (ward != null) {
            ward.close();
        }
    }
}

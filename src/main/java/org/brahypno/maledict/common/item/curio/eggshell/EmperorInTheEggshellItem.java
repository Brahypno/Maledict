package org.brahypno.maledict.common.item.curio.eggshell;

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
import top.theillusivec4.curios.api.SlotContext;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 「蛋壳中的皇帝」：佩戴时挂一层 ChangeLib 的 guard，单次上限与时间窗口都在这层上。
 *
 * <p>限伤：{@link CombatProtection#guard} 的 D4 层（{@link DamageStage#D4_CONSISTENCY_WRITE}），
 * 单次受击最多扣 {@link MaledictConfig#EMPEROR_EGGSHELL_GUARD_PERCENTAGE} × 最大生命值。百分比由
 * {@link CombatProtection.Guard#capFraction} 在每次判定上限时现算，所以穿戴期间最大生命变化
 * （生命饰品、buff、等级模组）立刻生效，不需要重新装备，也不需要 tick 刷新。
 *
 * <p>窗口：同一层上的 {@link CombatProtection.Guard#ramp}，宽度是
 * {@link MaledictConfig#EMPEROR_EGGSHELL_WINDOW_TICKS}。一击真正扣到血之后，窗口内的命中只能拿走
 * {@code elapsed / N} 那一份，窗口自己不会被窗口内的命中延长。它是防御的一部分而不是事件过滤器：
 * 结算发生在扣血处，所以普通命中、D1 直调 {@code actuallyHurt}、以及绕开受伤通道直接写血看到的是同一条曲线。
 * {@code N} 的配置下限是 21，因为原版 20 tick 的无敌帧仍在前面与它串联。
 *
 * <p>负面：佩戴时实体和方块交互距离都按基础值 -50%。固定 UUID 让多枚同饰品不会叠加到零。
 *
 * <p><b>层用固定 key 挂在实体身上，物品不带任何按实体的状态。</b>物品是全局单例，一个字段会在第二个
 * 穿戴者装备时被覆盖，让第一个人的层永远关不掉（撤下装备后依旧减伤），而第二个人的层会被第一个人的
 * unequip 提前关掉。同一实体重复注册同一个 key 会替换上一层，所以槽位移动、重复装备都只留一层上限。
 * 层不跨实例：装备/卸下由 Curios 的 tick 差异检测触发，重生的新实例在 clone 后重新触发 onEquip。
 */
public final class EmperorInTheEggshellItem extends MalumCurioItem {
    private static final String EFFECT = "maledict.emperor_in_the_eggshell.window";
    private static final String GUARD_EFFECT = "maledict.emperor_in_the_eggshell.guard";
    /**
     * 这层的固定 key：重复装备会替换旧层而不是叠出第二层，卸下时按它找回同一层。
     */
    static final String WARD_KEY = "maledict:emperor_in_the_eggshell/guard";
    private static final double REACH_PENALTY = -0.5D;
    private static final UUID BLOCK_REACH_MODIFIER = UUID.nameUUIDFromBytes(
            "maledict:emperor_in_the_eggshell/block_reach".getBytes(StandardCharsets.UTF_8));
    private static final UUID ENTITY_REACH_MODIFIER = UUID.nameUUIDFromBytes(
            "maledict:emperor_in_the_eggshell/entity_reach".getBytes(StandardCharsets.UTF_8));

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
     * Installs this wearer's layer: one per-hit cap and one time window, both keyed so a repeated equip
     * replaces the previous layer instead of stacking a second ceiling.
     */
    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        if (wearer == null || wearer.level().isClientSide){
            return;
        }
        installWard(wearer);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        if (wearer == null || wearer.level().isClientSide){
            return;
        }
        releaseWard(wearer);
    }

    /**
     * Re-reads both configured knobs and re-registers the layer, so a config reload is applied without
     * touching the wearer.
     *
     * <p>A zero share is not a zero cap — that would refuse damage entirely — so it leaves the layer
     * uncapped and keeps the window, which used to be an independent event handler.
     */
    static void installWard(LivingEntity wearer) {
        CombatProtection.Guard ward =
                CombatProtection.guard(wearer, DamageStage.D4_CONSISTENCY_WRITE, WARD_KEY);
        float share = MaledictConfig.EMPEROR_EGGSHELL_GUARD_PERCENTAGE.get().floatValue();
        if (share > 0.0F && Float.isFinite(share)){
            ward.capFraction(share);
        }
        // The window is the same layer's time gate: a hit inside it may take elapsed / N of its loss,
        // capped by the share above. Both ceilings are independent.
        ward.ramp(MaledictConfig.EMPEROR_EGGSHELL_WINDOW_TICKS.get());
    }

    static void releaseWard(LivingEntity wearer) {
        CombatProtection.Guard ward = CombatProtection.findGuard(wearer, WARD_KEY);
        if (ward != null){
            ward.close();
        }
    }
}

package org.brahypno.maledict.common.item.curio.eggshell;

import static org.brahypno.maledict.common.item.EffectTooltipComponents.*;

import com.google.common.collect.Multimap;
import com.sammy.malum.common.item.curiosities.curios.MalumCurioItem;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.brahypno.changelib.DamageHelper.DamageStage;
import org.brahypno.changelib.HealthHelper.CombatProtection;
import org.brahypno.maledict.config.MaledictConfig;
import top.theillusivec4.curios.api.SlotContext;

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
 * <p>负面：佩戴时实体和方块交互距离都按基础值 -50%。固定 id 让多枚同饰品不会叠加到零。
 *
 * <p><b>层用固定 key 挂在实体身上，物品不带任何按实体的状态。</b>物品是全局单例，一个字段会在第二个
 * 穿戴者装备时被覆盖，让第一个人的层永远关不掉（撤下装备后依旧减伤），而第二个人的层会被第一个人的
 * unequip 提前关掉。同一实体重复注册同一个 key 会替换上一层，所以槽位移动、重复装备都只留一层上限。
 * 层不跨实例：装备/卸下由 Curios 的 tick 差异检测触发，重生的新实例在 clone 后重新触发 onEquip。
 *
 * <p>1.21 侧的三处改写：交互距离吃原版的 {@code player.block_interaction_range} /
 * {@code player.entity_interaction_range}（NeoForge 21.1 已经删掉自己的 reach 属性，
 * {@code NeoForgeMod} 里只剩游泳速度、名牌距离与创造飞行三条）；{@code AttributeModifier} 认身份改用
 * {@link ResourceLocation}；{@code MULTIPLY_TOTAL} 改名 {@code ADD_MULTIPLIED_TOTAL}。
 */
public final class EmperorInTheEggshellItem extends MalumCurioItem {
    /** 只是后缀：{@link org.brahypno.maledict.common.item.EffectTooltipComponents#positiveEffect} 会自己拼上前缀。 */
    private static final String WINDOW_EFFECT = "maledict.emperor_in_the_eggshell.window";
    private static final String GUARD_EFFECT = "maledict.emperor_in_the_eggshell.guard";
    /**
     * 这层的固定 key：重复装备会替换旧层而不是叠出第二层，卸下时按它找回同一层。
     */
    static final String WARD_KEY = "maledict:emperor_in_the_eggshell/guard";
    /**
     * 这层的自锁令牌，只有本物品持有。带上它才能替换/撤掉这一层；别人拿到 key 也关不掉，
     * 外部 {@code unprotect} / {@code authorize} 也会被 ChangeLib 拒掉。
     */
    private static final Object WARD_OWNER = new Object();
    private static final double REACH_PENALTY = -0.5D;
    /** 修饰符 id 由物品名算死：跨存档一致，重复加载不会叠成两条。 */
    private static final ResourceLocation BLOCK_REACH_MODIFIER =
            ResourceLocation.fromNamespaceAndPath("maledict", "emperor_in_the_eggshell/block_reach");
    private static final ResourceLocation ENTITY_REACH_MODIFIER =
            ResourceLocation.fromNamespaceAndPath("maledict", "emperor_in_the_eggshell/entity_reach");

    public EmperorInTheEggshellItem() {
        // 护符槽（charm）。VOID 同时给出后期饰品的装备音效；槽位本身来自 MaledictItemTags 里的
        // curios:charm 物品标签。
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), MalumTrinketType.VOID);
    }

    @Override
    public void addAttributeModifiers(
            Multimap<Holder<Attribute>, AttributeModifier> attributeMap,
            SlotContext slotContext, ItemStack stack) {
        attributeMap.put(Attributes.BLOCK_INTERACTION_RANGE, new AttributeModifier(
                BLOCK_REACH_MODIFIER, REACH_PENALTY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        attributeMap.put(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(
                ENTITY_REACH_MODIFIER, REACH_PENALTY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    @Override
    public void addExtraTooltipLines(Consumer<Component> tooltip) {
        tooltip.accept(positiveEffect(WINDOW_EFFECT, MaledictConfig.EMPEROR_EGGSHELL_WINDOW_TICKS.get()));
        tooltip.accept(positiveEffect(GUARD_EFFECT, MaledictConfig.EMPEROR_EGGSHELL_GUARD_PERCENTAGE.get() * 100f));
    }

    /**
     * 装上佩戴者的这一层：单次上限与时间窗都挂上去，并自锁。
     *
     * <p>1.21 的 Curios 签名与 1.20.1 参数顺序一致（{@code slotContext, prevStack, stack}），
     * 只有 {@code ICurio} 那一侧的两参版本与之并存，覆写仍落在三参的 {@code ICurioItem} 上。
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
     * 重读两个配置旋钮并重新注册这一层，所以改配置不需要碰佩戴者。
     *
     * <p>比例为 0 不是「上限为 0」（那会完全拒绝伤害），而是让这一层不设上限、只保留时间窗 ——
     * 那个窗口原本是独立的事件处理器，现在同属一层。
     */
    static void installWard(LivingEntity wearer) {
        CombatProtection.Guard ward =
                CombatProtection.guard(wearer, DamageStage.D4_CONSISTENCY_WRITE, WARD_KEY, WARD_OWNER);
        float share = MaledictConfig.EMPEROR_EGGSHELL_GUARD_PERCENTAGE.get().floatValue();
        if (share > 0.0F && Float.isFinite(share)){
            ward.capFraction(share);
        }
        // 时间窗是同一层上的时间闸：窗口内的命中只能拿走「已经过的时间 / 窗口宽度」那一份，
        // 再被上面的上限压一次。两个天花板互相独立。
        ward.ramp(MaledictConfig.EMPEROR_EGGSHELL_WINDOW_TICKS.get());
        // 自锁：这一层只有本物品能替换或撤掉，别的模组既摘不掉也封不住。
        CombatProtection.seal(wearer, WARD_OWNER);
    }

    static void releaseWard(LivingEntity wearer) {
        CombatProtection.Guard ward = CombatProtection.findGuard(wearer, WARD_KEY);
        if (ward != null){
            ward.close(WARD_OWNER);
        }
    }
}

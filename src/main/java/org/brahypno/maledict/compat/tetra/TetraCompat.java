package org.brahypno.maledict.compat.tetra;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import se.mickelus.tetra.effect.ItemEffect;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.modular.ModularItem;
import se.mickelus.tetra.module.data.ImprovementData;

/**
 * Tetra 是可选前置，而且这里只负责「把能力暴露到 Tetra 工具上」这一条渠道：公共代码只经过
 * {@link MaledictAbilities}，{@code se.mickelus.tetra} 的类型全部关在 {@link LoadedOnly} 里，
 * 而它只在 {@link #loaded} 为真之后才会被第一次触碰。没装 Tetra 时 Maledict 照常启动，
 * 载体判定退回 {@code MaledictTags} 那两条物品标签 —— 能力本身不依赖 Tetra。
 *
 * <p>能力<b>只</b>认工具最终的效果等级（{@code getEffectLevel > 0}），不靠物品名、材料 ID 或者
 * 「背包里有沉辉铋锭」去猜。沉辉锭与共晶在 {@code data/tetra/materials/} 里挂了多少效果，
 * 工具上就要真的攒出多少效果，中间没有第二条路。
 */
public final class TetraCompat {
    private static final String MOD_ID = "tetra";

    private static boolean loaded;

    /** 由 {@code Maledict} 构造时调用一次；ModList 在构造期已可用。 */
    public static void init() {
        loaded = ModList.get().isLoaded(MOD_ID);
    }

    public static boolean loaded() {
        return loaded;
    }

    public static boolean hasLumenfall(ItemStack stack) {
        return loaded && LoadedOnly.hasEffect(stack, LoadedOnly.LUMENFALL);
    }

    public static boolean hasRefulgence(ItemStack stack) {
        return loaded && LoadedOnly.hasEffect(stack, LoadedOnly.REFULGENCE);
    }

    /**
     * 模块化工具自己给出的属性修饰（攻击力在模块上，不在物品的修饰表里）。
     * 没装 Tetra 或不是模块化物品时返回空表，由 {@code MaledictAbilities} 决定怎么兜底。
     */
    public static Multimap<Attribute, AttributeModifier> moduleAttributes(ItemStack stack) {
        return loaded ? LoadedOnly.moduleAttributes(stack) : ImmutableMultimap.of();
    }

    /**
     * 这件工具最终攒出的某个效果等级；没装 Tetra、不是模块化物品或没有该效果时返回 0。
     * 与 {@link #hasLumenfall}／{@link #hasRefulgence} 共用同一条判定，供界面展示使用。
     */
    public static int effectLevel(ItemStack stack, String effectKey) {
        return loaded ? LoadedOnly.effectLevel(stack, effectKey) : 0;
    }

    /**
     * 这件工具的强化列表里有没有某个键。
     *
     * <p>用来避免界面重复：强化会由 Tetra 自己列在工具提示里（剑身那种主模块），而插槽这类
     * 不允许强化的模块只能在我们的提示里补一行 —— 见 {@code ModularAbilityTooltipEvents}。
     */
    public static boolean hasImprovement(ItemStack stack, String improvementKey) {
        return loaded && LoadedOnly.hasImprovement(stack, improvementKey);
    }

    private static final class LoadedOnly {
        private static final ItemEffect LUMENFALL = ItemEffect.get("maledict.lumenfall");
        private static final ItemEffect REFULGENCE = ItemEffect.get("maledict.refulgence");

        private static boolean hasEffect(ItemStack stack, ItemEffect effect) {
            return stack.getItem() instanceof ModularItem modular && modular.getEffectLevel(stack, effect) > 0;
        }

        private static Multimap<Attribute, AttributeModifier> moduleAttributes(ItemStack stack) {
            return stack.getItem() instanceof ModularItem modular
                    ? modular.getAttributeModifiers(stack)
                    : ImmutableMultimap.of();
        }

        private static int effectLevel(ItemStack stack, String effectKey) {
            return stack.getItem() instanceof ModularItem modular
                    ? modular.getEffectLevel(stack, ItemEffect.get(effectKey))
                    : 0;
        }

        private static boolean hasImprovement(ItemStack stack, String improvementKey) {
            if (!(stack.getItem() instanceof IModularItem modular)) {
                return false;
            }
            for (ImprovementData improvement : modular.getImprovements(stack)) {
                if (improvementKey.equals(improvement.key)) {
                    return true;
                }
            }
            return false;
        }
    }

    private TetraCompat() {
    }
}

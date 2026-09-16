package org.brahypno.maledict.common.item;

import com.google.common.collect.Multimap;
import com.sammy.malum.common.item.IVoidItem;
import com.sammy.malum.common.item.curiosities.curios.MalumCurioItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.curio.EnlightenmentLevel;
import org.brahypno.maledict.registry.MaledictMobEffects;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 启蒙之年：护符（Curios 的 charm 槽），戴上后在头部渲染一张面具。
 *
 * <p>渲染部分见 {@code AgeOfEnlightenmentCurioRenderer}，几何见
 * {@code AgeOfEnlightenmentModel}，贴图由 {@code art/age-of-enlightenment/tools} 下的生成器产出。
 *
 * <p>物品栏里的黑光来自 {@link IVoidItem}：本类实现它，{@code MaledictScreenParticles}
 * 再把本物品注册进 Lodestone 的屏幕粒子表，Malum 的 {@code spawnVoidItemScreenParticles}
 * 就会在格子周围铺一层虚空噪点。这与神侵恶刃走的是同一条路。
 *
 * <p>玩法见 {@code AgeOfEnlightenmentEvents}：冷却速度加倍是戴上就有的常驻效果（见
 * {@code CooldownSpeed}），与伤害、击杀都无关；攻击半血生物时刷一圈魂息虚空并当场收获精魂，
 * 击杀则刷新启蒙之年，同时把黑暗年代丢给附近的一名敌人；被标记的敌人再挨打时，
 * 黑暗年代会继续传染给下一名敌人。
 *
 * <p>击杀给出的启蒙之年与黑暗年代读同一个等级——这枚护符 NBT 上的
 * {@link EnlightenmentLevel#TAG}，见 {@link #equippedLevel}。
 *
 * <h2>为什么用 Malum 的 {@link MalumCurioItem} 当基类</h2>
 * 不是为了少打几个字，而是这件饰品需要的两件东西它本来就有：
 * <ul>
 *   <li>{@link #addExtraTooltipLines} 配 {@link #positiveEffect}：Malum 那套 {@code +效果}
 *       的排版（表头、蓝字、翻译键）全在基类里，我们不用自己拼前缀格式；</li>
 *   <li>{@link #addAttributeModifiers}：槽位修饰符的注入点。基类的
 *       {@code getAttributeModifiers} 是 {@code final} 的，它建好 Multimap 再回调这里，
 *       我们只管往里加——Malum 的玻璃胸针送的符文槽也是这么写的。</li>
 * </ul>
 * 顺带白拿右击即佩戴（{@code canEquipFromUse}）和 {@code VOID} 那套虚空装备音效。
 */
public final class AgeOfEnlightenmentItem extends MalumCurioItem implements IVoidItem {

    /** Curios 的护符槽标识；本饰品自身就装在 charm 里，额外送的那个也是 charm。 */
    private static final String CHARM_SLOT = "charm";

    /**
     * 槽位修饰符的 UUID：由物品名算死。
     *
     * <p>这件饰品 {@code stacksTo(1)}，同一玩家身上最多一件，固定值不会撞车；顺带让效果在
     * 存档之间保持一致，不会每次加载都换一个身份。
     */
    private static final UUID SLOT_MODIFIER_ID =
            UUID.nameUUIDFromBytes("maledict:age_of_enlightenment/charm_slot".getBytes(StandardCharsets.UTF_8));

    /**
     * 效果行的翻译键前缀。
     *
     * <p>它<b>不是</b>完整的键：Malum 的 {@link #positiveEffect} 会自己拼上
     * {@code malum.gui.curio.effect.}，所以这里只给后缀，最终键是
     * {@code malum.gui.curio.effect.maledict.age_of_enlightenment.*}——与 Malum 自己的饰品
     * 同住一个命名空间，和法典条目挂在 {@code malum.gui.book.entry.maledict.*} 下是同一个路子。
     * 下面那三行 Shift 说明走的是自己的 {@code tooltip.maledict.*}，不受这个前缀影响。
     */
    private static final String EFFECT_PREFIX = "maledict.age_of_enlightenment.";
    private static final String HOLD_SHIFT = "tooltip.maledict.age_of_enlightenment.hold_shift";
    private static final String SHIFT_LINE = "tooltip.maledict.age_of_enlightenment.shift";
    private static final String ENLIGHTENED_SHIFT_LINE = "tooltip.maledict.age_of_enlightenment.shift.enlightened";

    /** 模型覆盖用的物品属性：{@code 1.0} 表示佩戴者身上有启蒙之年，切到第二张贴图。 */
    public static final ResourceLocation ENLIGHTENED_PROPERTY =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "enlightened");

    public AgeOfEnlightenmentItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC), MalumTrinketType.VOID);
    }

    /**
     * 佩戴时多给一个护符槽位。
     *
     * <p>Curios 的槽位加成走的是「槽位属性」：{@link CuriosApi#addSlotModifier} 把
     * {@code +1 charm} 挂到这件物品的属性表上，Curios 自己负责在戴上/摘下时结算槽位数量。
     *
     * <p>tooltip 里那行「+1 护符槽位」由 Curios 生成，不需要我们写，也不要覆盖
     * {@code getSlotsTooltip}——重复写会出两行。
     */
    @Override
    public void addAttributeModifiers(Multimap<Attribute, AttributeModifier> attributeMap,
                                      SlotContext slotContext, ItemStack stack) {
        CuriosApi.addSlotModifier(attributeMap, CHARM_SLOT, SLOT_MODIFIER_ID, 1.0D,
                AttributeModifier.Operation.ADDITION);
    }

    /**
     * Malum 风格的饰品说明：{@code +效果} 蓝字，前缀直接借 Malum 自己的 {@link #positiveEffect}。
     *
     * <p>传进 {@code positiveEffect} 的只是后缀，完整的键由它补成
     * {@code malum.gui.curio.effect.…}，见 {@link #EFFECT_PREFIX}——三个键写在
     * {@code MaledictLanguage} 里，名字对不上就会原样把键打到 tooltip 上。
     *
     * <p>只有按住 Shift 才会多出那句紫字诘问——参考 Malum 法典里「虚空」卷的语气，
     * 也给这枚护符一个自己的注脚。佩戴者身上有启蒙之年时换成另一句：
     * 「黑暗的时代曾经存在过吗」是站在黑暗里问的，「那无穷，无限，永动的启蒙之年啊」
     * 是拿到答案之后说的。
     */
    @Override
    public void addExtraTooltipLines(Consumer<Component> tooltip) {
        tooltip.accept(positiveEffect(EFFECT_PREFIX + "cooldown"));
        tooltip.accept(positiveEffect(EFFECT_PREFIX + "spirit_void"));
        tooltip.accept(positiveEffect(EFFECT_PREFIX + "enlightenment"));

        if (!Screen.hasShiftDown()) {
            tooltip.accept(Component.translatable(HOLD_SHIFT).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.accept(Component.translatable(hasEnlightenment() ? ENLIGHTENED_SHIFT_LINE : SHIFT_LINE)
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    /**
     * 本地玩家身上是否挂着启蒙之年。
     *
     * <p>刻意只看本地玩家：这里问的是「我这里现在是什么状态」，而不是「这个物品属于谁」。
     * 看物品主人的话，在箱子里、展示框里或者别人的背包里就会露馅。
     *
     * <p>客户端之外（专用服务器）永远返回 false，所以 tooltip 逻辑在服务端跑也不会去碰客户端类。
     */
    private static boolean hasEnlightenment() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return false;
        }
        Player player = Minecraft.getInstance().player;
        return player != null && player.hasEffect(MaledictMobEffects.AGE_OF_ENLIGHTENMENT.get());
    }

    /**
     * 玩家饰品栏里有没有这枚护符。
     *
     * <p>用 {@code isEquipped} 而不是自己遍历 {@code getCurios()}：Curios 自己处理槽位解锁、
     * 数量修正和「物品被无常临时没收」这类移出，直接问它比我们猜更准。
     */
    public static boolean isEquipped(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return CuriosApi.getCuriosInventory(entity)
                .map(handler -> handler.isEquipped(stack -> stack.getItem() instanceof AgeOfEnlightenmentItem))
                .orElse(false);
    }

    /**
     * 佩戴者身上这枚护符写的等级，见 {@link EnlightenmentLevel}。
     *
     * <p>击杀时给出的启蒙之年与黑暗年代都用它当等级：一件饰品只有一个等级，两种效果同步。
     *
     * <p>没戴护符同样是 0 级。调用方都先问过 {@link #isEquipped}，这里不必再区分
     * 「没戴」与「戴了但没写等级」——两者的处理都是退路 0 级。
     */
    public static int equippedLevel(LivingEntity entity) {
        return EnlightenmentLevel.fromTag(equippedStack(entity).getTag());
    }

    /**
     * 饰品栏里那枚护符本身。
     *
     * <p>{@link #isEquipped} 只问有没有，交给 Curios 自己的谓词最省事；这里要的是栈本身
     * （等级写在它身上），所以照 Curios 的槽位表走一遍。遍历方式与
     * {@code VicissitudeCurioReturns} 一致，槽位标识符排序与否都不影响结果——
     * 这件饰品 {@code stacksTo(1)}，场上最多一枚。
     */
    private static ItemStack equippedStack(LivingEntity entity) {
        if (entity == null) {
            return ItemStack.EMPTY;
        }
        ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(entity).orElse(null);
        if (inventory == null) {
            return ItemStack.EMPTY;
        }
        for (ICurioStacksHandler stacks : inventory.getCurios().values()) {
            IDynamicStackHandler slots = stacks.getStacks();
            for (int slot = 0; slot < slots.getSlots(); slot++) {
                ItemStack stack = slots.getStackInSlot(slot);
                if (stack.getItem() instanceof AgeOfEnlightenmentItem) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }
}

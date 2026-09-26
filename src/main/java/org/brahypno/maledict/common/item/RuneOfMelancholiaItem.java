package org.brahypno.maledict.common.item;

import com.sammy.malum.common.item.curiosities.curios.runes.AbstractRuneCurioItem;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.brahypno.maledict.common.entity.DamageAdaptation;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 「抑郁符文」：痛觉的第一半 —— 适应。
 *
 * <p>连着挨同一种伤害会越来越钝，换一种伤害打进来就把账目挤掉、重新敏感。窗口容量是
 * {@value #ADAPTATION_LEVEL}，算法整个复用无常的 {@link DamageAdaptation}（先记录、再判定），
 * 区别只在「消息」怎么取：无常按死法 id 记账，这里按**伤害类型 + 来源生物种类**记账，所以僵尸和
 * 蠹虫是两条消息，而一群僵尸共享同一条消息 —— 这正是「被一群僵尸围着打会打不动」的来源。
 *
 * <p>账目以 NBT 存在符文自己身上，不挂 capability：摘下来再戴上、被无常没收、被别的玩家捡走，
 * 账目都跟着物品走；佩戴者死亡时清空。
 */
public final class RuneOfMelancholiaItem extends AbstractRuneCurioItem {

    private static final String EFFECT_SUFFIX = "maledict.melancholia";

    /** 适应几：窗口只记得最近挨过的那一条消息。 */
    public static final int ADAPTATION_LEVEL = 1;

    private static final String ADAPTATION_TAG = "MelancholiaAdaptation";

    /** 窗口里的消息，顺序即「最新在前」。 */
    private static final String WINDOW_TAG = "Window";

    /** 还在窗口里的消息各挨过几次。 */
    private static final String HITS_TAG = "Hits";

    /** 没有来源的伤害（摔落、火、毒）只有类型这一半，用空串占位，键里照样不会和别的撞上。 */
    private static final String NO_SOURCE = "";

    public RuneOfMelancholiaItem(Item.Properties properties, MalumSpiritType spiritType) {
        super(properties, spiritType);
    }

    @Override
    public void addExtraTooltipLines(Consumer<Component> tooltip) {
        tooltip.accept(positiveEffect(EFFECT_SUFFIX));
    }

    /** 佩戴中的那一枚；没戴返回 {@code null}。账目要写在具体的 stack 上，所以这里返回物品本身。 */
    @Nullable
    public static ItemStack equipped(LivingEntity wearer) {
        if (wearer == null) {
            return null;
        }
        ICuriosItemHandler handler = CuriosApi.getCuriosInventory(wearer).resolve().orElse(null);
        if (handler == null) {
            return null;
        }
        return handler.findFirstCurio(stack -> stack.getItem() instanceof RuneOfMelancholiaItem)
                      .map(SlotResult::stack)
                      .orElse(null);
    }

    public static boolean isEquipped(LivingEntity wearer) {
        return equipped(wearer) != null;
    }

    /**
     * 一记伤害的「消息」：伤害类型 + 来源生物种类。
     *
     * <p>{@code getMsgId()} 是死法的 id，不是攻击者的 id（僵尸与蠹虫都是 {@code mob}），所以必须
     * 把来源也算进去；取**种类**而不是个体，才会让一群同类共享同一条消息。
     */
    public static String message(DamageSource source) {
        Entity sourceEntity = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        EntityType<?> type = sourceEntity == null ? null : sourceEntity.getType();
        String typeId = type == null ? NO_SOURCE : EntityType.getKey(type).toString();
        return source.getMsgId() + "|" + typeId;
    }

    /**
     * 为这一记伤害记账并给出倍率。符文不在身上时不记账，返回 {@code 1}。
     */
    public static float adaptationMultiplier(LivingEntity wearer, DamageSource source) {
        ItemStack rune = equipped(wearer);
        if (rune == null) {
            return 1.0F;
        }
        DamageAdaptation adaptation = read(rune);
        float multiplier = adaptation.adapt(message(source), ADAPTATION_LEVEL);
        write(rune, adaptation);
        return multiplier;
    }

    /** 佩戴者死亡时清空账目。 */
    public static void clearAdaptation(LivingEntity wearer) {
        ItemStack rune = equipped(wearer);
        if (rune != null) {
            rune.removeTagKey(ADAPTATION_TAG);
        }
    }

    private static DamageAdaptation read(ItemStack rune) {
        DamageAdaptation adaptation = new DamageAdaptation();
        CompoundTag tag = rune.getTagElement(ADAPTATION_TAG);
        if (tag == null) {
            return adaptation;
        }
        ListTag window = tag.getList(WINDOW_TAG, Tag.TAG_STRING);
        List<String> messages = new ArrayList<>(window.size());
        for (int index = 0; index < window.size(); index++) {
            messages.add(window.getString(index));
        }
        CompoundTag hits = tag.getCompound(HITS_TAG);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String message : hits.getAllKeys()) {
            counts.put(message, hits.getInt(message));
        }
        adaptation.restore(messages, counts);
        return adaptation;
    }

    private static void write(ItemStack rune, DamageAdaptation adaptation) {
        List<String> window = adaptation.snapshot();
        if (window.isEmpty()) {
            rune.removeTagKey(ADAPTATION_TAG);
            return;
        }

        ListTag messages = new ListTag();
        for (String message : window) {
            messages.add(StringTag.valueOf(message));
        }

        Map<String, Integer> counts = adaptation.hitCounts();
        CompoundTag hits = new CompoundTag();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            hits.putInt(entry.getKey(), entry.getValue());
        }

        CompoundTag tag = new CompoundTag();
        tag.put(WINDOW_TAG, messages);
        tag.put(HITS_TAG, hits);
        rune.addTagElement(ADAPTATION_TAG, tag);
    }
}

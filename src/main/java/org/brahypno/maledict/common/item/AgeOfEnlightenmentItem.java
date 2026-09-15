package org.brahypno.maledict.common.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * 启蒙之年：护符（Curios 的 charm 槽），戴上后在头部渲染一张面具。
 *
 * <p>渲染部分见 {@code AgeOfEnlightenmentCurioRenderer}，几何见
 * {@code AgeOfEnlightenmentModel}，贴图由 {@code art/age-of-enlightenment/tools} 下的生成器产出。
 * 具体效果待定，目前只负责装备与外观。
 */
public final class AgeOfEnlightenmentItem extends Item implements ICurioItem {
    public AgeOfEnlightenmentItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }
}

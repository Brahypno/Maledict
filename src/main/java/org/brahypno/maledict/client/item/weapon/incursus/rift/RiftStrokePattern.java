package org.brahypno.maledict.client.item.weapon.incursus.rift;

import java.util.List;

/**
 * 一种刀痕布局。
 *
 * <p>四种模式共用同一套裂隙材质与 RenderType，差别只在"哪几道、什么朝向、几时出生"。
 * 实现方在构造时就拿齐了报文与配置里的数值，因此 {@link #generate()} 是纯函数：
 * 同一组输入永远产生同一批 {@link RiftStroke}，且不触碰世界、网络或 GPU。
 */
public interface RiftStrokePattern {

    /**
     * 生成这一次效果的全部刀痕，顺序即构造顺序（{@code variant} 也按它取值）。
     */
    List<RiftStroke> generate();
}

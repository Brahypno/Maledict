package org.brahypno.maledict.client.item.weapon.incursus.rift;

/**
 * 一次裂隙效果的锚点语义。
 *
 * <p>普通挥砍的腰环属于挥砍者：他跳起、被击退、上台阶，花心都该跟着走，否则刀光会留在原地。
 * Malum 的飞腾与隐藏锋刃连斩相反——它们是一次性在触发时的世界坐标生成、随后各自飞走的粒子群，
 * 迁移到裂隙渲染后必须保持这个"钉在世界里"的手感。
 */
public enum RiftAnchorMode {
    /**
     * 每帧重取施法者本地插值后的腰部位置；实体缺席时退回报文里的世界坐标。
     */
    FOLLOW_OWNER,
    /**
     * 固定于触发时的世界坐标，不再查询施法者。
     */
    FIXED_WORLD
}

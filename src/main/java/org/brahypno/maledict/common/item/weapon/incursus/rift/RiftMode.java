package org.brahypno.maledict.common.item.weapon.incursus.rift;

/**
 * 一次裂隙斩击的布局模式。
 *
 * <p>放在公共包而不是 {@code client}：{@code IncursusRiftSkillPacket} 与服务端的
 * {@code HiddenBladeNecklaceRiftMixin} 都要引用它，这两处都不能加载 {@code Minecraft}、
 * {@code RenderType}、{@code ShaderInstance} 这类客户端专属类。具体怎么画由客户端
 * {@code IncursusRiftClientManager} 按模式挑 pattern。
 */
public enum RiftMode {
    /**
     * 普通挥砍的八瓣水平腰环。仍走旧报文 {@code IncursusSlashPacket}，锚点跟着挥砍者。
     */
    MELEE_RING,
    /**
     * 飞腾普通旋斩：Malum {@code AscensionRadialSlashParticleEffect} 的 3 层 × 16 道。
     */
    ASCENSION_SPIN,
    /**
     * 强化飞腾上挑：Malum {@code AscensionUppercutParticleEffect} 的 6 层 × 2 道。
     */
    ASCENSION_UPPERCUT,
    /**
     * 隐藏锋刃项链连斩：Malum {@code HiddenBladeCounterParticleEffect} 的 3 波 × 100 道。
     */
    HIDDEN_BLADE_FLURRY
}

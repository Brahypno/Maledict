package org.brahypno.maledict.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;

/**
 * 本模组的声音事件。
 *
 * <p>事件 id 带点号（{@code music.vicissitude.phase_one}）没问题，原版自己就是
 * {@code music.game} / {@code music.credits}。
 *
 * <p><b>必须用 {@link SoundEvent#createVariableRangeEvent}</b>：定长事件默认只有 16 格半径，
 * 虽然音乐走 {@code Attenuation.NONE} 不吃距离衰减，但语义上它就该是可变距离的。
 *
 * <p>对应的 {@code assets/maledict/sounds.json} 与两个 .ogg 是手写/转码进来的，
 * **不在 `src/generated` 里，不要交给 runData**。改文件名就得同时改 sounds.json 和这里。
 */
public final class MaledictSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Maledict.MODID);

    /** 一阶段（高位圣像）：人声圣咏，126 秒，不循环。 */
    public static final RegistryObject<SoundEvent> VICISSITUDE_MUSIC_PHASE_ONE =
            register("music.vicissitude.phase_one");
    /** 二阶段（前倾的执行者）：《骷髅之舞》去掉引子后的部分，374 秒，循环。 */
    public static final RegistryObject<SoundEvent> VICISSITUDE_MUSIC_PHASE_TWO =
            register("music.vicissitude.phase_two");

    public static final RegistryObject<SoundEvent> RAVEN_AMBIENT =
            register("entity.raven.ambient");
    public static final RegistryObject<SoundEvent> RAVEN_HURT =
            register("entity.raven.hurt");
    public static final RegistryObject<SoundEvent> RAVEN_DEATH =
            register("entity.raven.death");
    public static final RegistryObject<SoundEvent> RAVEN_STEP =
            register("entity.raven.step");
    public static final RegistryObject<SoundEvent> RAVEN_FLY =
            register("entity.raven.fly");

    /** 神侵恶刃：脱手（Rebound 投掷）。 */
    public static final RegistryObject<SoundEvent> INCURSUS_BLADE_THROW =
            register("item.incursus_blade.throw");
    /** 神侵恶刃：收回（飞镰掉头回手的那一刻）。 */
    public static final RegistryObject<SoundEvent> INCURSUS_BLADE_RECALL =
            register("item.incursus_blade.recall");
    /** 神侵恶刃：飞腾（Ascension 起跳旋斩）。 */
    public static final RegistryObject<SoundEvent> INCURSUS_BLADE_ASCENSION =
            register("item.incursus_blade.ascension");
    /** 神侵恶刃：攻击（每次挥砍）。 */
    public static final RegistryObject<SoundEvent> INCURSUS_BLADE_SLASH =
            register("item.incursus_blade.slash");
    /** 神侵恶刃：暴击（一次挥砍里至少打出一个暴击时补一层）。 */
    public static final RegistryObject<SoundEvent> INCURSUS_BLADE_CRIT =
            register("item.incursus_blade.crit");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(Maledict.MODID, name)));
    }

    private MaledictSounds() {
    }
}

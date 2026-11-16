package org.brahypno.maledict.common.combat.lumenfall;

import com.sammy.malum.visual_effects.networked.ParticleEffectType;
import com.sammy.malum.visual_effects.networked.data.ColorEffectData;
import com.sammy.malum.visual_effects.networked.data.NBTEffectData;
import com.sammy.malum.visual_effects.networked.data.PositionEffectData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.brahypno.maledict.client.combat.lumenfall.LumenfallRadianceActor;

import java.util.function.Supplier;

/**
 * 带强度参数的放射光晕，用来替换 Malum 哭泣之井那发写死强度的 {@code WEEPING_WELL_EMITS_RADIANCE}。
 *
 * <p>复用 Malum 的整套网络管道，不新增包：{@link ParticleEffectType} 的构造器会自己把实例登记进
 * {@code ParticleEffectTypeRegistry.EFFECT_TYPES}，发包时只带 id + 位置 + 颜色 + {@link NBTEffectData}，
 * 客户端按 id 取回本类再由 {@link #get()} 交给渲染侧。强度就放在 NBT 的
 * {@value #STRENGTH} 键里 —— Malum 自己的效果不读这个字段，所以这是我们的扩展位。
 *
 * <p><b>类加载边界：</b>本类在服务端也会被实例化（只为了登记），而真正的粒子代码在
 * {@link LumenfallRadianceActor}（客户端专用）。{@link #get()} 只在客户端被调用，lambda 里的方法引用
 * 那时才解析，所以专用服务器不会去加载客户端类 —— 与 Malum 自己的效果类同一个套路。
 */
public final class LumenfallRadianceEffect extends ParticleEffectType {
    public static final String ID = "maledict_lumenfall_radiance";

    /**
     * {@link NBTEffectData} 里承载强度（0..1）的键。
     */
    public static final String STRENGTH = "strength";

    /**
     * {@link NBTEffectData} 里承载空间铺开范围倍率的键。
     */
    public static final String RADIUS = "radius";

    private static LumenfallRadianceEffect instance;

    /**
     * 两端启动时各调一次；构造器已经把实例放进 Malum 的效果表。
     */
    public static synchronized void register() {
        if (instance == null){
            instance = new LumenfallRadianceEffect();
        }
    }

    public static LumenfallRadianceEffect instance() {
        register();
        return instance;
    }

    private LumenfallRadianceEffect() {
        super(ID);
    }

    @Override
    public Supplier<ParticleEffectActor> get() {
        return () -> LumenfallRadianceEffect::act;
    }

    private static void act(
            Level level, RandomSource random, PositionEffectData position,
            ColorEffectData color, NBTEffectData nbt) {
        float strength = nbt != null && nbt.compoundTag.contains(STRENGTH)
                         ? nbt.compoundTag.getFloat(STRENGTH)
                         : 1.0F;
        float radius = nbt != null && nbt.compoundTag.contains(RADIUS)
                       ? nbt.compoundTag.getFloat(RADIUS)
                       : 1.0F;
        LumenfallRadianceActor.spawn(level, random, position, strength, radius);
    }

    /**
     * 把强度与范围倍率一起打进 NBT，供 {@link #act} 取回。
     */
    public static NBTEffectData payload(float strength, float radius) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat(STRENGTH, strength);
        tag.putFloat(RADIUS, radius);
        return new NBTEffectData(tag);
    }
}

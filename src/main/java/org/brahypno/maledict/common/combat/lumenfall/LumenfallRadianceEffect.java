package org.brahypno.maledict.common.combat.lumenfall;

import com.sammy.malum.visual_effects.networked.MalumNetworkedParticleEffectColorData;
import com.sammy.malum.visual_effects.networked.MalumNetworkedParticleEffectType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.brahypno.maledict.client.combat.lumenfall.LumenfallRadianceActor;
import team.lodestar.lodestone.systems.network.particle.NetworkedParticleEffectExtraData;
import team.lodestar.lodestone.systems.network.particle.NetworkedParticleEffectPositionData;

import java.util.Optional;

/**
 * 带强度参数的放射光晕，用来替换 Malum 哭泣之井那发写死强度的光晕。
 *
 * <p>复用 Malum 的整套网络管道，不新增包：{@code NetworkedParticleEffectType} 的构造器会自己把实例登记进
 * {@code NetworkedParticleEffectType#EFFECT_TYPES}（见父类），发包时只带 id + 位置 + 颜色 + {@link Radiance}，
 * 客户端按 id 取回本类再由 {@link #act} 交给渲染侧。强度与铺开范围就放在 {@link Radiance} 里 ——
 * Malum 自己的效果不读我们的扩展位，所以这是纯 Maledict 的参数。
 *
 * <p><b>类加载边界：</b>本类在服务端也会被实例化（只为了登记），而真正的粒子代码在
 * {@link LumenfallRadianceActor}（客户端专用）。{@link #act} 只在客户端被调用，方法体里对客户端类的引用
 * 那一刻才解析，所以专用服务器不会去加载客户端类 —— 与 Malum 自己的效果类同一个套路。
 */
public final class LumenfallRadianceEffect extends MalumNetworkedParticleEffectType<LumenfallRadianceEffect.Radiance> {
    public static final String ID = "maledict_lumenfall_radiance";

    private static LumenfallRadianceEffect instance;

    /**
     * 两端启动时各调一次；构造器已经把实例放进 Malum 的效果表。重复调用无副作用。
     */
    public static synchronized void register() {
        if (instance == null) {
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
    public Optional<StreamCodec<ByteBuf, ? extends NetworkedParticleEffectExtraData>> getExtraCodec() {
        return Optional.of(Radiance.STREAM_CODEC);
    }

    @Override
    public void act(
            Level level, RandomSource random, NetworkedParticleEffectPositionData position,
            MalumNetworkedParticleEffectColorData color, Radiance radiance) {
        LumenfallRadianceActor.spawn(level, random, position, radiance.strength(), radiance.radius());
    }

    /**
     * 强度（粒子多少与大小，0..1）与铺开范围（飞多远，倍率）一起过线。
     *
     * <p>1.20.1 那边是打进 NBT 的两个键；1.8.2 的扩展数据是泛型 + {@code StreamCodec}，所以改成这个 record。
     * 两个值都是必填：效果类型一定带 {@code customData}，缺了会在发包时直接抛错，不会静默变成 0。
     */
    public record Radiance(float strength, float radius) implements NetworkedParticleEffectExtraData {
        public static final StreamCodec<ByteBuf, Radiance> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, Radiance::strength,
                ByteBufCodecs.FLOAT, Radiance::radius,
                Radiance::new);
    }
}

package org.brahypno.maledict.common.combat.lumenfall;

import com.sammy.malum.registry.common.ParticleEffectTypeRegistry;
import com.sammy.malum.visual_effects.networked.data.ColorEffectData;
import com.sammy.malum.visual_effects.networked.data.PositionEffectData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.config.MaledictConfig;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 沉辉的表现层：只派发 Malum 已经带着客户端同步的既有特效，不新增粒子类型、不新增网络包。
 *
 * <p>配色是沉辉铋的浅虹彩：{@code A9B8EF} 蓝紫银作主色、{@code E29BDA} 虹彩粉紫作折光，
 * 光点在两者之间渐变；{@code 76D6D0} 青绿留作描边反光，用在放射光晕的三色轮转里
 * （见 {@code LumenfallRadianceActor}）。
 *
 * <ul>
 *   <li>单体释放（80 tick 到期、没打死的那个目标）：一记<b>缩小版</b>放射光晕，也就是击杀那一下
 *       的单人小号版；不叠光点，免得比原来那层还大。</li>
 *   <li>死亡散辉：每个被波及目标一束 {@code CONCENTRATED_RISING_SPARKLES} 光点，死亡位置再放
 *       一份放射光晕（{@code LumenfallRadianceEffect}）。两者强度分别可配，见 {@link MaledictConfig}。</li>
 * </ul>
 *
 * <p>Malum 的 {@code ParticleEffectType} 没有「数量/尺寸/强度」参数 —— 放射光晕那边连
 * {@code ColorEffectData} 与 {@code NBTEffectData} 都不读，单发效果多大完全由效果类型决定。
 * 所以能调的只有：放不放、多久最多放一次、以及（必要时）换成别的效果类型。
 */
public final class LumenfallVisuals {
    /**
     * 主色 {@code A9B8EF}：蓝紫银。
     */
    private static final Color PRIMARY = new Color(0xA9B8EF);
    /**
     * 折光色 {@code E29BDA}：虹彩粉紫。
     */
    private static final Color SECONDARY = new Color(0xE29BDA);

    /**
     * 每个维度上一次放射光晕的 tick；只给冷却用，条目数等于维度数。
     */
    private static final Map<ResourceLocation, Long> RADIANCE_TICK = new HashMap<>();

    /**
     * 单体释放：只有一份<b>缩小版</b>的放射光晕 —— 击杀那一下的单人版。强度（粒子量）与范围
     * （铺开半径）都另有配置：{@link MaledictConfig#LUMENFALL_RELEASE_RADIANCE_STRENGTH}、
     * {@link MaledictConfig#LUMENFALL_RELEASE_RADIANCE_RADIUS}。
     *
     * <p>这里刻意<b>不</b>再叠一层光点：释放本来就只有一个目标，两层加起来的观感比原来那层还大，
     * 与「缩小」正好相反。强度设成 0 则释放没有粒子表现。
     */
    public static void release(LivingEntity victim) {
        float strength = MaledictConfig.LUMENFALL_RELEASE_RADIANCE_STRENGTH.get().floatValue();
        if (strength <= 0.0F){
            return;
        }
        LumenfallRadianceEffect.instance().createEntityEffect(
                victim, new ColorEffectData(PRIMARY, SECONDARY),
                LumenfallRadianceEffect.payload(
                        strength,
                        MaledictConfig.LUMENFALL_RELEASE_RADIANCE_RADIUS.get().floatValue()));
    }

    /**
     * 死亡散辉：每个被波及目标一束残光，死亡位置按条件放一层放射光晕。
     */
    public static void spread(Level level, Vec3 position, List<LivingEntity> targets) {
        // 一次散辉往往波及一圈目标：颜色数据只编一次，逐目标复用，别每人都 new 一份。
        ColorEffectData color = new ColorEffectData(PRIMARY, SECONDARY);
        for (LivingEntity target : targets) {
            ParticleEffectTypeRegistry.CONCENTRATED_RISING_SPARKLES.createEntityEffect(target, color);
        }
        if (shouldPlayRadiance(level, targets)){
            // 自己那份可调强度的放射光晕：Malum 哭泣之井那发写死了强度，这里把强度与铺开范围
            // 打进 NBT 传过去。散辉是群伤，范围保持满值 1.0。
            LumenfallRadianceEffect.instance().createPositionedEffect(
                    level, new PositionEffectData(position), color,
                    LumenfallRadianceEffect.payload(
                            MaledictConfig.LUMENFALL_SPREAD_RADIANCE_STRENGTH.get().floatValue(),
                            1.0F));
        }
    }

    /**
     * 放射光晕的条件参数：这一圈真的波及到了别人才放；同一维度在冷却时间内只放一次。
     * 冷却按维度计时，两个不同维度的战斗互不占用额度。
     */
    private static boolean shouldPlayRadiance(Level level, List<LivingEntity> targets) {
        if (!MaledictConfig.LUMENFALL_SPREAD_RADIANCE.get() || targets.isEmpty()){
            return false;
        }
        int cooldown = MaledictConfig.LUMENFALL_SPREAD_RADIANCE_COOLDOWN_TICKS.get();
        if (cooldown <= 0){
            return true;
        }
        long now = level.getGameTime();
        Long last = RADIANCE_TICK.get(level.dimension().location());
        if (last != null && now - last < cooldown){
            return false;
        }
        RADIANCE_TICK.put(level.dimension().location(), now);
        return true;
    }

    /**
     * 服务端停止时清掉冷却表，免得把上一次世界的时间戳带进下一次启动。
     */
    public static void clear() {
        RADIANCE_TICK.clear();
    }

    private LumenfallVisuals() {
    }
}

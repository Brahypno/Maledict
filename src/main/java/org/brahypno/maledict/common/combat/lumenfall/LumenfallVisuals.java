package org.brahypno.maledict.common.combat.lumenfall;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.config.MaledictConfig;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 沉辉的表现层：派发 Maledict 自己那份「可调强度放射光晕」（{@link LumenfallRadianceEffect}），
 * 粒子构建在客户端 {@code LumenfallRadianceActor}。特效走 Malum 既有的网络管道，不新增网络包。
 *
 * <p>配色是沉辉铋的浅虹彩：{@code A9B8EF} 蓝紫银作主色、{@code E29BDA} 虹彩粉紫作折光，
 * 光点在两者之间渐变；{@code 76D6D0} 青绿描边反光用在同一效果的粒子三色轮转里。
 *
 * <ul>
 *   <li>单体释放（80 tick 到期、没打死的那个目标）：一记<b>缩小版</b>放射光晕，也就是击杀那一下
 *       的单人小号版；强度与铺开范围都由配置给，见 {@link MaledictConfig}。</li>
 *   <li>死亡散辉：死亡位置按条件放一份满尺寸放射光晕。强度可配，且同一维度有冷却。</li>
 * </ul>
 *
 * <p>1.20.1 的散辉还会给每个被波及目标一束 Malum 的 {@code CONCENTRATED_RISING_SPARKLES} 残光；
 * 那个效果类型在 Malum 1.8.2 里已不存在，也没有语义相同的替代（最接近的 {@code SPIRIT_MOTE_SPARKLES}
 * 是精华放置特效），所以逐目标残光暂缺：{@link #spread} 只放光晕。要做的话得照这个文件的做法再建一个
 * 自制效果类型。
 */
public final class LumenfallVisuals {
    /**
     * 主色 {@code A9B8EF}：蓝紫银。
     */
    private static final Color PRIMARY = new Color(0xA9B8EF);
    /**
     * 折光色 {@code E29BDA}：虹彩粉紫。颜色数据要一起过线（效果类型带颜色编解码器），
     * 粒子自己的三色轮转在客户端构建器里，这两个值只决定被动方拿到的着色数据。
     */
    private static final Color SECONDARY = new Color(0xE29BDA);

    /**
     * 每个维度上一次放射光晕的 tick；只给冷却用，条目数等于维度数。
     */
    private static final Map<ResourceLocation, Long> RADIANCE_TICK = new HashMap<>();

    /**
     * 单体释放：只有一份<b>缩小版</b>的放射光晕 —— 击杀那一下的单人版。强度（粒子量）与范围
     * （铺开半径）都另有配置：{@link MaledictConfig#LUMENFALL_RELEASE_RADIANCE_STRENGTH}、
     * {@link MaledictConfig#LUMENFALL_RELEASE_RADIANCE_RADIUS}。强度设成 0 则释放没有粒子表现。
     */
    public static void release(LivingEntity victim) {
        float strength = MaledictConfig.LUMENFALL_RELEASE_RADIANCE_STRENGTH.get().floatValue();
        if (strength <= 0.0F) {
            return;
        }
        if (victim.level() instanceof ServerLevel level) {
            LumenfallRadianceEffect.instance().createEffect(victim)
                                                        .customData(new LumenfallRadianceEffect.Radiance(
                                                                strength,
                                                                MaledictConfig.LUMENFALL_RELEASE_RADIANCE_RADIUS.get()
                                                                              .floatValue()))
                                                        .color(PRIMARY)
                                                        .spawn(level);
        }
    }

    /**
     * 死亡散辉：死亡位置按条件放一份满尺寸的放射光晕（范围倍率固定 1.0，只有群伤才铺得开）。
     */
    public static void spread(Level level, Vec3 position, List<LivingEntity> targets) {
        if (!(level instanceof ServerLevel serverLevel) || !shouldPlayRadiance(level, targets)) {
            return;
        }
        LumenfallRadianceEffect.instance().createEffect(position)
                                                    .customData(new LumenfallRadianceEffect.Radiance(
                                                            MaledictConfig.LUMENFALL_SPREAD_RADIANCE_STRENGTH.get()
                                                                          .floatValue(),
                                                            1.0F))
                                                    .color(SECONDARY)
                                                    .spawn(serverLevel);
    }

    /**
     * 放射光晕的条件参数：这一圈真的波及到了别人才放；同一维度在冷却时间内只放一次。
     * 冷却按维度计时，两个不同维度的战斗互不占用额度。
     */
    private static boolean shouldPlayRadiance(Level level, List<LivingEntity> targets) {
        if (!MaledictConfig.LUMENFALL_SPREAD_RADIANCE.get() || targets.isEmpty()) {
            return false;
        }
        int cooldown = MaledictConfig.LUMENFALL_SPREAD_RADIANCE_COOLDOWN_TICKS.get();
        if (cooldown <= 0) {
            return true;
        }
        long now = level.getGameTime();
        Long last = RADIANCE_TICK.get(level.dimension().location());
        if (last != null && now - last < cooldown) {
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

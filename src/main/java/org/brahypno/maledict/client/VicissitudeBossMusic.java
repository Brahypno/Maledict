package org.brahypno.maledict.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity;
import org.brahypno.maledict.common.entity.VicissitudeBossStage;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.registry.MaledictSounds;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 无常 Boss 战的背景乐调度。
 *
 * <p><b>这里不需要网络包。</b> 同类模组之所以要发包切歌，是因为它们的 Boss 阶段没进同步数据；
 * 无常的 {@code DATA_STAGE} 和 {@code DATA_PHASE_TWO_REACHED} 本来就在 {@code SynchedEntityData} 里，
 * 客户端直接读就行。挂载点是现成的 {@link MaledictClientRenderEvents#onClientTick}——
 * 它每客户端 tick 扫一次 96 格内的无常。
 *
 * <p>阶段映射（见 docs/design/first-vicissitude/04 §1.1）：
 * <ul>
 *   <li>{@code DORMANT}：不放。此时只是座雕像，音乐一响就剧透了。</li>
 *   <li>{@code PHASE_ONE}：一阶段曲，<b>不循环</b>（126 秒 vs 最长 90 秒的一阶段）。</li>
 *   <li>{@code TRANSITION}：一阶段曲继续并淡出。<b>这格只有 3 秒</b>（60 tick），放不下独立曲子。</li>
 *   <li>{@code PHASE_TWO}：二阶段曲，<b>循环</b>（二阶段没有时限，打到死为止）。</li>
 *   <li>死亡 / {@code DYING}：淡出。</li>
 * </ul>
 *
 * <p>换曲时不 {@code stop()} 旧的那条，而是让它自己淡出，同时新的淡入——就是一次交叉淡化。
 */
public final class VicissitudeBossMusic {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 交战半径：无常离本地玩家超过这个距离就不放 BGM。
     *
     * <p>没有这道门的话，八十格外别人在打，你在这边挖矿也会被放 BGM——扫描半径是 96 格
     * （沿用 {@code MaledictClientRenderEvents} 的粒子扫描），对音乐来说太宽了。
     *
     * <p>48 这个数是取的 Boss 自己的 {@code damageRange} 默认值：超出这个距离，
     * 你的伤害会线性衰减到 72 格归零，也就是"已经打不动它了"。正常交战距离在 7 格上下。
     *
     * <p><b>写死是没办法</b>：{@code engagementRange} / {@code damageRange} 是 COMMON 配置，
     * 客户端连服务器的时候读不到服务端的值，拿本地那份又不一定一样。要调就改这里。
     */
    private static final double COMBAT_RADIUS = 48.0D;

    /** 每多少 tick 压一次原版背景音乐。 */
    private static final int VANILLA_MUSIC_SUPPRESS_INTERVAL = 100;
    /** play() 之后多少 tick 还没 active 就打一条警告。40 tick = 2 秒。 */
    private static final int STALL_WARN_TICKS = 40;

    /** 一首曲子：用哪个事件、要不要循环。 */
    private record Track(SoundEvent event, boolean looping) {
    }

    @Nullable
    private static VicissitudeMusicSound active;
    private static int ticks;
    /** 当前实例连续多少 tick 没能进入播放状态；用来发现 sounds.json / .ogg 缺失这类问题。 */
    private static int stalledTicks;

    private VicissitudeBossMusic() {
    }

    /**
     * 每客户端 tick 调一次。{@code bosses} 是本 tick 附近的无常，空表表示没有正在打的。
     */
    public static void tick(List<FirstVicissitudeBossEntity> bosses) {
        Track wanted = MaledictConfig.VICISSITUDE_BOSS_MUSIC.get() ? wantedTrack(bosses) : null;

        if (active != null && (wanted == null || active.getSoundEvent() != wanted.event())) {
            // 只标记淡出，不 stop()：让它和新的那首交叉淡化。
            active.fadeOut();
            active = null;
            stalledTicks = 0;
        }
        if (active == null && wanted != null) {
            active = new VicissitudeMusicSound(wanted.event(), wanted.looping(), volume());
            stalledTicks = 0;
            LOGGER.info("Vicissitude theme -> {} (looping={})",
                        wanted.event().getLocation(), wanted.looping());
        }
        if (active == null) {
            return;
        }

        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        if (soundManager.isActive(active)) {
            stalledTicks = 0;
        } else {
            soundManager.play(active);
            if (++stalledTicks == STALL_WARN_TICKS) {
                LOGGER.warn("Vicissitude theme {} is still not playing 2s after play(); "
                            + "check assets/maledict/sounds.json and sounds/music/*.ogg",
                            active.getSoundEvent().getLocation());
            }
        }
        // 原版「音乐」在战斗里要让位。注意：给血条设 setPlayBossMusic(true) 是压不掉的——
        // BossHealthOverlay#shouldPlayMusic() 只被 Minecraft#getSituationalMusic() 用在末地。
        if (++ticks % VANILLA_MUSIC_SUPPRESS_INTERVAL == 0) {
            Minecraft.getInstance().getMusicManager().stopPlaying();
        }
    }

    /**
     * 从附近所有无常里挑出该放哪一首。
     *
     * <p>选择规则本身在 {@link VicissitudeTheme#select} 里——那条规则踩过坑（见它的注释），
     * 所以抽成了不碰 Minecraft 的纯函数，由 {@code VicissitudeThemeTest} 覆盖。
     * 这里只负责把实体翻译成它的输入，再把结果翻译成曲子。
     */
    @Nullable
    private static Track wantedTrack(List<FirstVicissitudeBossEntity> bosses) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        List<VicissitudeTheme.Participant> states = new ArrayList<>(bosses.size());
        double radiusSqr = COMBAT_RADIUS * COMBAT_RADIUS;
        for (FirstVicissitudeBossEntity boss : bosses) {
            states.add(new VicissitudeTheme.Participant(
                    isEngaged(boss),
                    boss.distanceToSqr(player) <= radiusSqr,
                    boss.isPhaseTwoVisual()));
        }
        return switch (VicissitudeTheme.select(states)) {
            case NONE -> null;
            // 一阶段是固定计时（最长 90 秒），126 秒的曲子放得完，不用循环。
            // TRANSITION 也落在这里：一阶段曲继续，进入 PHASE_TWO 时才换成下面那条。
            case PHASE_ONE -> new Track(MaledictSounds.VICISSITUDE_MUSIC_PHASE_ONE.get(), false);
            // 二阶段没有时限，必须能循环。
            case PHASE_TWO -> new Track(MaledictSounds.VICISSITUDE_MUSIC_PHASE_TWO.get(), true);
        };
    }

    /**
     * 「正在打」：没死、不在垂死、也不是还没被唤醒的雕像。
     *
     * <p>{@code DORMANT} 就是"Boss 对任何生物都没有仇恨、一阶段都还没开始"——这时不放音乐。
     * 注意口径是**阶段**不是仇恨：刷怪蛋直接安排到一阶段的、或者仪式召唤出来的，只要进了
     * {@code PHASE_ONE} 就算开始，哪怕当下还没锁定目标。
     */
    private static boolean isEngaged(FirstVicissitudeBossEntity boss) {
        if (boss.getHealth() <= 0.0F) {
            return false;
        }
        VicissitudeBossStage stage = boss.getStage();
        return stage != VicissitudeBossStage.DORMANT && stage != VicissitudeBossStage.DYING;
    }

    private static float volume() {
        return (float) (double) MaledictConfig.VICISSITUDE_BOSS_MUSIC_VOLUME.get();
    }
}

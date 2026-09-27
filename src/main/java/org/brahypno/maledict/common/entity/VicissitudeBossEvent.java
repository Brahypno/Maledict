package org.brahypno.maledict.common.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import org.brahypno.maledict.network.BossBarStylePacket;
import org.brahypno.maledict.network.MaledictNetwork;

/**
 * 带皮肤号的无常血条。
 *
 * <p>皮肤号只在服务端是真值，客户端由 {@link BossBarStylePacket} 镜像——原因见那个类的注释：
 * 血条 UUID 是随机生成的，客户端没有别的办法认出这条血条属于谁。
 *
 * <p>三处发同步：入队补发（玩家可能是二阶段才第一次看见这条血条）、切阶段、离队时清掉映射。
 */
public final class VicissitudeBossEvent extends ServerBossEvent {
    /** 一阶段：当前生成的这套紫。 */
    public static final int STYLE_PHASE_ONE = 0;
    /** 二阶段：断口更宽、卡扣开裂。 */
    public static final int STYLE_PHASE_TWO = 1;

    private int style;

    public VicissitudeBossEvent(Component name, int style) {
        super(name, BossBarColor.PURPLE, BossBarOverlay.PROGRESS);
        this.style = style;
    }

    public int getStyle() {
        return style;
    }

    public void setStyle(int style) {
        if (style == this.style) {
            return;
        }
        this.style = style;
        for (ServerPlayer player : getPlayers()) {
            MaledictNetwork.sendBossBarStyle(player, getId(), style);
        }
    }

    /** 入队即补发：玩家可能是二阶段才第一次看见这条血条。 */
    @Override
    public void addPlayer(ServerPlayer player) {
        super.addPlayer(player);
        MaledictNetwork.sendBossBarStyle(player, getId(), style);
    }

    @Override
    public void removePlayer(ServerPlayer player) {
        super.removePlayer(player);
        MaledictNetwork.sendBossBarStyle(player, getId(), BossBarStylePacket.STYLE_NONE);
    }
}

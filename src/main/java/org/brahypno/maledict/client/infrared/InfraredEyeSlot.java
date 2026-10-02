package org.brahypno.maledict.client.infrared;

import java.util.List;

/**
 * 「眼槽这台设备开着吗」的纯判断：<b>第一格</b>视界槽存在，而且那一格的渲染开关不是灭的。
 *
 * <p><b>只看第一格。</b>视界槽正常就一格（基础 0 + 贤者献祭补的那 1），所以「第一格」就是这一格；
 * 万一将来多出几格（别的模组、别的修饰符），也不参与判断——设备认第一格的眼睛。
 *
 * <p><b>槽空着也算开着。</b>Curios 界面里那只眼睛是<b>整格槽位</b>的开关（{@code render_toggle}），
 * 与槽里有没有饰品无关：空槽默认是亮的，所以挣到视界槽之后就算什么都不戴，微光视觉照样工作；
 * 只有玩家自己把那一格点灭，或者还没挣到这一格（格数为 0），才不亮。
 *
 * <p>不碰 Minecraft 类，{@code InfraredEyeSlotTest} 才能直接断言这张表。
 */
final class InfraredEyeSlot {

    /** 参与判断的槽位下标：第一格。 */
    private static final int FIRST_SLOT = 0;

    /**
     * @param slots   视界槽的格数，没挣到时是 0
     * @param renders 每格的渲染开关；列表比格数短时，缺的那些按 Curios 的默认值（亮）算
     */
    static boolean isOn(int slots, List<Boolean> renders) {
        if (slots <= FIRST_SLOT) {
            return false;
        }
        return FIRST_SLOT >= renders.size() || Boolean.TRUE.equals(renders.get(FIRST_SLOT));
    }

    private InfraredEyeSlot() {
    }
}

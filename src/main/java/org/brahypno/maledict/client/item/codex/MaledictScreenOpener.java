package org.brahypno.maledict.client.item.codex;

import com.sammy.malum.client.screen.codex.BookEntry;
import com.sammy.malum.client.screen.codex.objects.progression.ScreenOpenerObject;
import com.sammy.malum.client.screen.codex.screens.progression.AbstractProgressionCodexScreen;
import com.sammy.malum.client.screen.codex.screens.progression.ProgressionScreenHolder;
import net.minecraft.resources.ResourceLocation;

/**
 * 本模组三张视野之间的跳转节点：画法与 {@link ScreenOpenerObject} 完全一致，但跳过去之后做两件
 * Malum 的控件不会做的事——把 {@link ProgressionScreenHolder#mostRecentScreen} 还原，
 * 并记下「法典停在哪一页」。
 *
 * <p>第一件：1.8.2 的 {@code openCodexViaTransition()} 会把 {@code mostRecentScreen} 改写成目标屏幕的
 * 持有者，而 Malum 的奥秘全典（{@code EncyclopediaEsotericaItem}）正是按这个字段挑屏幕的。不还原的话，
 * 玩家开过一次坠光秘典之后，再右键**普通的**奥秘全典也会翻到坠光秘典的页面上——那是 Malum 的书，
 * 不该被本模组的屏幕改写。1.20.1 的 Malum 没有这层全局状态（Esoterica 按自己的
 * {@code shouldOpenVoidCodex} 直接开奥术或虚空）。
 *
 * <p>第二件：{@link LightfallCodex} 另外记着「上次停在哪一页」。本模组的书需要这份记忆：书内跳转
 * 不会经过 {@code openCodexViaItem}，所以不在跳转时补记的话，关书再用坠光秘典物品开屏永远回到默认的
 * 奥术页。
 */
final class MaledictScreenOpener extends ScreenOpenerObject {
    private final ProgressionScreenHolder<?> destination;
    /** 跳转之前要做的副作用（例如记下「已见过虚空启示」）；没有就传 {@code null}。 */
    private final Runnable beforeOpen;

    MaledictScreenOpener(BookEntry entry, int posX, int posY, ProgressionScreenHolder<?> destination,
                         ResourceLocation icon, Runnable beforeOpen) {
        super(entry, posX, posY, destination, icon, 20, 20);
        this.destination = destination;
        this.beforeOpen = beforeOpen;
    }

    @Override
    public boolean click(AbstractProgressionCodexScreen screen, double mouseX, double mouseY) {
        ProgressionScreenHolder<?> previous = ProgressionScreenHolder.mostRecentScreen;
        try {
            if (beforeOpen != null) {
                beforeOpen.run();
            }
            LightfallCodex.rememberViewFor(destination);
            return super.click(screen, mouseX, mouseY);
        } finally {
            ProgressionScreenHolder.mostRecentScreen = previous;
        }
    }
}

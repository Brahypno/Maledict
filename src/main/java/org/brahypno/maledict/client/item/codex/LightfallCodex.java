package org.brahypno.maledict.client.item.codex;

import com.sammy.malum.client.screen.codex.screens.progression.ProgressionScreenHolder;

/**
 * The Lightfall Codex remembers the last view it was on, including the Lightfall page.
 * The screens are singletons (each {@code ProgressionScreenHolder} caches its instance), so each
 * one also keeps its own scroll offset and reopening the remembered view returns to that position.
 *
 * <p>「上次停在哪一页」由本类自己记：物品开屏走 {@link #openCodexViaItem}，书内跳转走
 * {@link #rememberViewFor}（1.20.1 的跳转节点直接回调 {@code openCodexViaItem}，所以那边天然记得住；
 * 1.8.2 的跳转封在持有者的 {@code openCodexViaTransition()} 里，必须在控件里补这一步）。
 * 不要改用 Malum 的 {@code ProgressionScreenHolder.mostRecentScreen} 兼记这件事——那个字段是
 * Malum 的两本书共用的，见 {@link #withoutBecomingMalumsMostRecent}。
 */
public final class LightfallCodex {
    public enum View {
        ARCANA,
        VOID,
        LIGHTFALL
    }

    private static View view = View.ARCANA;
    private static boolean knowsSoulbinding;

    public static void openCodexViaItem(boolean knowsSoulbinding) {
        LightfallCodex.knowsSoulbinding = knowsSoulbinding;
        switch (view) {
            case VOID -> LightfallVoidScreen.openCodexViaItem();
            case LIGHTFALL -> LightfallCodexScreen.openCodexViaItem();
            default -> LightfallArcanaScreen.openCodexViaItem();
        }
    }

    /**
     * 打开本模组的一张视野，同时不让它成为 Malum 的「最近一次法典」。
     *
     * <p>{@code ProgressionScreenHolder#openCodex} 会把 {@code mostRecentScreen} 改写成被打开的
     * 持有者，而 Malum 的奥秘全典（{@code EncyclopediaEsotericaItem}）按这个字段挑屏幕：不还原，
     * 玩家用过一次坠光秘典之后右键普通的奥秘全典就会翻到坠光秘典的页面。打开期间照常跳转，结束后
     * 把字段原样还回去（null 也照还：{@code getAppropriateCodexScreen()} 对 null 的回退就是奥术全典）。
     */
    static void withoutBecomingMalumsMostRecent(Runnable opener) {
        ProgressionScreenHolder<?> previous = ProgressionScreenHolder.mostRecentScreen;
        try {
            opener.run();
        } finally {
            ProgressionScreenHolder.mostRecentScreen = previous;
        }
    }

    /**
     * 记下「法典停在三张视野里的哪一页」：书内跳转节点也走这里，所以从奥术页点开坠光页、
     * 或从虚空页点回奥术页之后，再用坠光秘典物品开屏就回到最后停留的那一页，而不是开局默认的奥术页。
     *
     * <p>按目标持有者判页，而不是让调用方另传一个页枚举：两者一旦写岔，书里点的和记住的就会不一致。
     * 目标不是本模组的视野（Malum 自己的书）时不动记忆。
     */
    static void rememberViewFor(ProgressionScreenHolder<?> destination) {
        if (destination == LightfallArcanaScreen.SCREEN) {
            view = View.ARCANA;
        } else if (destination == LightfallVoidScreen.SCREEN) {
            view = View.VOID;
        } else if (destination == LightfallCodexScreen.SCREEN) {
            view = View.LIGHTFALL;
        }
    }

    static boolean knowsSoulbinding() {
        return knowsSoulbinding;
    }

    public static void rememberView(View remembered) {
        view = remembered;
    }

    private LightfallCodex() {
    }
}

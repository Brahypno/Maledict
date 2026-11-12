package org.brahypno.maledict.client.codex;

/**
 * The Lightfall Codex remembers the last page that was opened, including the Lightfall page.
 * The page instances are singletons, so each one also keeps its own scroll offset and
 * reopening the remembered page returns to that position.
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

    static boolean knowsSoulbinding() {
        return knowsSoulbinding;
    }

    public static void rememberView(View remembered) {
        view = remembered;
    }

    private LightfallCodex() {
    }
}

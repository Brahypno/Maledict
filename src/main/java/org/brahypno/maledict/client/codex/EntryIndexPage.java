package org.brahypno.maledict.client.codex;

import com.sammy.malum.client.screen.codex.pages.BookPage;
import com.sammy.malum.client.screen.codex.pages.EntryReference;
import com.sammy.malum.client.screen.codex.screens.CodexEntryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Two-column links inside a book page, with native entry navigation and its return path. */
public final class EntryIndexPage extends BookPage {
    private static final int CELL_WIDTH = 62;
    private static final int COLUMN_PITCH = 68;
    private final Component title;
    private final List<EntryReference> entries;

    public EntryIndexPage(String titleKey, List<EntryReference> entries) {
        super(ResourceLocation.fromNamespaceAndPath("malum", "textures/gui/book/pages/headline_page.png"));
        this.title = Component.translatable(titleKey);
        this.entries = List.copyOf(entries);
    }

    private int rowHeight() { return entries.size() > 4 ? 35 : 52; }
    private int startY() { return entries.size() > 4 ? 27 : 37; }
    private int cellX(int left, int index) { return left + 6 + index % 2 * COLUMN_PITCH; }
    private int cellY(int top, int index) { return top + startY() + index / 2 * rowHeight(); }

    @Override
    public void render(CodexEntryScreen screen, GuiGraphics graphics, int left, int top,
                       int mouseX, int mouseY, float partialTicks, boolean isRepeat) {
        ReadableTextPage.drawHeadline(graphics, title, left, top);
        var font = Minecraft.getInstance().font;
        int iconOffset = entries.size() > 4 ? 0 : 5;
        for (int i = 0; i < entries.size(); i++) {
            var reference = entries.get(i);
            if (!reference.entry.shouldShow()) continue;
            int x = cellX(left, i), y = cellY(top, i);
            boolean hovered = screen.isHovering(mouseX, mouseY, x, y, CELL_WIDTH, rowHeight());
            if (hovered) {
                graphics.fill(x, y, x + CELL_WIDTH, y + rowHeight(), 0x206A5037);
            }
            graphics.renderItem(reference.icon, x + (CELL_WIDTH - 16) / 2, y + iconOffset);
            var label = Component.translatable(reference.entry.translationKey());
            var lines = font.split(label, CELL_WIDTH);
            for (int line = 0; line < lines.size(); line++) {
                var text = lines.get(line);
                graphics.drawString(font, text, x + (CELL_WIDTH - font.width(text)) / 2,
                        y + iconOffset + 17 + line * 9, ReadableTextPage.INK, false);
            }
            if (hovered) {
                screen.renderLater(() -> graphics.renderComponentTooltip(font, List.of(label,
                        Component.translatable(reference.entry.descriptionTranslationKey())), mouseX, mouseY));
            }
        }
    }

    @Override
    public void click(CodexEntryScreen screen, int left, int top, double mouseX, double mouseY,
                      double relativeMouseX, double relativeMouseY) {
        // Malum 1.8.2 dispatches page clicks at guiTop + 8, but renders at guiTop + 11.
        int renderedTop = top + 3;
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i).entry;
            if (entry.shouldShow() && entry.hasContents()
                    && screen.isHovering(mouseX, mouseY, cellX(left, i), cellY(renderedTop, i), CELL_WIDTH, rowHeight())) {
                CodexEntryScreen.openScreen(entry);
                return;
            }
        }
    }
}

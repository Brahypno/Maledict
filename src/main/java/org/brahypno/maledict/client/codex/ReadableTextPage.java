package org.brahypno.maledict.client.codex;

import com.sammy.malum.client.screen.codex.helper.CodexItemHelper;
import com.sammy.malum.client.screen.codex.helper.CodexTextHelper;
import com.sammy.malum.client.screen.codex.pages.text.HeadlineTextPage;
import com.sammy.malum.client.screen.codex.screens.CodexEntryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Plain book ink avoids Malum's animated glow and offset text passes on these pages. */
public final class ReadableTextPage extends HeadlineTextPage {
    static final int INK = 0x513C2D;
    private final Component body;
    private final List<ItemStack> illustrations;

    public ReadableTextPage(String headline, String text) {
        this(headline, List.of(text), List.of());
    }

    public ReadableTextPage(String headline, List<String> textKeys, List<ItemStack> illustrations) {
        super(headline, textKeys.getFirst());
        var paragraphs = Component.empty();
        for (String key : textKeys) {
            if (!paragraphs.getSiblings().isEmpty()) paragraphs.append("\n");
            paragraphs.append(Component.translatable(TEXT + "." + key));
        }
        this.body = paragraphs;
        this.illustrations = List.copyOf(illustrations);
    }

    @Override
    public void render(CodexEntryScreen screen, GuiGraphics graphics, int left, int top,
                       int mouseX, int mouseY, float partialTicks, boolean isRepeat) {
        drawHeadline(graphics, headline, left, top);
        var font = Minecraft.getInstance().font;
        var lines = CodexTextHelper.wrapText(body, 130);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i), left + 6, top + 25 + i * 10, INK, false);
        }
        if (!illustrations.isEmpty()) {
            graphics.fill(left + 12, top + 147, left + 130, top + 148, 0x306A5037);
            int start = left + (142 - illustrations.size() * 16) / 2;
            for (int i = 0; i < illustrations.size(); i++) {
                CodexItemHelper.renderItem(screen, graphics, illustrations.get(i), start + i * 16,
                        top + 152, mouseX, mouseY);
            }
        }
    }

    static void drawHeadline(GuiGraphics graphics, Component title, int left, int top) {
        var font = Minecraft.getInstance().font;
        float scale = Math.min(1.0F, 130.0F / Math.max(1, font.width(title)));
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(left + 71, top + 5, 0);
        pose.scale(scale, scale, 1);
        graphics.drawString(font, title, -font.width(title) / 2, 0, INK, false);
        pose.popPose();
    }
}

package org.brahypno.maledict.client.codex;

import com.sammy.malum.client.screen.codex.pages.recipe.SpiritRiteRecipePage;
import com.sammy.malum.common.spiritrite.TotemicRiteType;

/**
 * Soulwood variant of {@link SpiritRiteRecipePage}.
 *
 * <p>Malum decides the glow from the entry identifier containing {@code corrupt}, but the
 * identifier is a translation key that may be renamed at any time. Pinning the corrupted look keeps
 * the page in step with the rite it shows, and lets one entry display both woods: the natural
 * variant stays the plain {@link SpiritRiteRecipePage}.
 */
final class SoulwoodSpiritRiteRecipePage extends SpiritRiteRecipePage {
    SoulwoodSpiritRiteRecipePage(TotemicRiteType rite) {
        super(rite);
    }

    @Override
    public boolean isCorrupted() {
        return true;
    }
}

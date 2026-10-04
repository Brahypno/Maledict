// Codex page layout checker for Maledict's localization.
//
// Malum's ArcanaCodexHelper#renderWrappingText lays out codex body text like this:
//   * the text is split into words on spaces; "\n" forces a line break;
//   * a word is pushed to the next line when width(line) + width(word) > 130;
//   * every word is appended with a trailing space, and that trailing space counts
//     towards the measured width of the line;
//   * line i is drawn at y = y0 + i * (font.lineHeight + 1) = y0 + i * 10;
//   * nothing clips the lines, so a page that needs more lines than fit draws the
//     overflow over the book frame / outside the page.
//
// Page geometry (EntryScreen): page texture is 142x172 at (guiLeft + 9 | 161, guiTop + 8).
// Body text offsets: TextPage y0=5, HeadlineTextPage y0=25, HeadlineTextItemPage y0=75,
// SpiritRiteTextPage y0=78; wrapping width 130 for all of them.
// A glyph drawn at y occupies [y, y + 9), so the last line must satisfy y0 + 10i + 9 <= 172.
//
// Usage: node tools/lang-layout/check-lang-layout.mjs
import { readFileSync, existsSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, "..", "..");
const langDir = resolve(root, "src/generated/resources/assets/maledict/lang");

const WRAP_WIDTH = 130;
const PAGE_BOTTOM = 172;
const LINE_HEIGHT = 10;
const GLYPH_HEIGHT = 9;

/** Minecraft's default (ascii.png) advance widths, spacing included. */
const WIDTHS = (() => {
  const w = {};
  const set = (chars, width) => { for (const c of chars) w[c] = width; };
  // Every value below is the ink extent of the glyph in vanilla ascii.png plus the 1px
  // spacing; verified against
  // .gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra/assets/minecraft/textures/font/ascii.png
  set("!.,:;'|", 2);
  set("i", 2);
  set("`l", 3);
  set('"()*', 4);
  set("I[]{}t", 4);
  set(" ", 4);
  set("<>fk", 5);
  set("#$%&+-/0123456789=?ABCDEFGHJKLMNOPQRSTUVWXYZ", 6);
  set("\\^_abcdefghjmnopqrsuvwxyz", 6);
  set("@~", 7);
  return w;
})();

/** Fallback advance for CJK and anything else drawn by the unicode font. */
const WIDE = 9;

function charWidth(ch) {
  const w = WIDTHS[ch];
  if (w !== undefined) return w;
  return ch.codePointAt(0) > 0x2000 ? WIDE : 5;
}

function textWidth(text) {
  let total = 0;
  for (const ch of text) total += charWidth(ch);
  return total;
}

/** Mirrors ArcanaCodexHelper#renderWrappingText and returns the rendered lines. */
function wrapLines(text) {
  const source = text + "\n";
  const lines = [];
  let line = "";
  let word = "";
  for (const chr of source) {
    if (chr === " " || chr === "\n") {
      if (word.length > 0) {
        if (textWidth(line) + textWidth(word) > WRAP_WIDTH) {
          lines.push(line);
          line = "";
        }
        line += word + " ";
        word = "";
      }
      if (chr === "\n") {
        lines.push(line);
        line = "";
      }
    } else {
      word += chr;
    }
  }
  return lines;
}

function capacity(y0) {
  return Math.max(0, Math.floor((PAGE_BOTTOM - GLYPH_HEIGHT - y0) / LINE_HEIGHT) + 1);
}

const PAGE_TYPES = {
  headlineItem: { y0: 75, capacity: capacity(75) },
  headline: { y0: 25, capacity: capacity(25) },
  text: { y0: 5, capacity: capacity(5) },
  spiritRite: { y0: 78, capacity: capacity(78) },
};

// page text key -> page type, as wired up in client/MaledictCodexEntries.java
const PAGES = {
  "malum.gui.book.entry.page.text.maledict.remembrance_bow.1": "headlineItem",
  "malum.gui.book.entry.page.text.maledict.remembrance_bow.2": "text",
  "malum.gui.book.entry.page.text.void.maledict.elegy_bow.1": "headlineItem",
  "malum.gui.book.entry.page.text.void.maledict.elegy_bow.2": "text",
  "malum.gui.book.entry.page.text.maledict.totemic_runes_continued.1": "headlineItem",
  "malum.gui.book.entry.page.text.maledict.totemic_runes_continued.2": "text",
  "malum.gui.book.entry.page.text.void.maledict.runeworking.1": "headlineItem",
  "malum.gui.book.entry.page.text.void.maledict.runeworking.2": "text",
  "malum.gui.book.entry.page.text.void.maledict.incursus_blade.1": "headlineItem",
  "malum.gui.book.entry.page.text.void.maledict.obelisks.soulwood_obelisk.1": "headline",
  "malum.gui.book.entry.page.text.void.maledict.obelisks.mnemonic_obelisk.1": "headline",
  "malum.gui.book.entry.page.text.void.maledict.umbral_experiment.1": "headline",
  "malum.gui.book.entry.page.text.maledict.rune_of_satiation.1": "headline",
  "malum.gui.book.entry.page.text.maledict.rune_of_decay.1": "headline",
  "malum.gui.book.entry.page.text.maledict.rune_of_the_pack.1": "headline",
  "malum.gui.book.entry.page.text.maledict.rune_of_ripening.1": "headline",
  "malum.gui.book.entry.page.text.void.maledict.rune_of_stagnant_evolution.1": "headline",
  "malum.gui.book.entry.page.text.void.maledict.rune_of_rotten_bone.1": "headline",
  "malum.gui.book.entry.page.text.void.maledict.rune_of_melancholia.1": "headline",
  "malum.gui.book.entry.page.text.void.maledict.rune_of_bliss.1": "headline",
  "malum.gui.book.entry.page.text.void.maledict.rune_of_the_fallen.1": "headline",
  "malum.gui.book.entry.page.text.void.maledict.vicissitude_rite.1": "headline",
  "malum.gui.book.entry.page.text.void.maledict.vicissitude_rite.2": "text",
  "malum.gui.book.entry.page.text.void.maledict.incursus_blade.2": "text",
  "malum.gui.book.entry.page.text.void.maledict.incursus_blade.3": "text",
  "malum.gui.book.entry.page.text.void.maledict.incursus_blade.4": "text",
  "malum.gui.book.entry.page.text.corrupt_sacrifice_rite": "spiritRite",
  "malum.gui.book.entry.page.text.corrupt_sacrifice_rite.2": "text",
};

function load(locale) {
  const file = resolve(langDir, `${locale}.json`);
  if (!existsSync(file)) throw new Error(`missing ${file}`);
  return JSON.parse(readFileSync(file, "utf8"));
}

const langs = { zh_cn: load("zh_cn"), en_us: load("en_us") };

let failures = 0;
const short = (key) => key.replace("malum.gui.book.entry.page.text.", "");

// Repo rule (AGENTS.md): the Chinese codex body copy carries a literal space after every 13
// visible characters. Practically that means no space-separated run may be longer than 13
// characters: the layout spaces are the only break points the wrapper can use inside CJK text,
// and a 14-character run measures 126px + spacing and pushes past the 130px column.
function checkChineseChunks(key, text) {
  const bad = text.split(/ +/).filter((run) => [...run].length > 13);
  if (bad.length > 0) {
    console.log(`    zh chunk rule violated in ${short(key)}: ${bad.map((b) => `${b} (${[...b].length})`).join(", ")}`);
    failures++;
  }
}

console.log("page capacities: " +
  Object.entries(PAGE_TYPES).map(([n, p]) => `${n}=${p.capacity}`).join(" "));
console.log("");

for (const [key, type] of Object.entries(PAGES)) {
  const cap = PAGE_TYPES[type].capacity;
  const row = [];
  for (const [locale, table] of Object.entries(langs)) {
    const text = table[key];
    if (text === undefined) {
      row.push(`${locale}: MISSING`);
      failures++;
      continue;
    }
    const lines = wrapLines(text);
    const over = lines.length > cap;
    if (over) failures++;
    if (locale === "zh_cn") checkChineseChunks(key, text);
    const widest = Math.max(...lines.map((l) => textWidth(l) - charWidth(" ")));
    row.push(`${locale}: ${String(lines.length).padStart(2)}/${cap} lines, widest ${String(widest).padStart(3)}px${over ? "  <-- OVERFLOW" : ""}`);
  }
  console.log(`${short(key)}  [${type}]\n    ${row.join("\n    ")}`);
}

console.log("");
console.log(failures === 0 ? "OK: every page fits." : `${failures} page(s) overflowing or missing.`);
process.exit(failures === 0 ? 0 : 1);

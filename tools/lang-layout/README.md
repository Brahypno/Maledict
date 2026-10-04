# lang-layout — 法典正文排版核算

`check-lang-layout.mjs` 复刻 Malum `ArcanaCodexHelper#renderWrappingText` 的换行算法，把
`src/generated/resources/assets/maledict/lang/en_us.json` 与 `zh_cn.json` 里每一页法典正文的
行数算出来，报告哪些页超出了页容量、哪些中文断句违反了 13 字规则。

```bash
node tools/lang-layout/check-lang-layout.mjs   # 退出码 0 = 全部放得下
```

`MaledictLanguage` 或 `MaledictCodexEntries` 改动、跑完 `runData` 之后跑一次即可。新增法典
页时，把新的 `...page.text.*` 键按页面类型补进脚本里的 `PAGES` 表。

## 排版模型（取自 Malum 1.20.1-1.6.7 反编译源码，不是猜的）

| 项 | 值 |
| --- | --- |
| 单页贴图 | 142 × 172（`EntryScreen` 画在 `guiLeft + 9 | 161`、`guiTop + 8`） |
| 折行宽度 | 130 px，按空格断词；`\n` 强制换行；行内每个词后面都补一个空格并计入宽度 |
| 行高 | `font.lineHeight + 1` = 10 px；画在第 i 行的字形占 `[y0 + 10i, y0 + 10i + 9)` |
| 正文起点 | `TextPage` y0=5、`HeadlineTextPage` y0=25、`HeadlineTextItemPage` y0=75、`SpiritRiteTextPage` y0=78 |
| 页容量 | TextPage 16 行、HeadlineTextPage 14 行、HeadlineTextItemPage 9 行、SpiritRiteTextPage 9 行 |

正文没有任何裁剪：行数超了就直接画到页框外面（Malum 自己的英文页也有 17–19 行的）。

字形宽度用的是原版 `ascii.png` 的实测值（墨迹宽度 + 1 px 字距），可从
`~/.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra/assets/minecraft/textures/font/ascii.png`
复算；中文全角按 9 px 计。这套宽度对得上仓库里「每 13 个可见字一个空格」的规矩：
13 × 9 + 4 = 121 ≤ 130，14 个字就顶到 130 且失去余量。

## 中英不齐长是常态

同一段话，英文的行数大约是中文的两倍——所以中文 6 行的页，英文可能 11 行。中文正文
按 13 字断句排得进 9 行的首页时，英文往往排不进；这种情况要给条目补一页 `TextPage`
续页（`MaledictCodexEntries` 里加一行 `new TextPage(键 + ".2")`），中文把同一段话拆到
两页、字句不动。

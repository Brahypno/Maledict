# BGM Licensing Research — "First Vicissitude" / 无常 boss fight (Maledict)

Research date: this session. Target: music that can be legally **bundled inside a distributed mod jar**,
including under monetisation (CurseForge/Modrinth rewards, ad revenue).

**Verification standard used.** Everything marked ✅ was read from the **primary source** — a CC deed or
legal code, a vendor's own terms of service, a Minecraft/Fabric wiki page, or the asset's own item page —
via `web_fetch` in this session. Items marked ⚠️ are reasoned conclusions flagged as opinion. Items marked
❌ **UNVERIFIED** could not be reached and are *not* asserted.

> ⚠️ I am not a lawyer. This is a good-faith engineering-level reading of public licence texts. If the mod
> ever earns meaningful money, have a lawyer read §1.7.

---

## 1. License landscape — what is actually safe

### 1.0 The distinction that decides everything

**"Can I use this in my video?" is a completely different question from "can I ship this audio file inside
my mod jar?"**

Bundling is *distribution of the sound recording itself*. Every user receives a byte-extractable copy of the
file. Many "royalty-free" licences grant only a **synchronisation** right (marry this music to your moving
image) and explicitly forbid redistributing the audio as an asset. A mod jar looks much more like "shipping
the asset" than like "scoring a video". This single distinction eliminates several otherwise attractive
libraries, and it is the axis the table in §1.10 grades on.

### 1.1 CC0 1.0 Universal — ✅ VERIFIED

Source: <https://creativecommons.org/publicdomain/zero/1.0/>

Deed, verbatim:

> "The person who associated a work with this deed has **dedicated** the work to the public domain by waiving
> all of his or her rights to the work worldwide under copyright law, including all related and neighboring
> rights, to the extent allowed by law."
> "You can copy, modify, distribute and perform the work, even for commercial purposes, **all without asking
> permission**."

**Requires for redistribution in a mod jar: nothing.** No attribution, no licence notice, no change notice,
no source URL. Commercial and monetised use explicitly permitted. CC0 cannot be revoked (it is a waiver, not
a revocable grant).

Four caveats that are in the deed's own "Other Information":

1. "In no way are the patent or trademark rights of any person affected by CC0, nor are the rights that other
   persons may have in the work or in how it is used, such as **publicity or privacy** rights." → irrelevant
   for instrumental music; relevant if a track contains a recognisable person's voice.
2. "Unless expressly stated otherwise, the person who associated a work with this deed makes **no warranties**
   about the work."
3. "When using or citing the work, you should not imply **endorsement** by the author."
4. **The one that actually matters:** "**Creative Commons has not verified the copyright status of any work to
   which CC0 has been applied.** CC makes no warranties about any work or its copyright status in any
   jurisdiction."

→ **Practical consequence:** a CC0 badge is only as strong as the uploader's right to apply it. If someone
uploads a commercial soundtrack and tags it CC0, you have no defence. **Archive the item page (HTML or PDF)
plus the date for every CC0 track you ship.** Ten minutes of work; it is your entire evidence file.

### 1.2 CC BY 3.0 Unported — ✅ VERIFIED

Source (full legal code): <https://creativecommons.org/licenses/by/3.0/legalcode.en>

Section 4(a) — **the licence URI must travel with every copy**:

> "You may Distribute or Publicly Perform the Work only under the terms of this License. **You must include a
> copy of, or the Uniform Resource Identifier (URI) for, this License with every copy of the Work** You
> Distribute or Publicly Perform. … You must keep intact all notices that refer to this License and to the
> disclaimer of warranties with every copy of the Work You Distribute or Publicly Perform."

Section 4(b) — **exactly what must be credited**:

> "…you must … keep intact all copyright notices for the Work and provide, reasonable to the medium or means
> You are utilizing:
> **(i)** the name of the Original Author (or pseudonym, if applicable) if supplied, and/or … the name of such
> party or parties [Attribution Parties];
> **(ii) the title of the Work if supplied;**
> **(iii)** to the extent reasonably practicable, **the URI**, if any, that Licensor specifies to be associated
> with the Work …; and,
> **(iv) in the case of an Adaptation, a credit identifying the use of the Work in the Adaptation**"

Section 4(b) — **the rule that tells you *where* to put it** (this is the important sentence for a mod):

> "…**in the case of an Adaptation or Collection, at a minimum such credit will appear, if a credit for all
> contributing authors of the Adaptation or Collection appears, then as part of these credits and in a manner
> at least as prominent as the credits for the other contributing authors.**"

Section 4(c) — moral rights:

> "you must not distort, mutilate, modify or take other derogatory action in relation to the Work which would
> be prejudicial to the Original Author's honor or reputation."

**Requires for redistribution in a mod jar:** author name, **title**, the material's URI where practicable,
the CC BY 3.0 URI **inside every distributed copy**, retained copyright notices, and — if you adapted it — a
note saying so. Placed **alongside your other credits, at least as prominent**.

### 1.3 CC BY 4.0 International — ✅ VERIFIED

Sources: <https://creativecommons.org/licenses/by/4.0/> (deed + footnotes),
<https://creativecommons.org/licenses/by/4.0/legalcode.en>

Requirements under §3(a)(1) — you must **retain** (if supplied by the licensor):

- (A) identification of the creator(s) and any others designated to receive attribution
- (B) **a copyright notice**
- (C) **a notice that refers to this Public License**
- (D) **a notice that refers to the disclaimer of warranties**
- (E) **a URI or hyperlink to the Licensed Material**, to the extent reasonably practicable

…**plus** §3(a)(1)(B): "**indicate if You modified the Licensed Material** and retain an indication of any
previous modifications"; **plus** §3(a)(1)(C): "indicate the Licensed Material is licensed under this Public
License, and include the text of, or the URI or hyperlink to, this Public License."

CC's own deed footnote spells out the 3.0 → 4.0 difference:

> "In 4.0, you must indicate if you modified the material and retain an indication of previous modifications.
> **In 3.0 and earlier license versions, the indication of changes is only required if you create a
> derivative.**"

The relief valve, §3(a)(2):

> "You may satisfy the conditions in Section 3(a)(1) **in any reasonable manner based on the medium, means,
> and context** in which You Share the Licensed Material. For example, it may be reasonable to satisfy the
> conditions by **providing a URI or hyperlink to a resource that includes the required information**."

**Requires for redistribution in a mod jar:** same as 3.0, plus an explicit "what I changed" note, plus a
warranty-disclaimer reference. A `CREDITS.md` inside the jar linking to a credits page satisfies this.

> **⚠️ Gotcha almost every guide misses:** §2(a)(4) says format conversion ("technical modifications") never
> produces Adapted Material — but §3(a)(1)(B) still requires you to *indicate* modifications. Since you **are**
> re-encoding to Ogg Vorbis and trimming for loop points, write "converted to Ogg Vorbis; trimmed for
> seamless looping" in every CC BY 4.0 credit line. One clause, zero cost, removes the argument entirely.

### 1.4 CC BY-SA — ⚠️ avoid; reason verified

Sources: <https://creativecommons.org/licenses/by-sa/4.0/legalcode.en>, CC BY-SA 3.0 legal code.

- §1(1): "where the Licensed Material is a **musical work, performance, or sound recording, Adapted Material
  is always produced where the Licensed Material is synched in timed relation with a moving image.**"
- §2(a)(4): "simply making modifications authorized by this Section 2(a)(4) [media/format technical
  modifications] **never produces Adapted Material**."
- §3(b) ShareAlike: if you Share Adapted Material, your Adapter's License must be a CC licence with the same
  License Elements (or a BY-SA Compatible License), with its URI included, and you may not add restricting
  terms.
- CC BY-SA **3.0** §1 explicitly defines **Collection** and states "a work that constitutes a Collection will
  not be considered an Adaptation"; §4(a) confirms SA does not reach the Collection itself.

**⚠️ Opinion:** an unmodified BY-SA track sitting in `assets/` next to your own code is very likely a
Collection, not Adapted Material, so SA probably does not infect your mod's licence. But "probably" is doing
a lot of work, you *are* trimming/looping, and every downstream reuser has to re-derive that argument.

**Recommendation: exclude BY-SA entirely.** §2 below contains enough CC0 and CC BY material that you never
need to take this risk. The itch.io/FMA survey also found **zero** BY-SA items in the Chinese/East-Asian
material screened — the trap mostly doesn't arise.

### 1.5 CC BY-ND / BY-NC / BY-NC-ND — ❌ exclude

- **ND (NoDerivatives):** looping *is* altering the work. Worse, FMA's own guide (✅ verified,
  <https://freemusicarchive.org/License_Guide>) states for BY-ND: "**Syncing a track to video/moving images
  constitutes a derivative work, which is prohibited by this licence.**" Off the table even for a trailer.
- **NC (NonCommercial):** monetised CurseForge/Modrinth rewards and ad revenue are commercial. FMA's guide,
  verbatim: for BY-NC, "You may NOT use this for fundraising, advertising, or promoting a product or service
  without further permission, even if you're a non-profit organization."

**This is where most of the good Chinese-themed material lives.** The delegated FMA/itch.io survey (see §2.4)
found that the closest matches on FMA — the SUB8ION *Moon Temple* / *Sacred Mountains* Chinese-ambient set,
Kharkov's *Guzheng*, Pistol Jazz's *Hi no Tori (Taiko solo)*, MASERPAN's *Guqin* — are all **CC BY-NC or
CC BY-NC-ND**. They are unusable here.

### 1.6 Free Music Archive (FMA) as a platform — ✅ SAFE, because it adds nothing

Source: <https://freemusicarchive.org/License_Guide>

Verbatim: "FMA artists can upload their own music and select a specific CC licence themselves. **Free Music
Archive does not own the copyright to these original works. FMA cannot license original work to you** for
commercial, private, or other use. FMA cannot alter nor change the terms of a music licence."

→ Your rights are **exactly** the CC licence shown on the individual track page. There is no extra FMA layer.

Two traps:
- FMA's catalogue skews heavily to CC BY-NC / BY-NC-ND. **Check every track page**, never the search listing.
- Legacy **"FMA-Limited License: Download Only"** permits "**ONLY** … personal downloading, listening, and
  streaming. **No video, no redistribution**, no broadcast/podcast". If you see this, walk away.

### 1.7 Royalty-free stock libraries — the redistribution-in-a-jar answer, per vendor

This is the section that matters most, and the answer differs per vendor. I read the actual binding terms for
**four of the six** you listed.

#### Pixabay — ⚠️ CONDITIONAL. The pre-2019 CC0 cutoff is the whole story.

✅ VERIFIED — Pixabay Terms of Service, "Last updated: April 4, 2023", read via Wayback because the live page
returns HTTP 403 (Cloudflare) to this tool:
<https://web.archive.org/web/20240101072348id_/https://pixabay.com/service/terms/>
(License Summary also verified: <https://web.archive.org/web/20241231233133/https://pixabay.com/service/license-summary/>)

**§3 CC0 License — the actionable rule:**

> "Some of the Content made available for download on the Service is subject to and licensed under the
> Creative Commons Zero (CC0) license … **CC0 Content on the Service is any content which lists a 'Published
> date' prior to January 9, 2019.** … the CC0 Content can be used for all personal and commercial purposes
> **without attributing** the author/content owner of the CC0 Content or Pixabay."

→ **Filter Pixabay audio to pre-2019 uploads and you have genuine, no-strings CC0.** Archive the page showing
the publish date.

**§4 Content License — everything published on/after 9 Jan 2019.** Grants an "irrevocable, worldwide,
non-exclusive and royalty-free right to download, use, copy, modify or adapt the Content for commercial or
non-commercial purposes" — **but** subject to Prohibited Uses, the first of which is:

> "**You cannot sell or distribute the Content (either in digital or physical form) on a Standalone basis.**
> … When we refer to '**Standalone**' we mean where **no creative effort has been applied to the Content and
> it remains in substantially the same form as it exists on the Services.**"

with these worked examples, verbatim:

> "using the Content in its original form or solely using a filter, changing colors, resizing or cropping the
> Content **remains Standalone use**."
> "using the Content with a combination of images, videos, audio files, other media, text, illustrations,
> background features and editing techniques **is not Standalone use**, so long as the combined effect is to
> make a 'new' creative work."

**⚠️ Opinion, and this is the crux:** dropping an unmodified post-2019 Pixabay MP3 into
`assets/maledict/sounds/` and shipping it in a jar sits much closer to the **prohibited** case than the
permitted one. The combined *product* is a creative work, but the *audio file* every user receives is
unchanged and directly extractable. That is "substantially the same form as it exists on the Services".

**§1 also undercuts §4:** Pixabay "reserve[s] the right … to **cancel or change the licenses granted by these
Terms**" — which sits awkwardly beside §4's "irrevocable" grant. For a mod you intend to ship for years, that
ambiguity is a liability.

§5: no attribution required (but appreciated): `by [Contributor] via Pixabay`.

**Verdict: use only pre-2019 (CC0-dated) Pixabay audio. Treat post-2019 Pixabay audio as not
redistributable in a jar.**

#### Uppbeat — ⚠️ MOSTLY PERMITS GAMES, with catches

✅ VERIFIED — Uppbeat User Agreement (Music Vine Limited t/a Uppbeat), Wayback snapshot 22 Dec 2024:
<https://web.archive.org/web/20241222183826/https://uppbeat.io/user-agreement>
(live page returns HTTP 429 to this tool)

Two definitions put games *inside* the grant:

> **Permitted Media:** "The type of Permitted Material, namely: (i) videos; (ii) audio (such as podcasts);
> and (iii) live streaming; **(iv) online games or applications.**"
> **Permitted Distribution:** "…**(v) any web-based game or application which can be downloaded and
> distributed online, for free or paid consumption.**"

That is genuinely more permissive than most. **But:**

- §5.3 "You may not **resell any Content (or otherwise make it available) as your 'product'**, as your music
  or as your song, even if it has been transformed or edited… This is called a 'Derivative Work'."
- §5.4 "You may not sell, transfer, share, give away or otherwise make any Content available to any other
  party except in accordance with this Agreement, and, in particular, you may not resell any Content … as
  part of any competing platform such as a music compilation or music library."
- §6.2 "All downloaded Content must be synched to your Permitted Material **whilst holding an active
  subscription.** It is not permitted to use previously downloaded Content once a subscription has ended. …
  It is not permitted to **download and stockpile** Content for future use outside of the subscription
  period."
- §4.4 perpetual tail: you may keep using Content already distributed, **provided first distribution happened
  during the Term.**
- §4.3 **Free Users must give the Credit — "This is a material term of this Agreement."**
- §5.1 limits permitted modification to restructuring/cutting; looping a section; using stems; reverb/effects
  to emulate environment; minimal effects. §5.2: "the original Content must remain **distinctly
  recognisable**."

**Verdict:** a paid Uppbeat subscription plausibly covers a mod, if you (a) publish the mod containing the
track **while subscribed**, (b) keep the track recognisable, (c) credit correctly, (d) never ship the raw
audio as a standalone download. It is a subscription dependency with terms-revision exposure. **If a CC0
track will do the job, prefer the CC0 track.**

#### Bensound — ❌ FREE TIER IS OUT OF SCOPE FOR GAMES. Paid tier is plausible but risky.

✅ VERIFIED — Bensound Licence & Services Agreement, "Last Revision: 22nd of June 2026":
<https://www.bensound.com/terms-and-conditions>

**§4.2 FREE LICENSE — verbatim:**

> "…you acknowledge and understand that (i) **the Free License … shall be revocable.** The Free License allows
> you to download and use Music Tracks, free of charge, in your Project **as part of an online video or live
> video streaming** that is published AND accessible free of charge, **OR** for usages limited to theatrical
> performances and films, providing that your Project is meant for educational purpose only and does not
> generate revenue."

→ **Video games are not within the Free License scope at all.** Do not use Bensound free music in a mod.

Games require **§4.3.1(ii) Pay-per-Track Professional License**, whose scope includes "…**background music
for a video game, application, software (non audio only)**…". Note the **Individual** license does *not*
cover games — you must buy the Professional tier.

Restrictions that bear on a mod jar:

- §5.1(xi) "**sublicense or assign the use of the Audio Assets for standalone distribution**" — prohibited.
- §5.1(xii) "make the Audio Assets available and/or distribute, resell, or perform the Audio Assets
  **separately from the Project** into which the Audio Assets have been incorporated" — prohibited.
  Embedding *in* the Project is contemplated; the ban is on separating them.
- §5.1(ix) "**permit a third party to use or copy the Audio Asset(s)**" — ⚠️ **this is the clause that should
  worry you.** Shipping a mod jar hands every downloader an extractable copy. Whether that is "permitting a
  third party to copy" or merely "distributing the Project" is **not resolved by the text**. This is
  genuinely ambiguous and is the reason I would not rely on it.
- §5.1(viii) permits "**basic editing** (including but not limited to **setting fade-in/fade-out points,
  determining the start and end points, or using only a selection** of the Audio Asset(s))" — so trimming and
  looping are fine; time-stretching, pitch-shifting or re-orchestrating are not.
- §5.3 "**Public performance rights … are not included**" — your responsibility to clear via a PRO.
- §4.3.2 / §4.5: All-Access licence certificates are only valid for Projects **published before the
  subscription expires**.

**Verdict: do not use the Free tier for a game — out of scope by its own text. A Professional license is
arguable but §5.1(ix) plus the PRO carve-out makes it a worse deal than a CC0 track. Pass.**

#### Epidemic Sound — ❌ UNVERIFIED

I could **not** retrieve Epidemic Sound's terms from a primary source.
`https://www.epidemicsound.com/music-licensing/` returns 404; the Wayback Machine has **no** archived copy at
`https://www.epidemicsound.com/terms-and-conditions/`. Their help-centre articles appeared in search results
but I did not successfully fetch them.

**I am not going to state what their licence says.** The general industry pattern for subscription
"royalty-free" services is a synchronisation licence for your own productions plus an explicit ban on
redistributing the audio as a standalone asset or inside a competing library — **but treat that as unverified
pattern-matching, not fact.** Independently: a subscription service is structurally the wrong shape of
licence for a mod jar you intend to ship indefinitely.

#### Artlist — ❌ UNVERIFIED

`https://artlist.io/license` returns HTTP 403 (Cloudflare); the Wayback snapshot redirects to a JS-only app
shell with no readable terms; the help-centre article "Understanding Artlist's license"
(<https://help.artlist.io/hc/en-us/articles/29490991524253-Understanding-Artlist-s-license>) also returns 403.

**Unverified — do not rely on it.** Same subscription-shape objection as Epidemic Sound.

#### YouTube Audio Library — ❌ UNVERIFIED

Both `https://support.google.com/youtube/answer/3376882` (tried with `?hl=en` and `?hl=en-IT`) and the
community thread `https://support.google.com/youtube/thread/209649566` render client-side: `web_fetch` returns
only the page `<title>` and no body text.

**I cannot confirm from a primary source whether YouTube Audio Library tracks may be bundled into a
distributed game.** Secondary sites make various claims; I deliberately did not rely on them. Given the
inability to verify, and that the Library is framed as a resource for YouTube uploads with per-track
attribution requirements, **do not use it for a mod jar.**

If you want to pursue it, the only defensible path is to read the licence text shown **on each individual
track's page inside YouTube Studio**, screenshot it, and archive it — that per-track text is the controlling
licence, not the help article.

### 1.8 Kevin MacLeod / Incompetech — ✅ VERIFIED. Current licence is **CC BY 4.0**.

Sources: <https://incompetech.com/music/royalty-free/faq.html> and
<https://incompetech.com/music/royalty-free/licenses/>

Two options: **(1) Creative Commons — free, requires credit**; **(2) Standard License — paid**, "for projects
where attribution is not wanted or is impossible (radio/TV ads, corporate presentations, on-hold music, etc.)".

The FAQ's copy-paste credit block, verbatim:

```
Title Kevin MacLeod (incompetech.com)
Licensed under Creative Commons: By Attribution 4.0
https://creativecommons.org/licenses/by/4.0/
```

> "It is important that you replace the word *Title* with the **Actual Title** of the piece that you are using!"

**Where the credit must go** — FAQ, "Do I have to put the credit where people can see it?":

> "**Yes.** … a credit needs to be placed such that a person who wants to know where the music came from
> should have no difficulty in finding it. A reasonable effort may be expended (e.g. clicking on a credits
> option) but the credit should not be obscured. … **Video Games — Most commonly, credits are placed on a
> 'Credits' screen found in the settings menu.**"

→ He explicitly names video games and a credits screen. A Credits screen satisfies this.

**Two facts people get wrong about Incompetech:**

> "**Is this music in the Public Domain? No. All of this music is copyrighted.** Though some of the baroque
> and classical compositions are in the public domain; **these recordings are not.**"

> "**Can I change your music?** Yes, you can sing over, chop, splice, compress, lengthen, and add instruments
> to anything you like. **You MUST make it clear in the credits which parts are yours, and which parts are
> mine. There is no need to mention if you cut and splice.**"

→ Pure cutting/splicing needs no note; layering/remixing does.

**FreePD.com is dead — ✅ VERIFIED.** <https://freepd.com/> now states: "After 17 years … we have officially
taken the service offline… **[freepd.com] is now permanently closed. 2008-2025**". FreePD used to host Kevin
MacLeod tracks dedicated to the public domain (CC0-equivalent) — and several OpenGameArt listings still point
at it. **Any freepd.com URL in an old track listing is a dead link and the PD dedication can no longer be
re-verified.** Prefer tracks whose CC0 status is visible on a live page.

**Verdict: Incompetech is usable and safe. It costs you one credits line. Put the credits screen in from day
one.**

### 1.9 Classical compositions vs. recordings — the trap

A **composition** (Bach, Chopin, a 12th-century conductus) can be public domain while every **recording** of
it is separately copyrighted as a sound recording, with performers holding neighbouring/performance rights on
top. **"It's Bach" is not a licence.**

Incompetech's FAQ states this bluntly (quoted in §1.8): "some of the baroque and classical compositions are in
the public domain; **these recordings are not**."

**Sources of genuinely PD/CC0 recordings:**

- **Wikimedia Commons** — files carry a machine-readable licence template (`PD-old`, `CC0`, `CC BY-SA 3.0`, …).
  You must open the individual `File:` page and read the template. ✅ Verified working example:
  <https://commons.wikimedia.org/wiki/File:Breves_dies_hominis.ogg> — a 12th–13th century composition,
  vocal performance by Magdalen Kadel, dedicated to the public domain by the uploader (the OGA mirror is
  item #5 in §2.1 below).
- **Musopen** — hosts **both** PD/CC0 recordings *and* CC BY / CC BY-NC ones. Per-track verification is
  mandatory.
- **IMSLP** — primarily a **score** library. Scans are often PD, but recordings hosted there are contributed
  by third parties under varying terms and are frequently **not** freely redistributable. Treat IMSLP as a
  score source, not an audio source, unless a specific recording's page grants a redistribution-friendly
  licence.

**US sound-recording copyright cutoff — ✅ NOW VERIFIED** (from 17 U.S.C. §1401,
<https://www.copyright.gov/title17/92chap14.html>; the full quote is in §2.6.0 below):

- Recordings first published **1925 or earlier → public domain.** (1925 entered PD on 1 Jan 2026.)
- **1926 → enters PD on 1 Jan 2027.** 1927 → 2028. 1931 → 2032. 1947–1956 → 110 years. Post-1956 → 2067.

**The composition/recording split is handled on Commons by *two* licence tags per file** — one for the
composition, one for the recording. ✅ **If a Commons file carries only a composition tag, the recording is
NOT cleared.** That is the single most important operational rule in this whole report for classical audio.

⚠️ Beware stale secondary sources: the Cornell/Hirtle chart's *foreign-recordings* row still says "Before
1925" (contradicting its own US row and the statute, which draws no US/foreign distinction), and both IMSLP's
and the Library of Congress's explainer pages still say "1922 and earlier". **The statute controls.**

**Performance / neighbouring rights.** The US has no separate performers' right — the sound-recording
copyright covers the fixation, and what you must clear for a mod is **reproduction + distribution** (the §114
digital-transmission performance right targets streaming services, not shipped files). Outside the US,
performers' and producers' rights are separate **neighbouring rights**; in the EU the term is 50 years
(Directive 2006/116/EC) or 70 years (Directive 2011/77/EU) from publication.

→ **This splits PD recordings into two tiers, and it matters because a Modrinth/CurseForge mod ships
worldwide:**
- **Tier A — globally clean:** pre-1926 fixations, or CC0. No attribution required.
- **Tier B — public domain *in the US only*:** US federal works (17 U.S.C. §105 — US military band
  recordings). §105 has no effect abroad, so a 1981 Marine Band recording is US-PD but its EU neighbouring
  rights run to roughly 2051. **Ship Tier B only if you accept a US-centric clearance.**

### 1.10 Summary table — does it permit *redistribution inside a mod jar*?

| Source / licence | Bundle the audio file in a jar? | Attribution? | Commercial / monetised? | Verified? |
|---|---|---|---|---|
| **CC0 1.0** | ✅ Yes | **None required** | ✅ Yes | ✅ deed read |
| **CC BY 3.0** | ✅ Yes | Author + **title** + work URI + licence URI **in every copy**, kept intact; changes if adapted; must sit with your other credits, no less prominent | ✅ Yes | ✅ legal code read |
| **CC BY 4.0** | ✅ Yes | Author + copyright notice + licence notice + warranty-disclaimer notice + material URI + **"I modified this"** statement + licence URI | ✅ Yes | ✅ deed + footnotes read |
| **CC BY-SA 3.0/4.0** | ⚠️ Probably, but avoid | As BY, **plus** ShareAlike on any Adapted Material | ✅ Yes | ✅ legal code read |
| **CC BY-ND** | ❌ No | — | — | ✅ FMA guide |
| **CC BY-NC / NC-SA / NC-ND** | ❌ No | — | ❌ No | ✅ FMA guide |
| **Pixabay, pre-2019** | ✅ Yes (true CC0) | None | ✅ Yes | ✅ Wayback ToS §3 |
| **Pixabay, post-2019** | ⚠️ **Risky — "Standalone" prohibition** | None | ✅ Yes | ✅ Wayback ToS §4 |
| **Free Music Archive** | ✅ Pass-through: whatever the track's own CC licence says | per track | per track | ✅ FMA License Guide |
| **Uppbeat (paid)** | ⚠️ Plausibly yes — games are named Permitted Media; subscription-bound | **Mandatory** (material term for free users) | ✅ Yes | ✅ Wayback User Agreement |
| **Bensound Free** | ❌ **No — games out of scope; licence revocable** | — | — | ✅ terms |
| **Bensound Professional** | ⚠️ Ambiguous (§5.1(ix) "permit a third party to use or copy") | Not required | ✅ Yes | ✅ terms |
| **Epidemic Sound** | ❌ **UNVERIFIED — do not rely** | ? | ? | ❌ could not fetch |
| **Artlist** | ❌ **UNVERIFIED — do not rely** | ? | ? | ❌ 403 / JS-only |
| **YouTube Audio Library** | ❌ **UNVERIFIED — do not use** | ? | ? | ❌ JS-only |
| **Incompetech / Kevin MacLeod** | ✅ Yes | Credit block above; **Credits screen** per his own guidance | ✅ Yes | ✅ FAQ |
| **Classical *recording*** | Depends on **that recording's** licence, not the composer's | per licence | per licence | ✅ (Incompetech FAQ; Commons example) |

**Bottom line, in order of preference:**

1. **CC0 1.0** — no obligations at all. This should be the backbone.
2. **CC BY 4.0 / 3.0** — costs one credit line per track plus a licence URI.
3. **Incompetech CC BY 4.0** — fine, with explicit video-game credits-screen guidance.
4. **Pre-2019 Pixabay (CC0-dated)** — fine; archive the proof.
5. Paid stock (Uppbeat, Bensound Professional) — possible but subscription-shaped. Only if a track is
   irreplaceable.
6. **Avoid:** CC BY-SA, BY-ND, anything NC, post-2019 Pixabay Content License, Bensound Free, YouTube Audio
   Library, and anything you couldn't verify.

---

## 2. Concrete recommended tracks/sources

> ### ⚠️ 阅读前必看：本节的选曲前提是错的
>
> 这一节是按 **"imperial Chinese / taoist cosmic horror"（中式帝皇恐怖）** 这个前提搜集的。
> **那个前提不是需求方给的**，是调研指令里自行加入的，见下面 "Theme reality check" 一段自己承认的
> "no ready-made imperial-Chinese suite"。
>
> 项目对这个 Boss 的真实定义在 [01_SPEC §1](01_SPEC.md)：**"冷静、无奈、不得已——不是神本身，
> 也不是恶魔或传统的骷髅死神"**，一阶段高位圣像、二阶段前倾的执行者、死亡是熄灭。
> 按这个定义，**真正对调性的是 §2.6 的公有领域古典那一节**（Holst《土星》《火星》、
> Berlioz、Mussorgsky、Satie、Bach 管风琴），而 **§2.6 B 组那 6 条中文素材整组可以跳过**。
>
> **顺位推荐看 [04 §3.2](04_AUDIO_AND_BOSS_BAR.md)**。本节保留原样，作为**素材库和许可证据**用——
> 清单、引文、核实标记都有效，只是排序和"最贴合"的判断作废。

**Method.** Every OpenGameArt item below was fetched individually and its licence block read. Two
non-OGA finds (`BiteMe Games`, `Visager`) I re-fetched and confirmed personally. The remaining itch.io and FMA
entries came from a **delegated research pass** that reported fetching each item page; I have marked those
**[delegated]** and re-verified two of the highest-value ones myself. Treat [delegated] entries as
"verified once, by a sub-agent, with the quoted text recorded" — re-check them at download time.

**Theme reality check, stated up front.** There is **no** ready-made "imperial Chinese / taoist cosmic horror
two-phase boss suite" available under a redistribution-safe licence. The realistic plan is to pick 2–3 beds
and **layer them in Audacity** — e.g. erhu/gong over taiko with a chant pad. Both CC0 and CC BY permit that;
you then note the modification in your credits. The exact erhu/guzheng/taiko-led dark-imperial sound is a
commissioning job.

### (a) Phase 1 — measured, ominous, ritualistic, loopable

**1. "Dark Shrine Loop" — qubodup (a remix of "Shrine" by yd)** ★ top pick
<https://opengameart.org/content/dark-shrine-loop>
**License: CC0 1.0** — page links <http://creativecommons.org/publicdomain/zero/1.0/> ✅ verified on page.
Files: **FLAC, OGG (679 KB), MP3, plus the LMMS project**. Loopable by construction ("loop" tag; it is a
temple/cult ambience). Tags: shrine, temple, cathedral, church, cult, prayer, worship, dark, evil, mystery.
29 favourites, 694 OGG downloads. **This is literally "dark ritual ambience" and it is CC0.** Attribution
optional; the page credits yd as the original author of "Shrine".

**2. "Views From Atop the Jade Kings Throne" — Hitctrl** ★ best instrumental colour match
<https://opengameart.org/content/views-from-atop-the-jade-kings-throne>
**License: CC BY 3.0** ✅ verified on page (links <http://creativecommons.org/licenses/by/3.0/>).
Files: **MP3 (9.5 MB) and OGG (3.3 MB)** — the OGG drops straight in. Tags: **erhu**, gong, strings, asian,
orchestral, fantasy, violin, cello, piano, drums. Attribution notice on page: "HitCtrl". Erhu + gong is
exactly the requested palette. Not authored as a loop — you will need loop surgery.

**3. "Chinese Game Music" (2 tracks) — BiteMe Games** ★ best loop-ready Chinese asset
<https://bitemegames.itch.io/chinese-game-music> **[re-verified by me directly]**
**License: CC0 1.0** — itch.io structured field: "**Asset license — Creative Commons Zero v1.0 Universal**"
✅. Page text, verbatim: *"2 pieces of music, designed for a Chinese martial arts setting, **using mainly the
Guzheng, Flute & Drums**. Both songs are 60s long, and have **a looping and non-looping version**. One calm
track (**Lotus Pond**) and one battle track (**Dragon Dance**)."* and *"You can use this asset for commercial
projects, without attribution required. You cannot resell this edit."* Also flagged **"No generative AI was
used"**. ZIP 71 MB, codec not stated (assume WAV).
→ **"Lotus Pond" is the Phase 1 bed; "Dragon Dance" is the Phase 2 bed — from the same pack, same
instrumentation, same key. That makes the phase transition musically coherent for free, and it is CC0.** The
mild internal tension between the CC0 field and "you cannot resell this edit" is irrelevant to you: you are
not reselling the audio as an edit.
**This is the single most useful find for your specific brief.**

**4. "Fantasy Choir — 3 orchestral pieces" — cesisco / César da Rocha**
<https://opengameart.org/content/fantasy-choir-3-orchestral-pieces>
**License: CC0 1.0** ✅ verified on page. ZIP of 24-bit files (69.7 MB); 3 separate pieces with 3 previews.
Page states: *"No need to credit me, but you can credit 'César da Rocha' if you like."*
Choir + strings + ensemble. **This is your monastic-chant layer** — pick the slow piece for Phase 1 and the
martial one for Phase 2. 58 favourites, 2,826 downloads; confirmed used in multiple shipped games per the
comments.

**5. "Breves dies hominis" — comp. 12th–13th c., performed by Magdalen Kadel (uploaded by ceninan)**
<https://opengameart.org/content/breves-dies-hominis>
**License: CC0 1.0** ✅ verified on page. Source file:
<https://commons.wikimedia.org/wiki/File:Breves_dies_hominis.ogg>. **OGG, 2.8 MB.**
Page: *"Vocal solo performance by Magdalen Kadel, singing a composition from the 12th-13th century with latin
lyrics… The performance has been put under public domain by user Makemi (Magdalen Kadel)."*
A single unaccompanied Latin voice. **Use it for the 无常 reveal / phase-transition moment** — maximally
eerie precisely because it is bare. Loop by crossfading a long reverb tail.

**6. "Ritual" — brainiac256**
<https://opengameart.org/content/ritual>
**License: CC BY 3.0** — the page lists **four** licences (LGPL 2.1, LGPL 3.0, CC BY 3.0, CC BY-SA 3.0) and
you may **elect CC BY 3.0** ✅ verified on page. **OGG, 2.7 MB.**
Page: *"Probably suited for an exotic but terrestrial environment, this track includes **driving ethnic
percussion and pentatonic motifs**"*. Tags include china, chinese, japan, orchestral, tabla. Attribution:
brainiac256.
⚠️ Note the LGPL options are irrelevant noise — pick CC BY 3.0 and say so in your credits.

**7. "Liyan" — elerya**
<https://opengameart.org/content/liyan>
**License: CC BY 3.0** ✅ verified on page. MP3, 1.2 MB. *"Asian atmospheric tune"*; tags asian, ambient, RPG,
instrumental. Attribution: elerya. Short and droney — good as a low-intensity intro before the fight proper.
Comments show it shipping in a Steam board-game adaptation and an Android game, both with credit — a mild
extra signal the licence is honoured in practice.

**8. "Oriental Suspense" — poltergasm** **[delegated]**
<https://poltergasm.itch.io/oriental-suspense-music>
**License: CC BY 4.0** — page states *"License: https://creativecommons.org/licenses/by/4.0/"*; creator:
*"I decided to experiment with an oriental theme using the BBC Orchestra."* MP3 + WAV. Length not stated
(~1:55–2:00 estimated from file sizes). Literally titled "suspense" — Phase 1 by intent.
⚠️ **The licence lives only in the description text, with no structured itch.io licence field. Archive the
page at download time.**

### (b) Phase 2 — faster, aggressive, percussion-heavy, chaotic

**9. "Boss Battle Music" (Epic Boss Battle) — Juhani Junkala / SubspaceAudio** ★ top CC0 pick
<https://opengameart.org/content/boss-battle-music>
**License: CC0 1.0** ✅ verified on page. **WAV, 21.8 MB**; the filename itself is
`Juhani Junkala - Epic Boss Battle [Seamlessly Looping].wav` and the page says *"Loops seamlessly!"*
106 favourites / **7,425 downloads** — the most-used CC0 boss theme on OGA. *"Epic boss battle music inspired
by God of War."*

**10. "Samurai Nights" — Majadroid** ★ best structural fit for a *phase-shifting* boss
<https://opengameart.org/content/samurai-nights>
**License: CC BY 4.0 / CC BY 3.0 / OGA-BY 3.0** (three listed; elect CC BY 4.0) ✅ verified on page. ZIP 3.9 MB.
Page, verbatim: *"a track with **high tension for fighting scenes**. The instruments are **traditional
chinese ones - like a Erhu**. I included the whole track **as well as all single loops**, so that you could
arrange them in a way you need it."*
→ **You can build the Phase 1 bed and the Phase 2 escalation from the same stem set, so the transition is
seamless by construction.** One OGA commenter says it "doesn't sound very tense" — audition it; it may suit
Phase 1 better than Phase 2.

**11. "Colossal Boss Battle Theme" — Matthew Pablo**
<https://opengameart.org/content/colossal-boss-battle-theme>
**License: CC BY 3.0** ✅ verified on page. ZIP 49.8 MB.
*"epic **choir chants**, heavy-hitting drums, and massive orchestral moments… **Looped version is also
included.**"* Tags: choir, chants, dark, evil, boss, final, monster, strings. 76 favourites, 3,605 downloads;
a commenter confirms a very good loop.
⚠️ The page's "Attribution Instructions" field points at `http://www.matthewpablo.com/services`, and a 2018
comment reports that site was already inactive — **I did not successfully fetch it, so treat that page as
possibly dead.** This does not weaken the grant: the OGA licence block (CC BY 3.0) is the controlling licence.
Crediting "Matthew Pablo" + track title + the OGA URL + the CC BY 3.0 URI satisfies CC BY 3.0 fully.

**12. "Blackmoor Ninjas (Battle Theme)" — Matthew Pablo** ★ the two-version trick
<https://opengameart.org/content/blackmoor-ninjas-battle-theme>
**License: CC BY 3.0** ✅ verified on page. ZIP 10.1 MB.
*"high energy guitars, huge **taiko drums**, **ninja shouts** and much more. I've included a complete package
with versions **with vocal chants and without vocal chants**."*
→ **Chant version for Phase 2, chant-less version for Phase 1 or for when you want the mix to duck under
boss dialogue.** 44 favourites, 1,597 downloads.

**13. "Taiko drums (seamless loop)" — jobro (submitted by congusbongus)**
<https://opengameart.org/content/taiko-drums-seamless-loop>
**License: CC BY 3.0** ✅ verified on page. **OGG, 10.6 MB.**
*"instrumental ensemble with taiko drums, bells and shouts, perfect for fighting or east Asian themed games."*
✅ **I also verified the upstream original** to check the licence chain:
<https://freesound.org/people/jobro/sounds/112248/> shows **"Attribution 3.0"**, and the file metadata:
**Stereo, 44100 Hz, 16-bit, 3:46, 38.2 MB WAV**. So the chain *freesound CC BY 3.0 → OGA CC-BY 3.0* is
consistent and the OGA claim holds up. Attribution: jobro.

**14. "Determined Pursuit (epic orchestra loop)" — Emma_MA**
<https://opengameart.org/content/determined-pursuit-epic-orchestra-loop>
**License: CC0 1.0**, and the page states outright: *"This track is in the public domain as of January 2017.
**No attribution necessary.**"* ✅ verified. WAV, 19.1 MB. *"**Loopable.** Frantic strings, determined horns
and busy drums… battle theme, boss fight or a hectic race against the clock."*

**15. "Drums of Dawn" — Tomasz Kucza / Magnesus** — the layering workhorse
<https://opengameart.org/content/drums-of-dawn>
**License: CC BY 3.0** ✅ verified on page. MP3, 2 MB. Tagged eastern, taiko, drum, bright, loud. Attribution
notice on page: "Tomasz Kucza / magory.games". Simple, drum-only — **ideal as a percussion bed you stack
under `Views From Atop the Jade Kings Throne`, or as the Phase 2 spine you assemble yourself.**

**16. "Dark Sanctum (Boss Fight) [Loop]" — Visager** **[re-verified by me directly]**
<https://freemusicarchive.org/music/Visager/Songs_From_An_Unmade_World/Visager_-_Songs_from_an_Unmade_World_-_18_Dark_Sanctum_Boss_Fight_-Loop-/>
**License: CC BY 4.0** — page, verbatim: *"Dark Sanctum (Boss Fight) [Loop] by Visager is licensed under a
[Attribution License](https://creativecommons.org/licenses/by/4.0/)."* ✅ **2:42. "AI generated? No."**
Part of *Songs From An Unmade World*, which contains several **authored-as-loop** boss tracks under CC BY 4.0
(*Battle! [Loop]*, *Miniboss Fight [Loop]*, *Final Sacrifice [Loop]*, *Eerie Mausoleum [Loop]*, *Haunted
Forest [Loop]*, *Shrine*). **[delegated]** for the siblings.
⚠️ FMA genres are Soundtrack / **Indie-Rock / Chiptune** — this reads as "retro game boss", **wrong palette
for imperial Chinese horror**. Keep it as a fallback, not a first choice.

**17. "Sinister Boss Appears!" — cynicmusic**
<https://opengameart.org/content/sinister-boss-appears>
**License: CC0 1.0** ✅ verified on page. WAV, 15.7 MB. *"Super evil boss introduction theme."*
⚠️ Minor oddity worth knowing: the page's "Copyright/Attribution Notice" field contains only
`http://cynicmusic.com http://pixelsphere.org`, while the licence block is CC0, and the author elsewhere
writes *"Much of the music on pixelsphere.org you may use for free in your game **if you contact me for
permission and join my mailing list**."* That sentence is about **other** tracks on his site — this one is
explicitly CC0 — but this is exactly the kind of page where you screenshot the CC0 block as evidence.
More of his CC0 catalogue: <https://opengameart.org/content/cc0-audio-uploader-cynicmusic>.

**18. "Horror Sound Pack" — Phlegmlee** **[delegated]**
<https://phlegmlee.itch.io/horror-sound-pack>
**License: CC0 1.0** — itch.io structured field "Asset license — Creative Commons Zero v1.0 Universal".
*"Five **loop-able**, atmospheric songs"* + 11 SFX (jump scares, footsteps, growling). "No generative AI".
ZIP 86 MB. **Phase 1 beds + boss stingers.** ⚠️ The itch.io field says CC0 while the prose says "free to
use" — mild inconsistency; the CC0 field is the machine-readable grant.

**19. "Dungeon Music Pack" — JHawk** **[delegated]**
<https://jhawk-studios.itch.io/dungeon-music-pack>
**License: CC0 1.0** — itch.io structured field. MP3 only. `HeatOfBattle.mp3` (6.8 MB) for Phase 2;
`Element.mp3`, `ClockWork.mp3`, `Abnormal Circumstances.mp3` for Phase 1.

### Bonus / texture and layering material

- **"Eastern Arctic Dubstep" — VishwaJai** — <https://opengameart.org/content/eastern-arctic-dubstep> —
  **CC0 1.0** ✅. MP3, 5:15. Page: *"Also: **loopable at full length and at 1:14**"*. Copyright notice:
  *"No attribution is required. But if you wanna be cool about it, credit Vishwa Jay."* East-meets-west,
  mysterious. ⚠️ The author commented *"Regrettably, I've lost the original files"* — **grab the MP3 now.**
- **"Sunrise in the Dynasty" — Yottabyte1024** — <https://opengameart.org/content/sunrise-in-the-dynasty> —
  **CC BY 3.0** (also offered as CC BY-SA 3.0; elect CC BY 3.0) ✅. MP3 3.2 MB. Chinese-style experiment.
  ⚠️ The page warns there is a silent gap mid-track, so **not loop-friendly as-is**. Pre-/post-fight theme.
- **Samuel Corwin — "Tibetan Monks Chanting at Gyuto Branch Monastery…"** — FMA — **CC BY 4.0**, 4:48
  **[delegated]** —
  <https://freemusicarchive.org/music/Samuel_Corwin/Selected_Field_Recordings_from_India_and_Nepal_Volume_II_Religious_Music/Samuel_Corwin-09-Tibetan_Monks_Chanting_at_Gyuto_Branch_Monastery_House_No_422_McLeod_Ganj/>
  The only **verifiable actual monastic chanting** source found. It is a field recording (room noise present),
  so use it as a layer under a drone, not as a bed.
- **Le Chaos Entre 2 Chaises feat. Dasom Baek — "Appréhension I"** — FMA — **CC BY 4.0**, 3:02 **[delegated]** —
  dark East Asian flute, "a contemplative ballad on the edge of apprehension".
- **Kevin MacLeod — "Ritual"** — FMA — **CC BY 3.0**, 4:24 **[delegated]** —
  <https://freemusicarchive.org/music/Kevin_MacLeod/Thatched_Villagers/Ritual/>
  ⚠️ **CC BY 3.0 additionally requires the title in the credit** (§1.2). Same for *Rite of Passage*,
  *Tikopia*, *Cambodean Odessy*.
- **"The Amazing Locomcircus" — Lenny Pixels** — CC BY 4.0 **[delegated]** — licence fine, **rejected on fit**
  (circus music).
- **OpenGameArt curated CC0 music collections** — mine these directly, but **verify each item page** (the
  collection header is a curator's claim, not a licence grant):
  - <https://opengameart.org/content/cc0-cinematic-music> (45 items; header says "No attribution is required")
  - <https://opengameart.org/content/good-cc0-music> (40 items; includes "Horror Loop", "Dark Theme",
    "Haunting piano", "Night Prowler", "Forgoten tomb ambience", "Oriental", "Orien2", "Ninja Theme")
  - <https://opengameart.org/content/cc0-fantasy-music-sounds>
  - OGA advanced search with licence filter:
    <https://opengameart.org/art-search-advanced> → Art Type = Music, License = CC0

### 2.3a Two more high-value Chinese-themed sources **[delegated]**

**John Bartmann — *Chinese Valentine* (7 tracks)** — FMA — **CC0 1.0** ★ **the most friction-free Chinese
material found on any site.**
<https://freemusicarchive.org/music/John_Bartmann/chinese-valentine>
All **seven** tracks individually checked → all CC0 1.0, e.g.
<https://freemusicarchive.org/music/John_Bartmann/chinese-valentine/floating-lanterns/> — *"Floating Lanterns
by John Bartmann is licensed under a CC0 1.0 Universal License."*
Tracks/lengths: Cherry Blossom 2:07 · Village Temple Romance 1:41 · Neon City Love Affair 2:18 ·
Floating Lanterns 2:58 · Long Way To Kunming 1:57 · Oriental Bossa 2:01 · Dumplings For Two 2:02. MP3.
**Phase 1 beds** — melodic and Chinese-flavoured, but **not horror**: you will need to pitch down and add
reverb, and none are authored as loops. No attribution required, redistribution fine.

**Geoff Harvey / Purple Planet Music — *Eastern Horizons* (27 tracks)** — FMA — **CC BY 4.0**
<https://freemusicarchive.org/music/geoff-harvey-purple-planet-music/eastern-horizons>
All **27** tracks checked → all CC BY 4.0, e.g. *Temple of Hangzhou* (2:06) and *Chinese Dragon* (2:02).
Uploaded 11 Jul 2026. MP3. Phase 1 — production-music pastiche rather than horror. Attribution + a
modification note required (§1.3). ⚠️ Purple Planet's own site returns an unrendered SPA shell, so its
separate terms could not be read; the FMA track pages are the controlling verification.

**Arkeia (Joshua McLean) — *FREE Music Pack 6: Horror*** — itch.io — **CC BY 4.0** — **the only pack found
that already ships `.ogg`** <https://arkeiamusic.itch.io/music-pack-6> **[delegated]**
*"License: Creative Commons International 4.0 Attribution"* → <https://creativecommons.org/licenses/by/4.0/>;
*"This music is free to use in free and commercial projects."* Mandatory credit, quoted verbatim:
*"Contains music ©2026 Arkeia (https://arkeiamusic.itch.io/) Licensed under Creative Commons Attribution 4.0
International"*. 5 horror BGMs, **both WAV (95 MB) and OGG (14 MB) archives**. Phase 1.

### 2.4 What was checked and REJECTED (the negatives are the valuable part)

**License-based rejections — CC BY-NC / NC-ND [delegated]:**
- SUB8ION — *Moon Temple*, *Sacred Mountains*, *Jade River*, *Jade Serenity* — **CC BY-NC-ND 4.0**.
  **This is the closest thing on FMA to a Chinese ambient game soundtrack, and it is NC-ND.** ✗
- Kharkov — *Guzheng* (15:22) — CC BY-**NC** 4.0 ✗
- Pistol Jazz — *Hi no Tori (Taiko solo)* — CC BY-**NC-ND** 4.0 ✗
- MASERPAN — *Guqin* — CC BY-**NC-ND** 4.0 ✗
- Alex Mason — *Ritual* — CC BY-**NC** 4.0 ✗ · Audiobinger — *China Town* — CC BY-**NC** 4.0 ✗
- Invisible China — *Yellow Cab* — CC BY-**NC-ND** 3.0 US ✗
- FMA's **Gagaku** genre page is **empty (0 tracks)** ✗; **Chinese Opera** genre page is largely mis-tagged ✗

**Bespoke / unclear licences on itch.io — unusable [delegated]:**
- **Crow's Free Commercial Music Pack ⑥ Chinese style music Pack** (Crowbacillus) — **no CC licence**:
  *"You may not modify or claim attribution rights to these works in any way"*, mandatory
  *"Music by [乌鸦Producer]"*, no resale.
- **20 Oriental Ninja Tunes** (RetroVandal) — **licence completely unclear**: the page says only *"Credits
  appreciated but not essential ;-)"*; the only commercial grant is a comment reply *"Yeah sure, feel free."*
  Tagged Atmospheric/Dark/Spooky with 20 looped WAVs — a painful loss, but unusable.
- **Free Traditional Chinese Music Pack I** (Xiaoyi) — *"You may not resell or redistribute the standalone
  audio files"* → a distributed mod redistributes. ✗
- **Horror Music Pack – Burial / Underneath** (juanjo_sound) — *"You do NOT have permission to: Re-upload,
  remix, or redistribute the music as your own."* ✗
- **Free Music Tenkaippin-天下一品** (T-STUDIO) — bespoke agreement, not CC; also tagged **AI Assisted**. ✗
- **Fantasy Music Mega Pack** (Blacis) — itch field says CC0 1.0 **but it is AI-generated**
  (*"*Ai generated/modified by hand"*, "AI Generated" tag). Flagged, not recommended.
- **Tallbeard Studios / Abstraction Music — FREE Music Loop Bundle** — <https://tallbeard.itch.io/music-loop-bundle>
  — **CC0 1.0**, 200+ seamless loops, *"waived all copyright… may be modified in any way the user chooses"* —
  **licence is perfect, but the tags are ambient/chiptune/upbeat → not genre-matched.** Backup only.

**Practical notes from the delegated pass:**
- **itch.io has no browsable CC-BY filter**: `itch.io/game-assets/assets-cc-by` = 404;
  `…/assets-cc0/free/tag-music` = 403 (Cloudflare). Only
  <https://itch.io/game-assets/assets-cc0/tag-music> works (53 CC0 music packs).
- **FMA search is title-only** — `erhu`, `suona`, `dizi`, `yangqin`, `taoist`, `gagaku` all return **zero**
  results. Erhu/guzheng material is not discoverable by instrument name there.
- **FMA's licence filters do not work via URL** — every `license-type=` combination returned the same count.
  Per-track verification is mandatory.
- **OGG-ready among the new finds: only Arkeia** (*FREE Music Pack 6: Horror*, CC BY 4.0, ships both WAV and
  OGG archives: <https://arkeiamusic.itch.io/music-pack-6> **[delegated]**). Everything else is WAV/MP3.

### 2.5 Suggested two-phase build

- **Phase 1 bed:** BiteMe Games *Lotus Pond* (CC0, loop-ready) → layered with **Fantasy Choir** (CC0) as a
  chant pad → plus **Dark Shrine Loop** (CC0) as an intro/ambient bed.
- **Phase transition:** **Breves dies hominis** (CC0) — the bare voice lands the 无常 reveal.
- **Phase 2 bed:** BiteMe Games *Dragon Dance* (CC0, loop-ready, same instrumentation as Phase 1) → layered
  with **Drums of Dawn** (CC BY 3.0, taiko) → optionally **Taiko drums (seamless loop)** (CC BY 3.0).
- **Phase 2 alternative / heavier:** **Boss Battle Music** (CC0) or **Colossal Boss Battle Theme** (CC BY 3.0).
- **Erhu colour throughout:** **Views From Atop the Jade Kings Throne** (CC BY 3.0) — or Majadroid's
  *Samurai Nights* stems (CC BY 4.0) if you want to build both phases from one source.
- **The all-CC0 path** (zero attribution obligations): Dark Shrine Loop + BiteMe *Lotus Pond*/*Dragon Dance* +
  Fantasy Choir + Breves dies hominis + Boss Battle Music + Determined Pursuit + Sinister Boss Appears.
  **If you want zero legal surface area, ship only this set and skip the CC BY tracks entirely.** You lose the
  erhu track and Matthew Pablo's choir-and-taiko pieces, which is a real but survivable loss.

### 2.6 Public-domain / CC0 *recordings* — Wikimedia Commons, Musopen, IMSLP

> Sourced from a delegated research pass that read each Commons `File:` page (via the MediaWiki API), the
> IMSLP wiki, and Musopen via Wayback. Licences are quoted verbatim from the file pages. I have **not**
> personally re-fetched these; the quotes and the §1401 statute text are the sub-agent's. Re-check at
> download time.

#### 2.6.0 The two-tag rule, and the verified cutoff

✅ **17 U.S.C. §1401**, quoted (<https://www.copyright.gov/title17/92chap14.html>):

- "(a)(2)(A)(i)(I) through December 31 of the year that is 95 years after the year of first publication"
- "(B)(i) Pre-1923 recordings … shall end on December 31 of the year that is 3 years after the date of
  enactment"
- "(B)(ii) 1923–1946 recordings … shall end on the date that is 5 years after the last day of the period
  described in subparagraph (A)(i)(I)" → 95+5 = **100 years**
- "(B)(iii) 1947–1956 … 15 years after" → **110 years**; "(iv) Post-1956 … **February 15, 2067**"

⇒ ≤1922→PD 2022 · 1923→2024 · 1924→2025 · **1925→1 Jan 2026** · **1926→1 Jan 2027** · 1927→2028 · 1931→2032 ·
1953→2064.

Corroborated by the Cornell/Hirtle chart (updated 1 Jan 2026,
<https://guides.library.cornell.edu/copyright/publicdomain>: "Before 1926 | None | In the public domain"),
Commons `Template:PD-US-record` ("Recordings that were first published prior to 1926 are in the public
domain" — the year is computed as `{{CURRENTYEAR}} - 100`), and Commons `Template:PD-traditional`.

⚠️ **On Commons, a PD file needs TWO tags — composition *and* recording.** ✅ Musopen's own FAQ states the
principle: *"there are at least two copyrights to any sound recording of a musical work – the copyright on
the underlying composition and the copyright on the particular performance."* **If only the composition tag
is present, the recording is not cleared.**

#### 2.6.1 Musopen — ⚠️ **a discovery tool, not a licensor**

⚠️ `musopen.org` returns **HTTP 403 (Cloudflare)** on every path; all findings are from Wayback snapshots.

ToS dated **18 Aug 2017** (<https://web.archive.org/web/20250117015636/https://musopen.org/page/tos/>):

- §5 carves PD out: *"Musopen Materials do not include: … (ii) any materials in or otherwise dedicated to
  the public domain."* and *"nothing in these Terms shall be deemed or interpreted to restrict or limit your
  rights with respect to public domain works available through the Musopen Service."*
- §5 restriction: *"you agree not to sell, license, distribute, copy, modify, publicly perform or display,
  transmit, publish, edit, adapt, create derivative works from, or otherwise make unauthorized use of the
  Musopen Materials."*
- §21.4 (caps): *"**THE MUSOPEN ENTITIES DO NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE COPYRIGHT
  STATUS OR OWNERSHIP OF ANY WORKS LISTED OR MADE AVAILABLE ON THE MUSOPEN SERVICE.**"*

FAQ (<https://web.archive.org/web/20240229224525/https://musopen.org/faq/>):

- #5: *"**Musopen cannot guarantee that any music uploaded by its users is, in fact, in the public domain**…
  Musopen does not review music uploaded by users of the site to determine if the music is in the public
  domain or subject to copyright."*
- #6: *"Public domain works are not protected by U.S. copyright law and are free to be used, copied,
  performed and distributed by anyone for any purpose, even if sold for profit."*

**Two traps:** the licence filter (archived browse page, 2 Jan 2025,
<https://web.archive.org/web/20250102165730/https://musopen.org/music/>) offers CC-BY, CC-BY-ND,
**CC-BY-NC-ND**, **CC-BY-NC**, **CC-BY-NC-SA**, CC-PD, CC-BY-SA — so PD, CC BY and CC BY-NC/ND tracks sit
mixed in one listing, and the marketing line "All the music we host is royalty and copyright free" is
contradicted by its own filter. And Musopen renders "Public Domain" as **CC Public Domain Mark 1.0 — which is
NOT CC0.** PD Mark is a *statement* that a work is already PD; it grants nothing and warrants nothing, unlike
CC0.

**Verdict: "I got it from Musopen" is not a chain of title.** The Commons copies are strictly better, because
they carry a per-recording licence template and, for the 2008 Musopen batch, a volunteer-reviewed VRT/OTRS
permission ticket.

#### 2.6.2 IMSLP — ❌ not practical

✅ Fetched live; `IMSLP:Performance_Restricted` redirects to IMSLP:Licensing_Policy_and_Guidelines:

> "For works that are in the public domain in Canada, recordings can be accepted, so long as the recordings
> are in the public domain in Canada (i.e., **published in 1964 or earlier**), or released under a free
> license. For works that are in the public domain in the US but not in Canada, recordings can only be
> accepted if **published in 1923 or earlier** or released under a free license."

→ **IMSLP clears recordings against *Canadian* law, not US law, and both figures are stale.** Its **PRL**
licence (<https://imslp.org/wiki/IMSLP:Performance_Restricted_Attribution_Non-commercial_No_Derivatives_1.0>)
"does not allow for-profit (i.e. commercial) use… does not allow the creation of Adaptations… **does not allow
public performance/broadcasting/recording**", and the policy states *"PR-licensed files are **not free for
any purpose**."* Of 5 *Danse macabre* recordings on IMSLP, 3 are CC BY-NC(-ND) 3.0. A PD-labelled recording
can also be a stub (`'O sole mio`, Caruso 1916, resolves to a 1,822-byte PDF, not audio).
**Treat IMSLP as a score library. Not an audio source.**

#### 2.6.3 Recommended recordings — Tier A (globally clean, no attribution required)

**A1. Holst — "Saturn, the Bringer of Old Age" (The Planets, mvt V)** ⭐ **best slow-phase pick.**
London Symphony Orchestra, **conducted by Holst himself** (Columbia, 1922–23).
<https://commons.wikimedia.org/wiki/File:Holst_-_Saturn.ogg> — Ogg, 4.35 MB, **7:00**.
Licence verbatim: `{{Copyright information|recording={{PD-US-record-expired|hide_us_warning}}|musical
composition={{PD-old-auto-expired|deathyear=1934}}}}` → rendered "Public domain".
A limping 5/4 ostinato that grinds you down — literally "the bringer of old age".

**A2. Holst — "Neptune, the Mystic" (mvt VII)** — same performer/licence.
<https://commons.wikimedia.org/wiki/File:Holst_-_Neptune.ogg> — Ogg, 3.56 MB, **5:32**.
Wordless female chorus dissolving into silence. Ideal **outro**.

**A4. Holst — "Mars, the Bringer of War"** ⭐ **best aggressive-phase pick, and the cleanest audio here.**
**United States Air Force Heritage of America Band**, transcr. Merlin Patterson (1998).
<https://commons.wikimedia.org/wiki/File:Holst-_mars.ogg> — Ogg, 7.30 MB, **7:57**.
Licence verbatim: `;Performance {{PD-USGov-Military-Air Force}} ;Composition
{{PD-old-auto-expired|deathyear=1934}}`. PD-USGov text: *"a work of a U.S. Air Force Airman or employee,
taken or made as part of that person's official duties. As a work of the U.S. federal government, the image
or file is in the **public domain** in the United States."* ⚠️ **Tier B — US-PD only (§105).**

**A5. Holst — "Uranus, the Magician" (mvt VI)** — same band/licence.
<https://commons.wikimedia.org/wiki/File:Holst-_uranus.ogg> — Ogg, 4.71 MB, **5:19**.
Lurching scherzo ending in a monstrous crescendo = a natural **phase-transition cue**. ⚠️ Tier B.

**A6. Berlioz — "Songe d'une nuit de sabbat" (Symphonie fantastique, mvt V)** ⭐ **most on-brief piece found.**
**U.S. Navy Band Concert Band**, transcr. Lt. Col. Jack T. Cline.
<https://commons.wikimedia.org/wiki/File:A_Dream_of_a_Witches%27_Sabbath_-_transcribed_by_Lt._Col._Jack_T._Cline_-_U.S._Navy_Band_Concert_Band.ogg>
— Ogg, 28.59 MB, **9:53**. Licence verbatim: `;Composition{{PD-old-100}} ;Performance{{PD-USGov-Navy}}`.
Witches' sabbath + a mocking *Dies irae* — the closest thing in the repertoire to a cosmic-horror boss theme.
⚠️ Tier B. (A U.S. Marine Band version at **9:04** also exists; its `|Permission=` says Navy while the
description says Marine Band — a tagging inconsistency, both are US federal works.)

**A7. Wagner — "Siegfried's Funeral March and Finale" (Götterdämmerung)**
United States Marine Corps Band, 8–11 Dec 1981.
<https://commons.wikimedia.org/wiki/File:Siegfrieds_funeral_march_and_finale.ogg> — Ogg, 11.97 MB, **10:30**.
`{{PD-USGov-Military-Marines}}`. Doom-laden then apocalyptic. ⚠️ Tier B, and the **lowest margin** of the
§105 items: a 1981 fixation means EU neighbouring rights run to ~2051.

**A8. Tchaikovsky — Symphony No. 6 "Pathétique", mvt IV Finale: Adagio lamentoso** — Musopen Symphony /
Skidmore College Orchestra.
<https://commons.wikimedia.org/wiki/File:Tchaikovsky,_Symphony_No._6_in_B_minor,_Op._74,_%27Pathetique%27_-_IV._Finale_Adagio_lamentoso.ogg>
— Ogg, 14.35 MB, **10:19**.
Licence verbatim: `{{Copyright information|recording={{Cc-zero}}|musical composition={{PD-old-auto-expired|deathyear=1893}}}}`,
file sits in **Category:CC-Zero**. **The recording is CC0 ⇒ globally clean.** Total despair.

**A9. Tchaikovsky — Pathétique mvt I** — same `recording={{Cc-zero}}`.
<https://commons.wikimedia.org/wiki/File:Tchaikovsky,_Symphony_No._6_In_B_Minor,_Op._74,_%27Pathetique%27_-_I._Adagio,_Allegro_Non_Troppo.ogg>
— Ogg, 22.19 MB, **17:31**. Covers both phases in one file.

**A10. Mussorgsky — "Night on Bald Mountain"** — Skidmore College Orchestra.
<https://commons.wikimedia.org/wiki/File:Modest_Mussorgsky_-_night_on_bald_mountain.ogg> — Ogg, 12.87 MB,
**12:13**. `Music: {{PD-old-100}}` / `Recording: {{PD-author|[http://musopen.com Musopen]}}` +
`{{PermissionTicket|id=2008012110017088}}`. Rendered VRT text: *"This work is **free and may be used by
anyone for any purpose**… The Wikimedia Foundation has received an e-mail confirming that the copyright
holder has approved publication under the terms mentioned on this page."*
**Strongest provenance in the list** — a PD-author dedication *plus* a reviewed permission ticket.
**Both phases.**

**A11. Mussorgsky/Ravel — Pictures at an Exhibition, Skidmore College Orchestra** — all `{{cc0}}`, from FMA.
Machine-readable: LicenseShortName "CC0", AttributionRequired false. ⚠️ **Each page carries a
`{{Licencereview}}` banner — the CC0 claim has NOT been independently reviewed on Commons. This is the
biggest gap in the chain of title; see §5.** Prefix all with `https://commons.wikimedia.org/wiki/`:
- **VIII. Catacombae. Sepulcrum romanum (Largo)** — `File:Skidmore_College_Orchestra_-_12_-_VIII_Catacombae_Sepulcrum_romanum_Largo.ogg` — **1:53**, 2.26 MB — **slow** — a chorale in a Roman tomb.
- **Cum mortuis in lingua mortua** — `File:Skidmore_College_Orchestra_-_13_-_Cum_mortuis_in_lingua_mortua_Andante_non_troppo_con_lamento.ogg` — **1:53**, 2.20 MB — **slow** — "with the dead in a dead language". **Best title-to-brief match in this report.**
- **IX. La Cabane sur des pattes de poule (Baba Yaga)** — `File:Skidmore_College_Orchestra_-_14_-_IX_La_Cabane_sur_des_pattes_de_poule_Allegro_con_brio_feroce_An.ogg` — **3:33**, 4.25 MB — **fast**.
- **I. Gnomus** — `File:Skidmore_College_Orchestra_-_02_-_I_Gnomus_Vivo.ogg` — **2:44**, 3.31 MB.

**A12. Weber — Der Freischütz Overture** — Skidmore College Orchestra.
<https://commons.wikimedia.org/wiki/File:Carl_Maria_von_Weber_-_der_freischutz,_j._277_-_overture.ogg> —
Ogg, 13.15 MB, **10:18**. `{{PD-author|Musopen}}` + the same VRT ticket. The opera that invented the demonic
Wolf's Glen; tremolando C-minor opening. Slow→fast.

**A13. Saint-Saëns — Danse macabre** — Philadelphia Symphony Orchestra cond. **Leopold Stokowski**, rec.
29 Apr 1925 (Victor 6505).
<https://commons.wikimedia.org/wiki/File:PhiladelphiaSymphonyOrchestra-DanseMacabre.ogg> — Ogg, 3.71 MB,
**6:59**. `{{PD-US-record-expired}}`. ✅ **1925 → 已于 2026-01-01 进入美国公有领域，本条现已解禁**
（调研写于 2025 年，当时它还是"零余量"的最新一项；以 2026-09-27 的日期算，它已经干净了）。
美国以外同样干净（EU 邻接权 1995 年到期）。

**A14. Beethoven — 32 Variations in C minor, WoO 80** — "La Pianista" (2010).
<https://commons.wikimedia.org/wiki/File:Beethoven_-_32_Variations_in_C_Minor,_WoO_80.ogg> — Ogg, 12.27 MB,
**12:08**. `===Composition=== {{PD-old-100}} ===Performance=== {{self|Cc-zero}}`, Category:CC-Zero,
Wikimedia Featured sound. Obsessive hammering C-minor ostinato. **Fast/tension.**

**A15. Chopin — Scherzo No. 3 in C-sharp minor, Op. 39** — La Pianista.
<https://commons.wikimedia.org/wiki/File:Chopin_-_Scherzo_No._3.ogg> — Ogg, 7.36 MB, **7:31**.
`{{self|Cc-zero}}` on the performance. Fast, with a hymn-like chorale mid-piece = a built-in phase change.

**A16. Ravel — String Quartet mvt II (Assez vif – très rythmé)** — U.S. Army Band.
<https://commons.wikimedia.org/wiki/File:Ravel_-_String_Quartet,_mvt_2_-_US_Army_Band.ogg> — Ogg, 15.70 MB,
**5:50**. Pizzicato, driving, strange. ⚠️ Tier B.

#### 2.6.4 Tier B — Chinese / imperial flavour (thin, and honestly so)

**Read this first:** **Commons has no usable full-length PD/CC0 recording of traditional Chinese instrumental
music.** Every full-length erhu/guzheng/pipa recording found is **CC BY-SA 4.0**, and two have compositions
still in copyright (see rejections). What exists is early-1900s acoustic-era transfers plus short CC0 samples.
**Plan: import the Chinese timbre as short CC0 samples/stingers, carry the "imperial" feeling with the 1914
Qing anthem, and take the long dark beds from §2.6.3.**

**B1. "Gong Jin'ou" (鞏金甌) — Qing dynasty national anthem, 1914** ⭐ **the imperial leitmotif.**
Victor Military Band, matrix B-15202, rec. 18 Sep 1914.
<https://commons.wikimedia.org/wiki/File:Cup_of_Solid_Gold_1914.ogg> — Ogg, 674 KB, **1:03**.
`{{PD-US-record}}` (a caution template — "may be deleted in the future" — applied because 1914 is
unambiguously in the pre-1926 bracket). Use as a leitmotif/intro sting. Globally clean.
**This is the single most on-brief Chinese item found anywhere in this research.**

**B2. "Chinese Vocal and Instrumental Ensemble" (1903)** — performer unknown; source LoC National Jukebox.
<https://commons.wikimedia.org/wiki/File:Chinese_Vocal_and_Instrumental_Ensemble.ogg> — Ogg, 2.48 MB, **2:54**.
`{{PD-US-record}}`, dated 1903-08-07. ⚠️ LoC frames its collection as permission-based, but a 1903 fixation
is US-PD by statute regardless. Severe fidelity = **useful grain for cosmic horror**, as a treated layer.

**B4. "Tune of Li Zhongtang" / "Praise the Dragon Flag"** — Kapoios2026 (own work, 2026), aural transcription.
<https://commons.wikimedia.org/wiki/File:Tune_of_Li_Zhongtang.ogg> — Ogg, 247 KB, **0:24**.
`{{self|cc-zero}}`, LicenseShortName **"CC0"**. CC0 imperial raw material to chop/pitch/stretch.
Too short to be BGM on its own.

**B5. Chinese gong (Tao-Chi gong, Duisburg 2014)** — the_very_Real_Horst, Freesound #240382.
<https://commons.wikimedia.org/wiki/File:240382_the-very-real-horst_chinese-gong-finish-session-2014-06-10-29-143.wav>
— `{{cc-zero}}`, **CC0**. **WAV, 5.14 MB, 0:29** (transcode needed). Boss-phase **gong stinger**.

**B6. Dizi (Chinese flute) sample** — Gorgoroth6669, Freesound #108242, licence independently reviewed on
Commons (`{{LicenseReview|user=INeverCry|date=2016-01-10}}`).
<https://commons.wikimedia.org/wiki/File:DiZi_Chinese_Flute_Sample.ogg> — `{{Cc-zero}}`, **CC0**.
Ogg, 303 KB, **0:17**. Process into your own motif.

**B7. Taiko (Scott Harding's Taiko, live)** — Singapore Wind Symphony Percussion Ensemble.
<https://commons.wikimedia.org/wiki/File:02_Taiko2_(short).oga> — `{{cc-zero}}`, **CC0**. Ogg, 357 KB,
**0:25**. Percussion bed for the aggressive phase.

**C1. Holst — "Mars" (Skidmore College Orchestra)** — ⚠️ **attribution required, credit "Musopen".**
<https://commons.wikimedia.org/wiki/File:Gustav_Holst_-_the_planets,_op._32_-_i._mars,_the_bringer_of_war.ogg>
— `{{attribution|nolink=[http://musopen.com Musopen]|text=Musopen}}`, LicenseShortName "Attribution".
Ogg, 11.02 MB, **8:18**; VRT ticket 2008012110017088. **Use only if you will credit "Musopen" — otherwise
use A4, the same piece with no obligations.**

#### 2.6.5 Rejected — with reasons

- **二泉映月 (Abing, erhu)** — `{{cc-by-sa-4.0|张沛坚}}` + VRT. Rejected: not PD/CC0 (ShareAlike) **and** the
  file's own Date field says composed/performed in the **1950s** ⇒ composition still in US copyright.
  Heartbreaking — it is the perfect mood.
- **江河水 (erhu)** — same CC BY-SA 4.0 tag; Date says composed **1962**. ✗
- **连环扣 / 雨打芭蕉** — compositions PD, **recordings CC BY-SA 4.0**; also bright/cheerful. ✗
- **Saint-Saëns — Danse macabre (Fourestier, 1953).flac** — tagged `{{PD-old-70-1923}} {{PD-EU-audio}}
  {{PD-US-record}}`. **REJECTED AS UNSAFE.** Rec. Feb 1953 ⇒ §1401(a)(2)(B)(iii) ⇒ **US protection until
  2063.** The tags establish EU PD only. **Do not ship.**
- **The entire `PDP-CH` / "Swiss Foundation Public Domain" series** — tagged `{{Pdproject}} {{PD-old-70}}`.
  **REJECTED.** `Template:Pdproject/i18n` renders **no licence at all**, only provenance; `{{PD-old-70}}`
  covers the *composer*, not the recording. A 1927 first release ⇒ US protection until 1 Jan 2028.
  **This is the composition/recording trap sitting in a large Commons category — the best argument for
  checking every file yourself.**
- **1931 Chinese recordings** (Kun-ch'ü P'i-p'a-chi etc.) — `{{PD-traditional}}`, but 1931 is in the
  1926–1946 bracket ⇒ **not US-PD until 1 Jan 2032.** ✗
- **`File:Baima Diao.ogg`** — licence is right (`{{self|cc-zero}}`) but the file carries a live **deletion
  nomination** (Aug 2026). Unstable. ✗
- **All Kevin MacLeod / Incompetech material on Commons** (incl. *Tea Roots* erhu, *Shenyang* erhu/pipa,
  *Mountain Emperor* taiko) — all **CC BY 3.0**, not PD/CC0. See §1.8.
- **PeriTune "Wuxia3"** — `{{YouTube CC-BY|PeriTune}}` with an **unresolved `{{LicenseReview}}`**.
  Aesthetically on-brief, licence unverified. ✗
- **"Yu Wang Tan Min" (c. 1920)** — `{{PD-traditional}}`, but the date is "circa" and the tag leans on a
  German 50-year term that no longer matches EU law (70 yrs). **Flagged, not cleared.**
- **`File:Holst, The Planets, Op. 32 - I. Mars…ogg`** (61-second Skidmore excerpt, `{{cc-zero}}`) — the CC0
  tag is a 2026 third-party uploader's, not Musopen's, and it is a 1-minute excerpt. ✗
- Also rejected: **IMSLP PRL**; anything CC BY-NC/ND/NC-ND; **Tabletop Audio** (CC BY-NC-ND 4.0);
  **ccMixter** (no CC0 tier); **Internet Archive Great 78 Project** (*"made available for use in research,
  teaching, and private study only. Copyrights that may exist in these materials have not been transferred
  to the Internet Archive"* — **not a PD grant**); **LoC National Jukebox** (`"rights_restricted": true`);
  **Sample Focus** / **Sonniss GDC** (custom licences; Sonniss is SFX).

#### 2.6.6 Practical usability of Commons audio

- **Formats:** `.ogg` (Vorbis), `.oga`, `.opus`, `.flac`, `.wav`, `.mp3`. **Minecraft needs Ogg Vorbis**, so
  `.ogg`/`.oga` drop straight in. FLAC/WAV/MP3 need
  `ffmpeg -i in.flac -c:a libvorbis -q:a 5 out.ogg` (§3.3).
- **Complete movements, not excerpts** — typically 2–13 minutes.
- **Quality varies enormously, and it cuts both ways:** 1903/1914 acoustic transfers are very lo-fi, mono and
  noisy (an *aesthetic asset* for cosmic horror, but treat before mixing under anything heavy); 1923/1925 are
  narrow-band but musical; 1977–2010 are clean stereo.
- **⚠️ Loopability: none of these are loop-ready.** They are through-composed concert movements. Budget for
  editing: pick a 20–40 s section and crossfade (§3.4), or use them as **one-shot boss themes** — which is
  closer to how Minecraft itself uses music anyway.
- **Sizes:** Commons Oggs run 0.25–28 MB; the FLACs (279 MB, 191 MB) and the 5 MB WAV need conversion first.

#### 2.6.7 A globally-clean two-phase soundtrack, if you want one

Using **Tier A only** (no §105, no attribution obligations):

- **Phase 1 (slow):** A1 *Saturn* + A11 *Catacombae* + A10 *Night on Bald Mountain* (slow section).
- **Phase 2 (fast):** A10 *Night on Bald Mountain* (fast section) + A11 *Baba Yaga* + A13 *Danse macabre* +
  A14 *Beethoven 32 Variations*.
- **Stingers / texture:** B1 Qing anthem (1914) + B5 gong + B6 dizi + B7 taiko + B4 *Li Zhongtang*.
- **Phase transition:** A2 *Neptune* (dissolving chorus) or A15 *Chopin Scherzo No. 3* (hymn-chorale midpoint).

Blend that with the CC0 game-music beds in §2.1–2.3 and you have a complete soundtrack with **zero
attribution obligations and no territorial caveats**.

---

## 3. Practical bundling notes

### 3.1 Required audio format

- ✅ **Ogg Vorbis, `.ogg`.** Fabric's official docs: *"OGG Vorbis is an open container format for multimedia
  data, such as audio, and is used in case of Minecraft's sound files."*
  <https://docs.fabricmc.net/develop/sounds/custom>
- ✅ Minecraft decodes Ogg through `OggAudioStream` (`net.minecraft.client.sound.OggAudioStream`), which wraps
  **stb_vorbis** and decodes to **float**. The javadoc shows `private long pointer` (the stb_vorbis handle),
  `readChannels(FloatBuffer, FloatBuffer, ChannelList)`, and `getBuffer(int)`:
  <https://maven.fabricmc.net/docs/yarn-21w15a+build.2/net/minecraft/client/sound/OggAudioStream.html>
  → **Consequence:** because the decode path goes to float, 24-bit or float sources are not inherently
  rejected the way a hard 16-bit PCM decoder would reject them. 16-bit is still the safe, conventional choice
  and is what vanilla assets use.
- **Sample rate: ⚠️ no primary-source requirement found.** Vanilla assets and essentially all community
  guidance use **44100 Hz**, and the OpenAL backend resamples as needed. Use 44.1 kHz. Do not ship 48 kHz or
  22.05 kHz unless you have tested it.
- **Bit depth: ⚠️ no primary-source requirement found.** Community folklore says "must be 16-bit"; I found no
  primary source confirming that, and the stb_vorbis→float path argues against it. Export 16-bit anyway —
  it is the default everywhere and costs nothing.
- ✅ **File naming is a real trap.** The Minecraft Wiki states the sound `name` "cannot contain whitespace
  characters", and — critically — **"Failure to follow this guideline in at least one entry will result in the
  entire `sounds.json` being ignored, in favor of vanilla sounds."**
  <https://minecraft.wiki/w/Sounds.json>
  → `snake_case` filenames only, no spaces anywhere in the path. This is the most common way a mod's sound
  file silently does nothing at all.

### 3.2 Mono vs. stereo — and why boss music is stereo

✅ VERIFIED, Minecraft Wiki `sounds.json`, verbatim:

> "If the sound file has one channel (mono), it can be played locationally (sound volume decreases the farther
> you are from the source). If the file has two channels (stereo), **the volume does not change** (for example
> music, ambient sounds)."

**Channel count *is* the positional / non-positional switch. There is no flag for it.**

- **SFX → mono.** A sword hit, a footstep, or a boss roar that should come *from* the boss must attenuate with
  distance, or the audio contradicts the visuals. Fabric's guide frames this as a requirement — *"To avoid
  problems with how Minecraft handles distancing, your audio needs to have only a single channel (Mono)"* —
  **but note that guide is written around a whistle-SFX example**, so read it as SFX guidance, not a universal
  law.
- **Boss music → stereo, deliberately.** A boss theme must play at constant volume for every player in the
  arena regardless of position. Mono boss music would fade as the player backs away, and positional playback
  of a full mix sounds broken. **Keep both phase tracks stereo.**
- **You can have both:** if you want a positional musical element (distant chanting emanating from the boss's
  altar), make that a **separate mono** file and play it positionally while the stereo bed plays non-positionally.

### 3.3 ffmpeg commands

> ⚠️ **ffmpeg is not installed on this machine** (`Get-Command ffmpeg` → not found), so **I could not run
> these**. They are written against verified option semantics; run them and listen.

✅ VERIFIED option semantics from the official FFmpeg trac wiki,
<https://trac.ffmpeg.org/wiki/TheoraVorbisEncodingGuide>:

- `-qscale:a` / `-q:a` range is **-1.0 to 10.0**, 10 = highest. Default is **`-q:a 3`, targeting 112 kbps.**
- Bitrate formula: **`16×(q+4)` below 4, `32×q` below 8, `64×(q-4)` otherwise.**
  → q=-1 ≈ 48 · q=0 = 64 · q=1 = 80 · q=2 = 96 · q=3 = 112 · q=4 = 128 · q=5 = 160 · q=6 = 192 ·
  q=7 = 224 · q=8 = 256 · q=9 = 320 · q=10 = 384 kbps.
- `-ac` sets channel count and `-ar` sets sample rate (both generic libavcodec audio options, ✅ documented at
  <https://ffmpeg.org/ffmpeg-codecs.html>).

**Standard conversion — stereo, for boss music:**
```bash
ffmpeg -i input.wav -ac 2 -ar 44100 -c:a libvorbis -q:a 6 output.ogg
```
`-q:a 6` ≈ 192 kbps — good for a dense orchestral/percussion mix. `-q:a 5` (160 kbps) is usually
indistinguishable on game audio and ~17% smaller. `-q:a 4` (128 kbps) is fine for ambient pads.

**Mono, for a positional SFX:**
```bash
ffmpeg -i input.wav -ac 1 -ar 44100 -c:a libvorbis -q:a 5 output.ogg
```

**Downmix stereo → mono without phase weirdness** (prefer this over bare `-ac 1` when the source has wide
stereo content):
```bash
ffmpeg -i input.wav -af "pan=mono|c0=0.5*c0+0.5*c1" -ar 44100 -c:a libvorbis -q:a 5 output.ogg
```

**Batch-convert a folder (PowerShell, Windows):**
```powershell
Get-ChildItem *.wav, *.mp3, *.flac | ForEach-Object {
  ffmpeg -i $_.FullName -ac 2 -ar 44100 -c:a libvorbis -q:a 6 ("{0}.ogg" -f $_.BaseName)
}
```

**Loudness-normalise before encoding** (EBU R128). Minecraft does not normalise for you, and your two phases
must match or the transition will jump in volume:
```bash
# 1) measure
ffmpeg -i input.wav -af loudnorm=I=-16:TP=-1.5:LRA=11:print_format=summary -f null -
# 2) apply (one-pass; or feed the measured values back for two-pass)
ffmpeg -i input.wav -af loudnorm=I=-16:TP=-1.5:LRA=11 -ar 44100 -ac 2 -c:a libvorbis -q:a 6 output.ogg
```
Target ≈ **-16 LUFS integrated, -1.5 dBTP** for music. Vanilla Minecraft music sits around -16 to -18 LUFS.
⚠️ The `loudnorm` filter syntax is standard ffmpeg but I did not verify it against the primary docs in this
session — check `ffmpeg -h filter=loudnorm` on your machine.

**Verify what you produced:**
```bash
ffprobe -v error -show_entries stream=codec_name,sample_rate,channels,duration,bit_rate \
  -of default=noprint_wrappers=1 output.ogg
```

### 3.4 Making it loop seamlessly

✅ **VERIFIED: `sounds.json` has no loop field.** The wiki's per-sound key list is exactly: `name`, `volume`,
`pitch`, `weight`, `stream`, `attenuation_distance`, `preload`, `type`. **There is no native loop-point
support in Java Edition's `sounds.json` — the file plays once.**
<https://minecraft.wiki/w/Sounds.json>

✅ **Corollary: Ogg Vorbis loop-point metadata (the `LOOPSTART`/`LOOPLENGTH` comment convention that RPG Maker
and some engines read) is NOT honoured by Minecraft.** Do not rely on it.

Three real options:

**A. Seamless full-file loop — best for a boss fight.** Make the *entire file* loop cleanly from end to
start, then have your mod re-trigger it when it finishes. This is what "seamlessly looping" assets are for,
and why **Boss Battle Music** and **Taiko drums (seamless loop)** above are tagged that way.

The edit recipe:

1. **Work in samples, not seconds.** Find the exact loop length by selecting a bar-aligned region in Audacity
   and reading the sample offsets. Beat-aligned loops are the only ones that stay in time across cycles.
2. **Choose a boundary where the waveform is near-silent or phase-continuous** — typically a drum hit's decay
   tail rather than mid-note. Cutting at a zero crossing alone is not enough; you also need the *slope* to
   match.
3. **Fix the tail — do not fade it.** A fade-out at the end is the enemy of a seamless loop: it creates an
   audible volume dip every cycle. Instead, take the reverb tail that runs past your loop end and **wrap it
   around to the beginning**: copy the last N ms, paste at the start, then move the loop end back by N ms so
   the tail plays into the loop head. This is the single change that makes a loop sound continuous.
4. **Never prepend or append silence.** Silence at the head or tail creates a gap every cycle. (This is
   presumably why OGA's *Taiko drums* is described as "cut and encoded from the original".)
5. **Zoom to sample level at the join** and confirm continuity in both amplitude and slope.
6. **Empirical test:** loop it gaplessly (`mpv --loop-file=inf output.ogg`, or foobar2000 on repeat) and
   listen for a click or a rhythmic stumble. **If you can hear the seam, it isn't seamless.**

**B. Stem loops.** Some assets (notably *Samurai Nights*) ship individual loops. You can sequence them in
code, but that is real work. Cheaper: assemble your own 30–60 s bed in Audacity from the stems and export one
seamless file.

**C. Crossfade in code.** Play the tail while starting the head. Complex, and unnecessary if you do (A).

**Crossfading Phase 1 → Phase 2.** Two approaches:

- **Hard cut on phase change** — simplest, and visually justified when the boss transforms.
- **Overlap** — start the Phase 2 sound while Phase 1 is still fading; hold two `SoundInstance`s with
  independent volumes and ramp one down / the other up over ~1.5–2 s. (Java's `SoundInstance` API does not
  expose a seek, so you cannot ship one file and seek within it — ship one file per phase and manage the
  overlap yourself.)

**Implementation shape (Forge/NeoForge):** implement a client-side `TickableSoundInstance` whose `tick()`
detects that the underlying sound has stopped and re-triggers it; set it non-positional
(`Attenuation.NONE` / relative). **⚠️ I did not verify a specific Forge/NeoForge API signature in this
session** — check the current `TickableSoundInstance` interface in your mappings. The verified part is the
*design*: streaming + stereo + explicit re-trigger, because `sounds.json` will not loop for you.

### 3.5 `sounds.json` entry

✅ Structure verified against <https://minecraft.wiki/w/Sounds.json>:

`src/main/resources/assets/maledict/sounds.json`:
```json
{
  "music.maledict.first_vicissitude.phase1": {
    "subtitle": "sound.maledict.first_vicissitude.phase1",
    "sounds": [
      {
        "name": "maledict:music/first_vicissitude_phase1",
        "stream": true,
        "volume": 1.0,
        "pitch": 1.0,
        "weight": 1
      }
    ]
  },
  "music.maledict.first_vicissitude.phase2": {
    "subtitle": "sound.maledict.first_vicissitude.phase2",
    "sounds": [
      { "name": "maledict:music/first_vicissitude_phase2", "stream": true, "volume": 1.0, "pitch": 1.0 }
    ]
  }
}
```

- File goes at `src/main/resources/assets/maledict/sounds/music/first_vicissitude_phase1.ogg`.
- `name` is relative to `assets/<namespace>/sounds/`, **without the `.ogg` extension**, forward slashes,
  **no whitespace**.
- ✅ `stream: true` — wiki: *"It is recommended that this is set to 'true' for sounds that have a duration
  longer than a few seconds to avoid lag. Used for all sounds in the 'music' and 'record' categories."* And:
  *"setting it to false allows many more instances of the sound to be ran at the same time while setting it to
  true **only allows 4 instances** (of that type) to be ran at the same time."*
  → Two boss phases = 2 concurrent streams: comfortably inside the limit. Do not put 5 streaming tracks on one
  sound event.
- `preload: true` loads it with the pack rather than on first play — worth it for a theme that starts on a
  phase transition, to avoid a hitch.
- Register the `SoundEvent` in code. Fabric docs show
  `Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id))`.
  **⚠️ Forge/NeoForge-specific registration (`DeferredRegister` for `SoundEvent`) was not verified here.**
- Play it with `SoundSource.MUSIC` so it respects the Music volume slider — players expect this and it is
  polite.

### 3.6 Ogg file size budget

> ⚠️ **Computed from the verified libvorbis bitrate formula, not measured** (ffmpeg is not installed here).
> `size ≈ bitrate × duration ÷ 8`. A 2-minute track = 120 s.

| `-q:a` | ≈ bitrate | ≈ MB per 2 min | Notes |
|---|---|---|---|
| 3 (default) | 112 kbps | **1.7 MB** | audible artefacts on dense orchestral; fine for a drone/pad |
| 4 | 128 kbps | **1.9 MB** | good default for ambient / Phase 1 |
| **5** | **160 kbps** | **2.4 MB** | **sweet spot — near-transparent for most game audio** |
| **6** | **192 kbps** | **2.9 MB** | **safe for dense percussion + choir (Phase 2)** |
| 7 | 224 kbps | **3.4 MB** | diminishing returns |
| 8 | 256 kbps | **3.8 MB** | overkill for a mod |

**Two boss themes at `-q:a 6` ≈ 5.8 MB total. At `-q:a 5` ≈ 4.8 MB.** That is a completely reasonable
addition to a mod jar. For scale: several of the *source* files recommended above are 20–50 MB each
(*Taiko drums* upstream WAV is **38.2 MB** for 3:46 at 44.1 kHz/16-bit stereo), so the encode is doing roughly
10× compression.

**Budget guidance:**

- **Keep total new audio under ~10 MB** for a mod of this scope. Past that, players on slow connections notice
  the download and your CurseForge/Modrinth CDN cost grows for no perceptual benefit.
- **16-bit / 44.1 kHz / stereo / `-q:a 5–6`, and no higher.** 24-bit or 96 kHz for a mod is pure waste — the
  decoder throws the extra precision into float and OpenAL handles the rest.
- **Mono roughly halves the bitrate at the same `-q:a`**, so positional SFX in mono buy real space.
- Long sparse drones with near-silence are already exploited by Vorbis VBR — do not hand-tune.
- ⚠️ **Unverified memory caveat:** I could not confirm whether Minecraft's `stream: true` path holds the whole
  decoded PCM in memory. If it does, a 2-minute stereo 44.1 kHz track is roughly **21 MB of float PCM in RAM**
  regardless of file size. **Two streaming boss tracks should be fine; do not ship a 10-track streaming OST
  inside a boss mod.** Test on a low-RAM client.

---

## 4. Ready-to-use attribution file

**Two artifacts, both required.** `CREDITS.md` ships **inside the jar** (CC BY 3.0 §4(a): the licence URI must
accompany *every copy*; CC BY 4.0 §3(a)(1)(E)+(C): the material URI and licence URI must be retained). The
**in-game Credits screen** is where a human actually finds it (CC BY's "at least as prominent" rule;
Incompetech's own "credits screen found in the settings menu" guidance). Mirror `CREDITS.md` in the repo and
link it from the mod description.

### `CREDITS.md`

```markdown
# Credits

## Music

This mod bundles music written by other people. Each track below is listed with the
license it is used under and the exact changes that were made to the original file.

Full license texts:
* CC0 1.0 Universal ............ https://creativecommons.org/publicdomain/zero/1.0/legalcode.en
* CC BY 3.0 Unported ........... https://creativecommons.org/licenses/by/3.0/legalcode.en
* CC BY 4.0 International ...... https://creativecommons.org/licenses/by/4.0/legalcode.en

--------------------------------------------------------------------------------
### Phase 1 — "First Vicissitude" (measured)
--------------------------------------------------------------------------------

* "Dark Shrine Loop" by qubodup (a remix of "Shrine" by yd)
  Source:  https://opengameart.org/content/dark-shrine-loop
  License: CC0 1.0 Universal (Public Domain Dedication)
           https://creativecommons.org/publicdomain/zero/1.0/
  Changes: converted to Ogg Vorbis; level-normalised to -16 LUFS.
  No rights reserved. Credited here as a courtesy.

* "Lotus Pond" from "Chinese Game Music" by BiteMe Games
  Source:  https://bitemegames.itch.io/chinese-game-music
  License: CC0 1.0 Universal (Public Domain Dedication)
           https://creativecommons.org/publicdomain/zero/1.0/
  Changes: level-normalised to -16 LUFS; loop point trimmed.
  No rights reserved. Credited here as a courtesy.

* "Views From Atop the Jade Kings Throne" by Hitctrl
  Source:  https://opengameart.org/content/views-from-atop-the-jade-kings-throne
  License: Creative Commons Attribution 3.0 Unported (CC BY 3.0)
           https://creativecommons.org/licenses/by/3.0/
  Changes: converted from MP3 to Ogg Vorbis; trimmed and crossfaded to loop
           seamlessly; level-normalised to -16 LUFS.
  Copyright (c) Hitctrl. Licensed under CC BY 3.0. This mod's use of the work
  is not endorsed by the author.

* "Fantasy Choir" by cesisco (César da Rocha)
  Source:  https://opengameart.org/content/fantasy-choir-3-orchestral-pieces
  License: CC0 1.0 Universal (Public Domain Dedication)
           https://creativecommons.org/publicdomain/zero/1.0/
  Changes: converted to Ogg Vorbis; excerpted; level-normalised.
  No rights reserved. Credited here as a courtesy.

* "Breves dies hominis" — 12th–13th century composition,
  performed by Magdalen Kadel
  Source:  https://opengameart.org/content/breves-dies-hominis
           (original: https://commons.wikimedia.org/wiki/File:Breves_dies_hominis.ogg)
  License: CC0 1.0 Universal / Public Domain Dedication
           https://creativecommons.org/publicdomain/zero/1.0/
  Changes: converted to Ogg Vorbis; reverb tail extended for looping.
  No rights reserved. Credited here as a courtesy.

--------------------------------------------------------------------------------
### Phase 2 — "First Vicissitude" (aggressive)
--------------------------------------------------------------------------------

* "Dragon Dance" from "Chinese Game Music" by BiteMe Games
  Source:  https://bitemegames.itch.io/chinese-game-music
  License: CC0 1.0 Universal (Public Domain Dedication)
           https://creativecommons.org/publicdomain/zero/1.0/
  No rights reserved. Credited here as a courtesy.

* "Boss Battle Music" (Epic Boss Battle) by Juhani Junkala (SubspaceAudio)
  Source:  https://opengameart.org/content/boss-battle-music
  License: CC0 1.0 Universal (Public Domain Dedication)
           https://creativecommons.org/publicdomain/zero/1.0/
  Changes: converted from WAV to Ogg Vorbis; level-normalised to -16 LUFS.
  No rights reserved. Credited here as a courtesy.

* "Drums of Dawn" by Tomasz Kucza (Magnesus / magory.games)
  Source:  https://opengameart.org/content/drums-of-dawn
  License: Creative Commons Attribution 3.0 Unported (CC BY 3.0)
           https://creativecommons.org/licenses/by/3.0/
  Changes: converted from MP3 to Ogg Vorbis; level-normalised.
  Copyright (c) Tomasz Kucza. Licensed under CC BY 3.0. This mod's use of the
  work is not endorsed by the author.

* "Taiko drums (seamless loop)" by jobro
  Source:  https://opengameart.org/content/taiko-drums-seamless-loop
           (original: https://freesound.org/people/jobro/sounds/112248/)
  License: Creative Commons Attribution 3.0 Unported (CC BY 3.0)
           https://creativecommons.org/licenses/by/3.0/
  Changes: converted to Ogg Vorbis; level-normalised.
  Copyright (c) jobro. Licensed under CC BY 3.0. This mod's use of the work is
  not endorsed by the author.

* "Samurai Nights" by Majadroid
  Source:  https://opengameart.org/content/samurai-nights
  License: Creative Commons Attribution 4.0 International (CC BY 4.0)
           https://creativecommons.org/licenses/by/4.0/
  Changes: excerpted and re-arranged from the author's individual loop stems;
           converted to Ogg Vorbis; level-normalised to -16 LUFS.
  Copyright (c) Majadroid. Licensed under CC BY 4.0. This mod's use of the work
  is not endorsed by the author. Note: the disclaimer-of-warranties notice for
  this work is the one reproduced in the CC BY 4.0 legal code linked above.

* "Colossal Boss Battle Theme" by Matthew Pablo
  Source:  https://opengameart.org/content/colossal-boss-battle-theme
  License: Creative Commons Attribution 3.0 Unported (CC BY 3.0)
           https://creativecommons.org/licenses/by/3.0/
  Changes: used the author's included looped version; converted from WAV to
           Ogg Vorbis; level-normalised to -16 LUFS.
  Copyright (c) Matthew Pablo. Licensed under CC BY 3.0. This mod's use of the
  work is not endorsed by the author.

--------------------------------------------------------------------------------
### Public domain / CC0 notice

Tracks marked "CC0 1.0 Universal" above are dedicated to the public domain.
No attribution is legally required for them. They are credited here because it
is the right thing to do and because it helps players find the artists.
--------------------------------------------------------------------------------

## Other credits
[your artists, coders, translators, etc. — keep this section, and keep the music
 credits at the same prominence as everything here]
```

### What is actually mandatory, per licence

| Licence | Mandatory | Authority |
|---|---|---|
| **CC0 1.0** | **Nothing.** No attribution, no licence notice, no change notice, no URI. Commercial and monetised use permitted. Cannot be revoked. | ✅ <https://creativecommons.org/publicdomain/zero/1.0/> |
| **CC BY 3.0** | Author name (+ designated attribution parties); **the title**; the work's URI where reasonably practicable; a copy of / URI to the CC BY 3.0 licence **with every distributed copy**; copyright notices kept intact; if you adapted it, a note identifying the use. Must appear **with your other credits, at least as prominent**. | ✅ §4(a), §4(b) |
| **CC BY 4.0** | Creator + attribution parties; a copyright notice; a licence notice; a **warranty-disclaimer notice**; a URI/hyperlink to the material; an **indication that you modified it**; a statement that it is CC BY 4.0 plus the licence text or URI. May be satisfied "in any reasonable manner", including by linking to a resource that contains all of it. | ✅ §3(a)(1)–(3), §3(a)(2) |

### Three things people get wrong, that the template above fixes

1. **CC BY 4.0 wants you to say what you changed.** Even though §2(a)(4) says format conversion is a
   "technical modification" that does not create Adapted Material, §3(a)(1)(B) still requires you to
   *indicate modifications*. Hence "converted to Ogg Vorbis; trimmed for looping" on every 4.0 line.
2. **The credit must be as prominent as your other credits.** If your mod has a "Music by <friend>" line, the
   CC BY tracks need equal billing (§4(b) for 3.0; the general "reasonable manner" test for 4.0). A
   low-visibility footnote does not obviously comply.
3. **The licence URI has to travel with every copy.** A credits screen alone does not do this for CC BY 3.0 —
   hence `CREDITS.md` inside the jar, which also happens to be the version players can read on GitHub.

### Practical placement checklist

- [ ] `src/main/resources/CREDITS.md` — satisfies "licence URI with every copy".
- [ ] `CREDITS.md` (or `LICENSES.md`) at the repo root, linked from `README.md`.
- [ ] In-game **Credits screen** reachable from the mod list / config screen.
- [ ] Mod description on CurseForge/Modrinth links to the credits page.
- [ ] A `licenses/` folder in the repo holding a **saved copy of each source page** (PDF or `.txt` of the item
      page and its licence block) with the download date. **Do this for CC0 tracks too** — CC0 is only as good
      as the uploader's right to apply it, and CC's own deed says "Creative Commons has not verified the
      copyright status of any work to which CC0 has been applied."
- [ ] Record source URL, download date, and SHA-256 of every original file **before** you convert it. Ten
      minutes now, unanswerable evidence later.

---

## 5. What I could NOT verify — explicit list

| Item | Status | Why |
|---|---|---|
| Epidemic Sound licence terms | ❌ **UNVERIFIED** | `epidemicsound.com/music-licensing/` → 404; no Wayback copy of the T&Cs. **Nothing asserted.** |
| Artlist licence terms | ❌ **UNVERIFIED** | `artlist.io/license` → 403 (Cloudflare); Wayback snapshot is a JS-only shell; help-centre article → 403. **Nothing asserted.** |
| YouTube Audio Library — may tracks be bundled in a game? | ❌ **UNVERIFIED** | Google support pages are client-rendered; `web_fetch` returned only the `<title>`. **Nothing asserted.** |
| Exact US public-domain cutoff year for sound recordings | ✅ **NOW VERIFIED** — see §2.6.0 | 17 U.S.C. §1401 quoted; pre-1926 = PD, 1926 → 1 Jan 2027. Corroborated by the Cornell/Hirtle chart and Commons templates. |
| Musopen live 2026 terms | ❌ **UNVERIFIED** | `musopen.org` returns 403 (Cloudflare) on every path. All Musopen findings are from **Wayback snapshots** (ToS dated 18 Aug 2017; FAQ snapshot Feb 2024; browse page Jan 2025). Could not confirm whether "Musopen Plus" changes licensing, or whether per-track licence icons still match Jan 2025. |
| Commons `{{Licencereview}}` CC0 claims (item A11 — Skidmore/FMA *Pictures at an Exhibition*) | ⚠️ **REVIEW PENDING on Commons** | The CC0 claim has **not** been independently reviewed, and the sub-agent **did not open the individual FMA track pages** to confirm the CC0 marking at source. **This is the largest single gap in the chain of title.** If A11 matters, spend five minutes on freemusicarchive.org. |
| Provenance of the 2023 Holst Columbia transfer (A1–A3) | ❌ **UNVERIFIED** | The Commons Source field says only "Columbia Records" — unknown whether from original discs or a modern CD reissue. US law gives no new copyright to a faithful transfer, but a remastered reissue could in principle claim one. Low risk, not zero. |
| IMSLP current practice | ❌ **UNVERIFIED** | Its stated rules are stale ("1964 or earlier" Canada / "1923 or earlier" US). No live PR-licensed *audio* file page was found; no bytes were downloaded (bot check on normal paths). |
| Neighbouring rights outside the US/EU | ❌ **NOT AUDITED** | Japan, China, Korea, Brazil etc. This is the specific reason Tier A is safer than the §105 military-band items for worldwide distribution. |
| purple-planet.com licence text | ❌ **UNVERIFIED** | Every official URL returns an unrendered SPA shell. (Geoff Harvey's *Eastern Horizons* FMA tracks were verified as CC BY 4.0 on FMA itself — see §2.4 — but his own site's terms were not readable.) |
| A hard sample-rate requirement for Minecraft Ogg | ❌ **NOT FOUND** | No primary source states one. 44.1 kHz recommended as convention. |
| A hard bit-depth requirement for Minecraft Ogg | ❌ **NOT FOUND** | Community folklore says 16-bit; no primary source found, and the stb_vorbis→float decode path argues against it. |
| Forge/NeoForge `TickableSoundInstance` / `SoundEvent` registration signatures | ❌ **NOT VERIFIED** | Only the Fabric registration snippet was read from official docs. |
| Whether `stream: true` holds decoded PCM fully in memory | ❌ **NOT VERIFIED** | Relevant to the RAM budget note in §3.6. |
| `ffmpeg`/`ffprobe` commands actually executing | ❌ **NOT RUN** | ffmpeg is not installed on this machine. Option semantics are verified; the commands are not. |
| `loudnorm` filter syntax | ⚠️ Assumed standard | Not checked against the primary filter docs. |
| itch.io / FMA entries marked **[delegated]** | ⚠️ Verified once by a sub-agent | Each reports having fetched the item page and recorded the quoted licence text. I personally re-verified **BiteMe Games** and **Visager**. Re-check the rest at download time. |
| Audibility of any recommended track | ❌ **NOT AUDITIONED** | All Phase 1 / Phase 2 assignments are inferred from titles, tags and creator descriptions — **not from listening**. Treat the phase split as a starting hypothesis and audition before committing. |

---

## 6. Addendum — second research pass on PD/CC0 classical recordings

Extends §2.6. A follow-up Commons pass, run after the first Musopen survey pointed at Bach's *Toccata and
Fugue* and Satie's *Gymnopédies* as the strongest eerie repertoire, produced **two additions and four more
rejections**. The rejections are the more valuable half: one of them (R15) invalidates an obvious automation
strategy.

### 6.1 Two additions

**A17. Satie — Gymnopédie No. 1** ⭐ **CC0, and it actually loops**
Performer: the uploader's own piano rendition (User:Teknopazzo, "Own work"); composition published Paris 1888,
Satie d. 1925.
<https://commons.wikimedia.org/wiki/File:Gymnopedie_No._1..ogg>
Licence template verbatim: `{{cc-zero}}` — machine-readable `LicenseShortName` **"CC0"**, UsageTerms
"Creative Commons Zero, Public Domain Dedication", LicenseUrl cc0/1.0, **AttributionRequired: false**;
category CC-Zero. **Ogg Vorbis, 6.50 MB, 204.8 s (3:25).**
Satie's floating, harmonically rootless piano — the canonical "something is wrong and very old" cue. Because
it is a **piano miniature** rather than an orchestral movement, it loops far more forgivingly than anything
else in §2.6. **Slow ominous phase.**
⚠️ Mild caveat: the file description is garbled ("One of the most famous opera of Eric Satie") and the
uploader's user page is a redlink. The CC0 tag is unambiguous and it sits in Category:CC-Zero with **no**
licence-review banner — but provenance is thinner than A14/A15 (the La Pianista files, which are Wikimedia
Featured sounds). Archive the page.

**C2. Bach — Toccata and Fugue in D minor, BWV 565** ⭐ **the archetypal horror-organ piece**
Performer: **Norbert Schenk**, organ; recorded 2017 at the Evangelische Kirche Solms-Albshausen, Hessen.
<https://commons.wikimedia.org/wiki/File:Bach,_Toccata_und_Fuge_d-moll_BWV_565,_Norbert_Schenk.mp3>
Licence: `{{Cc-by-4.0}}` + `{{PermissionTicket|id=2020041810006148|user=Mussklprozz}}`.
The VRT-archived release statement, verbatim from the file page (signed "2020-03-10, Norbert Schenk"):

> "I hereby assert that I am the creator and/or sole owner of the exclusive copyright of File:Bach, Toccata
> und Fuge d-moll BWV 565, Norbert Schenk.mp3. I agree to publish that work under the free license Cc-by-4.0.
> **I acknowledge that I grant anyone the right to use the work in a commercial product, and to modify it
> according to their needs**, as long as they abide by the terms of the license and any other applicable
> laws."

Machine-readable: `LicenseShortName` "CC BY 4.0", **AttributionRequired: true**.
**MP3, 7.03 MB, 463.97 s (7:44)** — transcode to Ogg Vorbis (§3.3). **Slow ominous opening → fast driving
fugue**, so it covers both phases in one file. Requires a credit to Norbert Schenk (§1.3 / §4).
**This has a materially better chain of title than the Musopen "Public Domain Mark 1.0" version of the same
piece**, because a named rights holder signed an explicit commercial-use grant that Wikimedia volunteers
reviewed and archived.

### 6.2 Four more rejections

**R14. `File:Toccata et Fugue BWV565.ogg`** — 8:33 of solo organ Bach, attractive filename, and **exactly the
file a naive search grabs first**. Performer Ashtar Moïra, **recorded c. 2006**, tagged **`{{PD-old}}` only** —
a *composition* tag (Bach d. 1750) applied to a 2006 performance. **Mistagged.** The rendered licence box
itself warns: *"You must also include a United States public domain tag to indicate why this work is in the
public domain in the United States."* It sits in `Category:PD-old missing SDC copyright status`. A 2006
fixation cannot be PD.

**R15. `File:Satie - Gnossienne 1.ogg` and `File:Gnossienne 3 (Satie).ogg`** (La Pianista, 2010) — ⚠️ **the
single best demonstration in this report of why you must read the wikitext, not the machine-readable
fields.**
Wikitext verbatim: `===For the Original Composition=== {{pd-old}}` / `===For the Performance===
{{self|cc-by-sa-3.0|GFDL}}`.
But the API's aggregated `License` field for both reads **`"pd"`** and `LicenseShortName` reads **`"Public
domain"`** — because the *composition* tag dominates the aggregate — while the performance is actually
**CC BY-SA 3.0**.
→ **Anyone relying on a licence-scraping tool or the Commons API aggregate would ship these believing they
were public domain.** If you automate any part of the asset pipeline, **parse the wikitext or the per-file
`rel="license"` anchors, never the aggregate `License`/`LicenseShortName` fields.**

**R16. `File:Satie Gymnopedie No 1 performed by Michael Laucke.flac`** — 2001 guitar arrangement by the
classical guitarist Michael Laucke, VRT ticket 2016052310032124. Wikitext: `{{self|cc-by-sa-4.0}}` +
`{{PD-because|...}}`. **Performance *and arrangement* are CC BY-SA 4.0** — an arrangement carries its own
copyright — even though the aggregate again reads "Public domain". FLAC, 10.23 MB, 2:52.

**R17. `File:PDP-CH - Philadelphia Orchestra, Leopold Stokowski - Toccata and Fugue in D minor, BWV 565 … .flac`**
— Stokowski/Philadelphia, **recorded 6 April 1927, first release 1927**. Tags `{{Pdproject}} {{PD-old-70}}`.
**Rejected, and it confirms the pattern across the entire series.** 1927 ⇒ the 1926–1946 bracket ⇒ **US
protection until 1 January 2028.**
✅ **Category-wide finding (upgraded from §2.6.5):** four separate `PDP-CH` files have now been checked
(Mozart Requiem 1927, Gounod 1927, Bach/Stokowski 1927, plus the 1953 Fourestier *Danse macabre*), and **every
one relies solely on `{{Pdproject}}`, which per `Template:Pdproject/i18n` renders no licence at all — only
provenance.** → **Treat the whole `Category:Swiss Foundation Public Domain` as unusable without independent
per-item analysis.**

### 6.3 Musopen — final confirmations

- **ToS unchanged since 2017** — three snapshots (2023-06-05, 2024-05-01, 2025-01-17) are all headed
  "August 18, 2017" and are substantively identical.
- **Licence census across 12 Musopen pages / 172 licence anchors:** 107 × `publicdomain/mark/1.0`,
  19 × `by-nc-nd/3.0`, 18 × `by/3.0`, 13 × `by-sa/3.0`, 10 × `by-nc/3.0`, 5 × `by-nc-sa/3.0`, **0 × CC0**.
  → Roughly **one in eight Musopen recordings is NonCommercial and/or NoDerivatives.**
- ⚠️ **Musopen shows no human-readable licence string on a track — only CC badge icons.** The authoritative
  licence is the `href` of the `<a itemprop="license" rel="license" href="...">` anchor, whose only label is
  `aria-label="License Detail"`. **A human skimming the page will not see it. Read the anchor's href.**
- **There is no "dark" or "eerie" mood filter** — the mood facets are
  romantic/happy/sad/relaxing/energetic/fun/celebratory/tragic/passionate/dramatic.
- **Named NC traps on Musopen:** *every* recording on the *Danse macabre* Op. 40 page is NC — **there is no PD
  option for Danse macabre on Musopen.** *Trois Gnossiennes* (Carl Banner) is CC BY-NC-ND and is the only
  recording there. On the otherwise-usable *Moonlight Sonata* page, the complete-performance takes and Jürgen
  Noll are NC-ND, and Stefano Ligoratti is NC. Satie by "Prodigal Procrastinator" is CC BY-NC-SA. Fulda
  Symphonic Orchestra's Beethoven 5 is CC BY-SA.
- Recordings the sub-research verified as **PDM 1.0** — usable *if* the underlying PD claim holds, but with
  **no warranty** (§2.6.1): *Night on Bald Mountain*/Skidmore (12:13 — matches A10 exactly), *Toccata and
  Fugue*/Carl Weinrich (8:38), *Gymnopédie no. 1*/Edward Rosser (2:53), Beethoven 5 mvt I/European Archive
  (7:16), *Moonlight Sonata* mvt I/Paul Pitman (5:35), Chopin *Nocturne* Op. 9 no. 1/Olga Gurevich (5:37).

### 6.4 Revised globally-clean shipping set

- **Slow phase:** A1 Holst *Saturn* + **A17 Satie *Gymnopédie No. 1*** + A11 *Catacombae* / *Cum mortuis*
  (+ A7 Wagner *Siegfried's Funeral March* if you want length).
- **Fast phase:** A4 Holst *Mars* (USAF) + A6 Berlioz *Witches' Sabbath* + A11 *Baba Yaga*
  (+ **C2 Bach *Toccata and Fugue***, which also has a slow opening and so doubles as a transition).
- **Stingers/textures:** B5 gong, B7 taiko, B6 dizi, B1 Qing anthem (1914).

**Tier A alone** — A1, A2, A8, A9, A10, A11, A13, A14, A15, **A17**, plus the CC0 stingers — is a complete,
globally-clean two-phase boss soundtrack with **no attribution obligations and no territorial caveats**.
Take C2 (Bach) if you are willing to add one credit line; skip Tier B (§105 military-band items) if your
distribution footprint is worldwide.

### 6.5 Raw research files

The un-condensed research output behind §2.4, §2.6 and this addendum is archived at
`docs/design/first-vicissitude/source/research/` (`music-research-itchio-fma.md`, `musopen-research.md`,
`imslp-alt-research.md`, `bgm-licensing-research.md`). It contains the per-item URL lists and the full
rejection reasoning that this document summarises.

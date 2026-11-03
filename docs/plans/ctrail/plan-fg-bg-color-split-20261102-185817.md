# Plan: Split `<fgcolor>` / `<bgcolor>` and warn on unknown coloring (CTRAIL-23)

**Date:** 2026-11-02
**Status:** `Approved ("continue")` — 2026-11-02 · `[FINISHED]` — 2.0.0, see [modifications log](../../modifications/ctrail/modifications-fg-bg-color-split-20261102-193225.md)
**Base:** `main` @ `3bc1289` (1.5.0)
**Branch:** `CTRAIL-23/fg-bg-color-split`
**Target version:** **2.0.0** (major: background names removed from `<fgcolor>`)
**Jira:** [CTRAIL-23](https://verame.atlassian.net/browse/CTRAIL-23)
**Confluence:** _not published — publishing tooling failed twice on 2026-11-02 (scratch Python env missing `markdown`); retry after the build_

---

## Concise View

### Decisions

From the ticket (agreed 2026-11-02):

1. `<fgcolor>` = text color only; `<bgcolor>` (new) = background only. Background names leave
   `<fgcolor>` — **breaking**, 2.0.0.
2. A background name in `<fgcolor>` → one WARN naming the `<bgcolor>` replacement; value ignored,
   not auto-converted.
3. Unknown elements and unknown color values under `<coloring>` → one WARN each; startup continues.

The ticket's four open questions, taken as proposed ("looks good"), plus one I'm deciding:

| # | Question | Taken as |
|---|---|---|
| Q1 | `<flcolor>` text-only too? | **Yes** — same rule as `<fgcolor>` (no proposal was on the ticket; this follows your "only fgcolor and bgcolor") |
| Q2 | Filename prefix inherits the background? | **Yes**, unless `<flcolor>` is set |
| Q3 | `<defaultBgColor>`? | **In** — background for unmatched lines only |
| Q4 | Warning scope? | **`<coloring>` only** |
| new | `<defaultFgColor>` / `<noticeColor>` | **Text-only too**, same WARN — they are text colors |

[→ Detailed](#d-decisions)

### Approach

**Compose once at load, not per line.** A pair's text code and background code are concatenated
into the one color string the formatter already uses (`\033[1;37m` + `\033[41m`). So
`LineFormatter` needs almost no change, the filename prefix inherits the background for free (it
already falls back to the line color), and a config with no background produces exactly the codes
it does today.

[→ Detailed](#d-approach)

### Changes

| File | Change |
|---|---|
| `props/CtrailProps.java` | split `getColorCode` into `textColorCode` / `backgroundColorCode`; read `<bgcolor>` and `<defaultBgColor>`; count pairs by node, not by `fgcolor`; unknown-element / value WARNs |
| `unit/LineFormatter.java` | default color includes `<defaultBgColor>` (one line) |
| `etc/ctrail.xml` | fix `blue_underline` → `blue_underlined` (⚠️ see below); document `<bgcolor>` |
| README, `-h` footer, CHANGELOG, version files | document the split; ⚠️ breaking section with before/after; 1.5.0 → 2.0.0 |

[→ Detailed](#d-changes)

### Shipped-config typo — corrected during build

The plan said `etc/ctrail.xml:104` held a live `<flcolor>blue_underline</flcolor>`. **It doesn't**:
it sits inside an XML comment (an example block). It never affected output and does not WARN.
The spelling in the comment is fixed to `blue_underlined`; no behavior changes.

[→ Detailed](#d-typo)

### Tests and verification

~25 new JUnit 4 tests (props, formatter, warnings via a logback `ListAppender`), each red-checked.
Plus a byte-for-byte diff of 1.5.0 vs 2.0.0 output on the stock config: the only allowed
difference is the new WARN for the typo, before the typo fix.

[→ Detailed](#d-tests)

---

## Detailed View

<a id="d-decisions"></a>
### Decisions ([↑](#concise-view))

| Element | Accepts | Rejected with WARN |
|---|---|---|
| `<fgcolor>`, `<flcolor>`, `<defaultFgColor>`, `<noticeColor>` | regular, `_bold`, `_underlined`, `_bright`, `_bold_bright`, `orange` | `*_background`, `*_background_bright` (WARN names the `<bgcolor>` replacement); unknown names |
| `<bgcolor>`, `<defaultBgColor>` (new) | `black red green yellow blue purple cyan white`, each optionally `_bright` | anything else, e.g. `red_bold`, `red_background`, `chartreuse` |

Why reject `red_background` in `<bgcolor>` too: the ticket's first draft accepted it as a courtesy,
but with the split agreed, one spelling per slot is clearer and the WARN tells the user what to
write.

<a id="d-approach"></a>
### Approach ([↑](#concise-view))

Today `initColoring` (`CtrailProps.java:682`) stores one code per keyword in `_keysToColors`, and
`LineFormatter.format()` writes `logClr + line + RESET`, using `fileClr` (flcolor) or `logClr` for
the prefix, and `_defFgColor` when nothing matched.

New: for each pair, `lineColor = (text code ?: default text code if a bg is set) + (bg code ?: "")`.
Stored in `_keysToColors` as before. Consequences:

- pair with fg only → identical string to today;
- pair with bg only → default text color + background;
- prefix: `flcolor` if set, else `lineColor` → inherits the background (Q2);
- unmatched lines: `_defFgColor + defaultBg` (Q3) — one line in `LineFormatter`'s constructor;
- quick mode untouched (never reads the file; its colors are fixed text codes).

<a id="d-changes"></a>
### Changes ([↑](#concise-view))

#### `props/CtrailProps.java`

- `textColorCode(String name_, String where_)` — existing switch, but a name ending in
  `_BACKGROUND` / `_BACKGROUND_BRIGHT` returns null with WARN
  `<fgcolor>red_background</fgcolor> for keyword ERROR is not a text color; use <bgcolor>red</bgcolor>`;
  an unknown name returns null with WARN.
- `backgroundColorCode(String name_, String where_)` — maps `red` → `RED_BACKGROUND`,
  `red_bright` → `RED_BACKGROUND_BRIGHT` through the same switch; anything else WARN + null.
- `initColoring`: count pairs with `configurationsAt("coloring.linecolors.colorpair").size()`
  instead of counting `fgcolor` values (`:692`), so a bg-only pair is counted and later pairs keep
  their index; read `bgcolor`; compose.
- `warnAboutUnknownColoringElements(config_)`: walks `config_.getKeys("coloring")` against a
  whitelist (`linecolors.defaultFgColor`, `linecolors.defaultBgColor`, `linecolors.colorpair.{keyword,fgcolor,bgcolor,flcolor}`,
  `filename.blankLineOnFileChange`, `noticeColor`) and WARNs once per unknown key, naming the valid
  siblings. Naming the pair's keyword in that message depends on whether indexed `getKeys` works in
  commons-configuration2; if not, the message names the path only. Reported either way.
- `_defaultBgColor` field (`@Getter`), read from `<defaultBgColor>`.

#### `unit/LineFormatter.java`

`_defFgColor = defaultText + defaultBg`. Nothing else changes.

<a id="d-typo"></a>
### Shipped-config typo ([↑](#concise-view))

Found during the regression run: the 2.0.0 jar printed no WARN for it, and a test asserting one
failed. Cause: lines 100–105 of `etc/ctrail.xml` are a commented-out "full color pair example".
The comment's spelling is corrected; the regression diff therefore needed no second pass.

<a id="d-tests"></a>
### Tests ([↑](#concise-view))

JUnit 4, matching the suite. Each red-checked by reverting the line it covers. WARNs asserted
through a logback `ListAppender` on `CtrailProps`' logger — that also gives the suite a way to
test log output it has lacked so far.

| Class | Covers |
|---|---|
| `props/ColorSplitPropsTest` (new) | fg+bg composed in order; bg-only pair counted, next pair's index unchanged; `_bright` background; `red_background` in `<fgcolor>` rejected + one WARN naming `<bgcolor>red</bgcolor>`; `red_bold` in `<bgcolor>` rejected + WARN; unknown names WARN; `<flcolor>` / `<defaultFgColor>` / `<noticeColor>` reject backgrounds; `<defaultBgColor>` read |
| `props/UnknownColoringElementTest` (new) | `<bgcolour>` → exactly one WARN; unknown element under `<linecolors>` → WARN; known elements → no WARN; ctrail still loads every other setting |
| `unit/ColorSplitLineFormatterTest` (new) | white-on-red line bytes; prefix inherits bg; `<flcolor>` overrides prefix; `<defaultBgColor>` on unmatched lines only; no bg configured → identical to today's bytes |
| existing suites | green; `ColorUnitTestCase` still counts text colors |

Fixtures: `ctrail-color-split.xml`, `ctrail-color-unknown.xml`.

### Build / verify

1. Save the current 1.5.0 jar for comparison.
2. `mvn clean test` green; red-check each new test.
3. `mvn clean package`; on a sample log with the stock `etc/ctrail.xml` (typo not yet fixed),
   `diff` 1.5.0 vs 2.0.0 stdout → identical; stderr shows only the new `blue_underline` WARN.
4. Smoke: white on red, bg-only, rejected `red_background`, `<bgcolour>` typo.
5. Fix the shipped typo; re-run 3 → prefixes for those keywords now blue-underlined, nothing else.
6. ✅ / ❌ in the modifications log.

### Jira / Confluence

CTRAIL-23 → In Progress on "go"; Done when verified and committed. Plan published to CTrail.

### Open questions

None blocking. The typo fix (above) is a visible change — confirm or veto.

### Out of scope

- Validating the rest of `ctrail.xml` (Q4).
- The config-loading bug: one bad non-color value still aborts the rest of loading.
- Highlighting only the matched text, and quick-mode backgrounds.

## Definition of Done

> **Tests are not optional** — every logic-bearing method has a unit test, red-checked by
> reverting the line it covers; full suite green; byte-for-byte regression on the stock config;
> smoke-tested on the packaged jar; committed on `CTRAIL-23/fg-bg-color-split` with the version
> bump and CHANGELOG in the same commit; plan and changelog updated; the plan published to
> Confluence.

Deviation from the standard wording: JUnit 4 without Mockito, matching the existing suite. No
endpoints, so no RestAssured tests.

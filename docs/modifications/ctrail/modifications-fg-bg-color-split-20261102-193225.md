# Modifications: `<fgcolor>` / `<bgcolor>` split (CTRAIL-23)

**Date:** 2026-11-02
**Branch:** `CTRAIL-23/fg-bg-color-split` from `main` @ `3bc1289`
**Version:** 1.5.0 → **2.0.0** (major: background names removed from `<fgcolor>`)
**Plan:** [plan-fg-bg-color-split-20261102-185817.md](../../plans/ctrail/plan-fg-bg-color-split-20261102-185817.md)

---

## Concise View

- `<fgcolor>` = text, `<bgcolor>` = background, combinable; `<defaultBgColor>` for unmatched lines.
- ⚠️ Breaking: `*_background` names rejected in every text slot with a WARN naming the replacement.
- Unknown `<coloring>` elements and color names now WARN once each.
- Pairs counted by node: fixes a skipped bg-only pair and a spurious ERROR seen in test logs.
- 24 new tests, 15 mutations red-checked, suite 213 green; stock config byte-identical to 1.5.0. ✅
- ⚠️ Not done: plan not published to Confluence (tooling failed twice; see below).

[→ Detailed View](#detailed-view)

---

## Detailed View

### Design ([↑](#concise-view))

As planned: colors compose once at load (`textColorCode` + `backgroundColorCode` → `composeColor`),
so `LineFormatter` only gained the default background. Ticket questions taken as proposed:
`<flcolor>` text-only, prefix inherits the background, `<defaultBgColor>` in, warnings limited to
`<coloring>`. `<defaultFgColor>` and `<noticeColor>` are text-only too.

### Corrections during build ([↑](#concise-view))

- **Shipped-config typo was not live.** The plan said `etc/ctrail.xml:104`
  (`<flcolor>blue_underline</flcolor>`) would start warning. It is inside an XML comment, so it
  never mattered. Found when a test asserting the WARN failed. Comment spelling fixed; no behavior
  change.
- **Duplicate warning.** The old `getColorCode` logged its own WARN for unknown names, so
  `chartreuse` warned twice. Lowered to DEBUG; test now asserts exactly one.
- **Double space** in messages with no keyword context (`</noticeColor>  is not`). Fixed, tested.
- **`-h` footer edit** briefly broke compilation (a regex turned `\n` into a real newline); fixed.

### Pair counting ([↑](#concise-view))

`initColoring` counted `fgcolor` values. A pair without one was skipped, and
`ctrail-first-word-match.xml` (pair `(SEV)` has two `<fgcolor>`) made the loop read `colorpair(7)`,
logging `Key 'coloring.linecolors.colorpair(7).fgcolor' does not map to an existing object` on
every load. Now `configurationsAt("coloring.linecolors.colorpair").size()`. The loop body moved to
`initColorPair` / `registerColorKeyword`, which also removed the duplicated case-sensitive and
case-insensitive branches.

### Verification ([↑](#concise-view))

- 213 tests green, random class order. 15 mutations, all red.
- Byte-for-byte: stock config, same 7-keyword log tailed twice, 1.5.0 baseline jar vs 2.0.0 —
  stdout identical, stderr identical.
- Warnings now testable: new `testutil/LogCapture` wraps a logback `ListAppender`.

### Not done ([↑](#concise-view))

- Plan **not published to Confluence**: the scratch Python env lacked `markdown` and two attempts
  failed (2-attempt limit). The plan's `Confluence:` line says so.
- Duplicate `<fgcolor>` in one pair silently takes the first; not warned.
- Rest of `ctrail.xml` is still unvalidated, and the config-loading bug (one bad non-color value
  aborts loading) remains.
- Tests are JUnit 4 without Mockito, matching the suite.

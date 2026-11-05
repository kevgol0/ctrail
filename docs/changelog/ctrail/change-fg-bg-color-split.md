# Change: `<fgcolor>` / `<bgcolor>` split (CTRAIL-23) — 2.0.0

## Summary

`<fgcolor>` is text-only; new `<bgcolor>` is background-only and combines with it (white on red).
New `<defaultBgColor>`. Background names are rejected from every text slot with a WARN naming the
replacement (breaking). Unknown `<coloring>` elements and color names WARN instead of being
silently ignored. Color pairs are counted by node, fixing a skipped bg-only pair and a spurious
ERROR on pairs with two `<fgcolor>`.

## Code

```java
// CtrailProps.initColorPair(): text and background resolve separately, then compose once
final String lineColor = composeColor(textColorCode(fgName, "fgcolor", context),
        backgroundColorCode(bgName, "bgcolor", context));

// composeColor(): no background -> the text code exactly as before
if (StringUtils.isEmpty(background_)) return text_;
return (StringUtils.isEmpty(text_) ? _defaultFgColor : text_) + background_;
```

```java
// LineFormatter: unmatched lines take <defaultBgColor>, empty unless configured
_defFgColor = defaultText + StringUtils.defaultString(props.getDefaultBgColor());
```

## Tests

| Class | Tests | Covers |
|---|---|---|
| `props/ColorSplitPropsTest` | 13 | compose order; bg-only keeps default text; later pair not shifted; `_bright`; background name rejected from `<fgcolor>`, `<flcolor>`, `<noticeColor>` with replacement; text style rejected from `<bgcolor>`; unknown name warns exactly once; `<defaultBgColor>` read / absent; case and spaces |
| `props/UnknownColoringElementTest` | 5 | `<bgcolour>` warns once naming the keyword; unknown element outside a pair; no other warnings; rest of config still loads; shipped config has no warnings |
| `unit/ColorSplitLineFormatterTest` | 6 | white-on-red bytes; prefix inherits background; `<flcolor>` overrides prefix only; default background on unmatched lines only; unchanged output without backgrounds |
| `testutil/LogCapture` | — | new helper: asserts on WARNs via a logback `ListAppender` |

- Full suite: **213 passed, 0 failed** (189 before).
- 15 mutations of the new code, each turned at least one test red.
- Regression: stock `etc/ctrail.xml`, 1.5.0 vs 2.0.0 jar on the same log — stdout **byte-identical**
  (20 lines), stderr identical.
- Smoke: white on red, bg-only, rejected `red_background` and `red_bold`, `<bgcolour>` and
  `<sparkle>` warnings, and the original report (`<bgcolor>RED</bgcolor>`) now renders red.

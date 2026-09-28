# Modifications: phase 6 — explicit charset (CTRAIL-14)

**Date:** 2026-09-28
**Branch:** `CTRAIL-14/charset` from `main` @ `649bcd4`
**Version:** 1.2.3 → **1.3.0** (minor: new config key)
**Plan:** [plan-fix-code-review-bugs-20260927-235255.md](../../plans/ctrail/plan-fix-code-review-bugs-20260927-235255.md) — phase 6

---

## Concise View

Both decode points now use one configurable charset, defaulting to UTF-8. 118 tests.

This was flagged in the plan as the **highest-regression-risk change** in the list, because it sits
under the read loop, the tail-N seek arithmetic and every filter comparison. The 9 pre-existing seek
tests and the 6 lifecycle tests pass unchanged, which is the evidence that the seek side was not
disturbed.

[→ Detailed View](#detailed-view)

---

## Detailed View

### Two defects, one cause

| Path | Was | Now |
|---|---|---|
| files | `RandomAccessFile.readLine()` — Latin-1 by specification, not configurable | `FileTailTracker.readLine(Charset)` |
| stdin | `new InputStreamReader(stream)` — platform default | explicit charset |

Because the two disagreed, `cat f \| ctr` and `ctr f` could render identical bytes differently.

### Demonstrated, not asserted

Run against the same UTF-8 file, using the new key to reproduce the old behaviour:

```
charset=ISO-8859-1   cafÃ© lattÃ©     ã­ã° start
charset=UTF-8        café latté      ログ start
```

Stdin agrees: `printf 'café via pipe\n' | ctr` → `stdin:café via pipe`.

### The part that mattered more than appearance

A `<includes>` or `<excludes>` keyword containing a non-ASCII character was compared against
mis-decoded text and **silently never matched**, so filtering looked like it did nothing. There is a
test for exactly that.

### Terminator parity

The replacement keeps `readLine()`'s contract — `\r`, `\n`, `\r\n`, and a final line with no
terminator — and leaves the file pointer after the terminator so the seek arithmetic and the
rotation check are untouched. `terminatorsBehaveAsReadLineDid` pins all four cases.

### Performance note

`RandomAccessFile` is unbuffered, so the new reader does one `read()` per byte — exactly what
`readLine()` already did. No regression, but no improvement either; a buffered reader over the
tracker would be a separate change.

### ⚠️ Behaviour change on upgrade

Input decodes as UTF-8 rather than Latin-1. ASCII logs are unaffected. `<charset>ISO-8859-1</charset>`
restores the old behaviour, and that escape hatch is documented in the README and `etc/ctrail.xml`.

### Verification

- Suite: **118 passing**; the seek and lifecycle tests unchanged.
- New `CharsetHandlingTest` (6 tests): UTF-8 round-trip, non-ASCII keyword matching, terminator
  parity, configurability, the UTF-8 default, and an unusable name falling back with a warning.

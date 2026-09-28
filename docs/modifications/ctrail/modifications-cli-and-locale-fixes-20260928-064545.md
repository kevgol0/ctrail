# Modifications: phase 1 — CLI and formatting fixes

**Date:** 2026-09-28
**Branch:** `fix/cli-and-config` from `main` @ `653f6cf`
**Jira:** [CTRAIL-6](https://verame.atlassian.net/browse/CTRAIL-6), [CTRAIL-7](https://verame.atlassian.net/browse/CTRAIL-7), [CTRAIL-12](https://verame.atlassian.net/browse/CTRAIL-12)
**Plan:** [plan-fix-code-review-bugs-20260927-235255.md](../../plans/ctrail/plan-fix-code-review-bugs-20260927-235255.md) — phase 1
**Changelog:** [change-cli-and-locale-fixes.md](../../changelog/ctrail/change-cli-and-locale-fixes.md)

---

## Concise View

Three independent fixes, 101 tests green, each verified red-then-green.

* **CTRAIL-7** — `Integer.parseInt` in a try/catch replaces the `StringUtils.isNumeric` guard; negatives rejected
* **CTRAIL-6** — `-e` applied after `-n` so it wins, as documented
* **CTRAIL-12** — `Locale.ROOT` on all three `String.format` calls in `DurationFormatter`

[→ Detailed View](#detailed-view)

---

## Detailed View

### Files changed

| File | Change |
|---|---|
| `CtrailEntryPoint.java` | `setTailLastLinesFromArg` parses inside try/catch and rejects negatives; `-e` block moved after `-n`; method made `protected` for reach |
| `unit/DurationFormatter.java` | `Locale.ROOT` on every `String.format`; Javadoc states the ASCII guarantee |
| `CtrailCliOptionsTest.java` | new — 4 tests |
| `unit/DurationFormatterTest.java` | new locale test; `@After` restores the default locale |

### Testing through the constructor, not the helper

`setTailLastLinesFromArg` was private. Rather than assert on it directly, `CtrailCliOptionsTest`
builds a real `CtrailEntryPoint` with the actual argument array, so the tests exercise the path a
user hits — including the `catch (ParseException)` that let the original exception escape. The
method is `protected` rather than private only to keep it reachable for a future direct test; the
current tests do not call it.

### Red-check

Each fix reverted on its own:

| Reverted | Result |
|---|---|
| CTRAIL-7 parse guard | RED — `oversizedLineCountIsIgnoredRatherThanFatal:81` |
| CTRAIL-6 `-e` placement | RED — `entireFileBeatsLineCountRegardlessOfArgumentOrder:125` |
| CTRAIL-12 `Locale.ROOT` | RED — `digitsAreAsciiRegardlessOfDefaultLocale:53` |

No cross-talk: each mutation failed only its own test.

### Smoke test, packaged jar

```
-e -n 50         -> 200 lines
-n 50 -e         -> 200 lines
-e               -> 200 lines
-n 50            ->  50 lines
-n 99999999999   -> 114 lines, no exception   (falls back to skipAheadInBytes)
```

Devanagari-default JVM: `ctrail: watching e.log - 1 KB, modified 15s ago` — ASCII digits.

### Note

`CTRAIL-8` was originally scoped into this phase. It became a config redesign (single `<tailLast>`
with `<count>`/`<unit>`, old keys as deprecated aliases) and moved to its own phase 6b.

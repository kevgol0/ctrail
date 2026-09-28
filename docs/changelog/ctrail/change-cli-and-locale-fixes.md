# Fix: three CLI and formatting defects (CTRAIL-6, -7, -12)

**Changed:** 2026-09-28 — `fix/cli-and-config`, phase 1 of the code-review fix plan

## Summary

| Ticket | Defect | Now |
|---|---|---|
| CTRAIL-7 | `ctr -n 99999999999` crashed with an uncaught `NumberFormatException` | warns and keeps the configured value |
| CTRAIL-6 | `-n` silently overrode `-e`, contradicting the README | `-e` wins, in either argument order |
| CTRAIL-12 | elapsed times rendered in locale-specific digits | always ASCII |

## CTRAIL-7 — parse is the guard

`StringUtils.isNumeric` tests **digit-ness**; `Integer.parseInt` tests **range**. An all-digit value
above `Integer.MAX_VALUE` passed the first and threw on the second, escaping the
`catch (ParseException)` and killing the run — the opposite of what the method's own Javadoc
promised.

The parse is now the guard, and negative values are rejected too: a negative count would be read as
"disabled" by the tail-N branch, which is not what the user asked for.

```
$ ctr -n 99999999999 app.log     # before: stack trace, exit 1
$ ctr -n 99999999999 app.log     # after:  WARN, falls back to the configured value
```

## CTRAIL-6 — order of application, not order of arguments

`-e` was applied before `-n`, so `-n` overwrote the `setTailLastLines(0)` that `-e` had just
performed. Command-line order made no difference, because the code tested `e` before `n`
regardless.

`-e` is now applied **after** `-n`, so it wins as documented:

| Command | Before | After |
|---|---|---|
| `ctr -e -n 50 file` | 50 lines | **200 lines** (whole file) |
| `ctr -n 50 -e file` | 50 lines | **200 lines** |
| `ctr -n 50 file` | 50 lines | 50 lines |

## CTRAIL-12 — ASCII digits, always

`DurationFormatter` called `String.format` with no `Locale`, inheriting `Locale.getDefault(FORMAT)`:

| Locale | Before | After |
|---|---|---|
| `en_US` | `4m 12s` | `4m 12s` |
| `hi-IN-u-nu-deva` | `४m १२s` | `4m 12s` |
| `ar-EG` | `٤m ١٢s` | `4m 12s` |
| `th-TH-u-nu-thai` | `๔m ๑๒s` | `4m 12s` |

This also un-breaks `DurationFormatterTest`, which asserts the ASCII forms and would have failed
outright on such a machine. It brings the class in line with `LineFormatter`, which already used
`Locale.ROOT` deliberately.

## Test results

```
Tests run: 101, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

New: `CtrailCliOptionsTest` (4 tests, driving the real constructor rather than the private parse
helper) and `DurationFormatterTest.digitsAreAsciiRegardlessOfDefaultLocale`.

**Red-check** — each fix reverted independently fails its own test and nothing else:

| Reverted | Fails |
|---|---|
| CTRAIL-7 parse guard | `oversizedLineCountIsIgnoredRatherThanFatal` |
| CTRAIL-6 `-e` placement | `entireFileBeatsLineCountRegardlessOfArgumentOrder` |
| CTRAIL-12 `Locale.ROOT` | `digitsAreAsciiRegardlessOfDefaultLocale` |

**Smoke test**, packaged jar: `-e -n 50` and `-n 50 -e` both emit the whole 200-line file;
`-n 99999999999` emits no exception and falls back to the configured behaviour; a Devanagari-default
JVM prints `modified 15s ago` in ASCII.

## Not changed

The README already documented the correct `-e` behaviour — the code was wrong, not the docs.

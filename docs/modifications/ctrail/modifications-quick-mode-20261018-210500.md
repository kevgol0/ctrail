# Modifications: quick mode, `ctr -q REGEX` (CTRAIL-22)

**Date:** 2026-10-18
**Branch:** `CTRAIL-22/quick-mode` from `main` @ `25ec68f`
**Version:** 1.4.2 → **1.5.0** (minor: new option)
**Plan:** [plan-quick-mode-20261018-184213.md](../../plans/ctrail/plan-quick-mode-20261018-184213.md)
**Confluence:** [Plan: Quick mode — ctr -q PATTERN (CTRAIL-22)](https://verame.atlassian.net/wiki/spaces/CTrail/pages/102662145)

---

## Concise View

- `-q REGEX`, repeatable: every line shown; a matching line is coloured whole; case-sensitive;
  one colour per pattern, first wins. `ctrail.xml` and every other option ignored.
- Built as a replacement `CtrailProps` that never reads the file, so config isolation needs no
  per-setting overrides.
- Any parse error is now a usage error (stderr, exit 2).
- 28 new tests, 13 mutations red-checked; suite 189 green; smoke-tested on the jar. ✅
- Plan's open caveat resolved: the config file is **not** read in quick mode.

[→ Detailed View](#detailed-view)

---

## Detailed View

### Design ([↑](#concise-view))

As planned. `QuickPattern` (new) holds the source text, the compiled `Pattern` and its colour.
`CtrailProps.installQuickDefaults()` builds a defaults-only instance through a new private
constructor and pins it as the singleton. `LineFormatter` colours by the first matching pattern
when quick patterns are present and skips the keyword loop.

### Config-loading order — the plan's caveat ([↑](#concise-view))

The plan expected `ctrail.xml` to be read and discarded in quick mode, its WARNs still printing.
Resolved better than planned: the constructor's early `loadProps()` was redundant (every override
calls `getInstance()` anyway), so it was removed and arguments are parsed first. In quick mode
nothing calls `getInstance()` before `installQuickDefaults()`. Smoke-verified: a config with a
deprecated key prints its WARN in normal mode and not in quick mode.

### Argument parsing restructured ([↑](#concise-view))

`loadArgsAndOverrides` was one ~140-line method. Now: `buildOptions`, `parseArgs`,
`handleInfoOptions` (-h/--version), `applyStandardOverrides` (unchanged logic, moved), and the new
`applyQuickMode`, `compileQuickPatterns`, `warnAboutIgnoredOptions`, `describeQuickMode`.
`-h`/`--version` now run before the overrides rather than after; both exit, so behaviour is the same.

### Usage errors ([↑](#concise-view))

`CtrailUsageException` (new); `main()` prints `ctrail: <message>` to stderr and exits 2.
Messages seen in the smoke test:

```
ctrail: -q [ is not a valid regex: Unclosed character class
ctrail: Missing argument for option: q (see ctr -h)
ctrail: Unrecognized option: --bogus (see ctr -h)
```

### Not covered by unit tests ([↑](#concise-view))

Verified by smoke test only — they are log or stdout output with no capture harness in the suite:

- the WARN listing ignored options (`quick mode ignores -n -e`)
- the quick-mode banner being emitted (its text is unit-tested)
- the config file not being read

### Other ([↑](#concise-view))

- Fixture mistake caught by its own guard test: `<filefilter><filename>` is a glob-style name
  (`quick*.log`), not a regex.
- `_matchpattern` gained a Lombok `@Getter` so the CLI test can prove `-m` is ignored.
- `bin/install.sh` usage example was still on 1.4.0 (missed in 1.4.1/1.4.2); now 1.5.0.
- Tests are JUnit 4 without Mockito, matching the suite.

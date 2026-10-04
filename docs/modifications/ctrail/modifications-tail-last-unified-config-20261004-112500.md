# Modifications: phase 6b — unified `<tailLast>` (CTRAIL-8)

**Date:** 2026-10-04
**Branch:** `CTRAIL-8/tail-last-unified-config`, stacked on `CTRAIL-1/shutdown-and-stdin-fallback` (PR #22)
**Version:** 1.3.1 → **1.4.0** (minor: new config key and CLI flag)
**Plan:** [plan-fix-code-review-bugs-20260927-235255.md](../../plans/ctrail/plan-fix-code-review-bugs-20260927-235255.md) — phase 6b

---

## Concise View

- `<tailLast><count>N|0|all</count><unit>lines|bytes</unit></tailLast>` replaces `tailLastLines`
  and `skipAheadInBytes`; both remain as deprecated aliases with their legacy meaning.
- New `-c/--bytes N`. `-n` beats `-c`; `-e` beats both.
- ⚠️ `-n 0` now means "no history", not "last 1000 bytes".
- 22 new/changed tests, 9 mutations red-checked. Suite: 147 green. Smoke-tested on the jar.
- Stacked on PR #22 — merge that first.

[→ Detailed View](#detailed-view)

---

## Detailed View

### Design decisions ([↑](#concise-view))

The ticket's nested-element design was used, not the plan's original unit-suffixed string. Taken
with Kevin on 2026-10-04:

| Question | Decision |
|---|---|
| `count=0` | start at the end (`tail -n 0`) |
| deprecated `tailLastLines=0` | means "unset": defers to `skipAheadInBytes`, as the 1.2 README promised |
| `-c/--bytes` | added |

Follow-on decided during implementation: with `0` = start at end, `skipAheadInBytes=0` (whole file)
needed a replacement, so `<count>` also accepts **`all`**. Not in the ticket.

### Parsing ([↑](#concise-view))

`initTailLast` detects the new key with `config.subset("execution.tailLast")`, the aliases with
`containsKey`. All values are read as strings and parsed locally: `config.getInt` on a bad value
throws out of the constructor's single try/catch and silently skips every setting after it — a
latent defect for every other `getInt` in that constructor, not fixed here.

### Tracker positioning ([↑](#concise-view))

`FileTailTracker` constructor delegates to `positionForTailLast()`; the old two-branch logic and its
false comment ("an existing config with tailLastLines=0 is unchanged") are gone.

### Test notes ([↑](#concise-view))

- `CtrailProps` has no reset hook, so `TailLastPropsTest` builds a fresh `new CtrailProps()` per
  test and the CLI test pins `setTailLast(3, LINES)` in `@Before`.
- `lineCountWinsWhenBothCountsAreGiven` does not distinguish "skip -c" from "apply -c then -n"; both
  give the same result. Only the WARN differs.
- JUnit 4, matching the suite (see the 1.3.1 log).

### Not done ([↑](#concise-view))

- The other `config.getInt` calls in `CtrailProps` still abort loading on a bad value.
- PR not merged; Jira CTRAIL-8 left In Progress until it is.

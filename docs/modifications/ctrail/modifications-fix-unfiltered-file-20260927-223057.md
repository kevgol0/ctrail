# Modifications: unfiltered files showed nothing

**Date:** 2026-09-27
**Branch:** `fix/unfiltered-file-shows-nothing` from `main` @ `db3d661`
**Plan:** [plan-fix-unfiltered-file-shows-nothing-20260927-222710.md](../../plans/ctrail/plan-fix-unfiltered-file-shows-nothing-20260927-222710.md)
**Changelog:** [change-unfiltered-file-shows-nothing.md](../../changelog/ctrail/change-unfiltered-file-shows-nothing.md)

---

## Concise View

One statement in `FileTailTracker.shouldExcludeLineDueToSeachTerms`: a source with no filter
attached now excludes nothing, instead of inheriting `fileFilterDefaultsToInclude`. With the
shipped config that flag is `false`, which had been hiding every line of any file matching no
`<filefilter>`.

96 tests (94 before). Red-check fails exactly the one regression test.

[→ Detailed View](#detailed-view)

---

## Detailed View

### Files changed

| File | Change |
|---|---|
| `files/FileTailTracker.java` | `return false` when no filter; dead `_defLineExclude` field and its constructor wiring removed |
| `files/FileReaderThreadFilterTest.java` | two tests added; `setUp` pins `fileFilterDefaultsToInclude` |
| `docs/plans/…`, `docs/changelog/…` | new |

### Discovered during verification

The first red-check run failed **two** tests, not one. The second was
`testMatchIsCaseInsensitiveByConfig` — collateral from my own new tests flipping
`fileFilterDefaultsToInclude` on the `CtrailProps` singleton with no restore, which leaked into
whichever test ran next. The fix masked it (the flag stops mattering for unfiltered sources), so it
would have sat there as order-dependence waiting for the next test that cared. `setUp` now pins the
flag explicitly, and the repeated red-check then failed exactly one test.

### Verification

- Suite: 96 passing.
- Red-check: restoring `return !isFileFilterDefaultsToInclude()` fails only
  `unmatchedFileEmitsEveryLineEvenWhenDefaultsToExclude` (`expected:<3> but was:<0>`).
- Smoke, shipped `etc/ctrail.xml`, unmatched filename: banner + last 10 lines (was banner only).
- Smoke, shipped config, `cityspark-x.log.0` which matches a filter: only the two `geo-lookup`
  lines — strict allow-list behaviour intact.

### Not touched

- CVE-2026-45205 / commons-configuration2 2.15.0 — still open, still undiagnosed.
- `FileSearchFilter`'s within-a-filter semantics, which were correct.

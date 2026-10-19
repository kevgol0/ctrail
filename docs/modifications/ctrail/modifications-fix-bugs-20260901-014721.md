# Modifications: Fix 12 Bugs in ctrail

**Date:** 2026-09-01  
**Commit:** `decb091`

## Changes

| # | File | Fix |
|---|------|-----|
| 1 | `CtrailEntryPoint.java` | `-v` now sets `enabledExcludeFiltering` instead of overwriting `-f` |
| 2 | `StdinReaderThread.java` | Added `runPassthrough()` for stdin with no match/filter |
| 3 | `CtrailProps.java` | `contains()` → `containsKey()` for filter dedup |
| 4 | `CtrailProps.java` | Added `color_` arg to `getColorCode` warning log |
| 5 | `CtrailProps.java` | Fixed 2-placeholder/1-arg error log in `initFiltering` |
| 6 | `LineFormatter.java` | Converted `_tmp*` instance fields to local variables |
| 7 | `LineFormatter.java` | Removed per-line `refreshProps()`, init once in constructor |
| 8 | `CtrailProps.java` | Removed redundant `new String()` in `calcVersion` |
| 9 | `CtrailProps.java` | Removed duplicate `]` append in case-sensitive debug trace |
| 10 | `OutputWriterThread.java` | `pollLast` → `pollFirst` in shutdown drain loop |
| 11 | `StdinReaderThread.java` | `runWithMatch` now respects `useCaseSensitiveSarch` |
| 12 | `FileTailTracker.java` | `shouldExcludeLineDueToSeachTerms` returns `_defLineExclude` when no filter set |

### New property
- `CtrailProps._enabledExcludeFiltering` — controls exclude filtering independently from include filtering
- `FileSearchFilter.shouldExcludeLineDueToSeachTerms` checks this before evaluating exclude terms

## Verification
- `mvn test` — all tests pass

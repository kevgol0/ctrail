# Fix: a file with no matching `<filefilter>` showed nothing

**Fixed:** 2026-09-27 — `fix/unfiltered-file-shows-nothing`

## Summary

With the shipped `etc/ctrail.xml`, tailing any file whose name did not match a `<filefilter>`
printed the startup banner and then **nothing**:

```
$ ctr myapp.log          # 30 lines in the file
ctrail: watching myapp.log - 231 bytes, modified 0s ago
                                                          <- no lines, ever
```

Pointing ctrail at a log file is the tool's primary use, so on a default install it produced an
empty screen unless the user happened to add a matching filter.

## Root cause

`fileFilterDefaultsToInclude` was applied in **two** places. One was right, one was not.

| Where | Applies to | Correct? |
|---|---|---|
| `FileSearchFilter._defLineInclude` | a line matching neither list, on a source that **has** a filter | yes |
| `FileTailTracker._defLineExclude` | every line on a source with **no** filter | **the bug** |

`FileTailTracker` set `_defLineExclude = !isFileFilterDefaultsToInclude()`. The shipped config sets
that flag `false`, so `_defLineExclude` became `true`, and `shouldExcludeLineDueToSeachTerms`
returned `true` for every line of an unfiltered file.

The config's own comment already scoped the setting correctly:

> verdict for a line that matched NEITHER list, **on a source that does have a filter**

So this was the implementation contradicting its documented contract, not a design ambiguity.

## The change

One statement in `FileTailTracker.shouldExcludeLineDueToSeachTerms`:

```java
//
// no filter on this source, so there is nothing to exclude against.
// fileFilterDefaultsToInclude is the verdict for a line that matched
// neither list WITHIN a filter, and FileSearchFilter already applies it.
// Applying it a second time here hid every line of any file that matched
// no <filefilter> - which, with the shipped config, is most files
//
return false;
```

`_defLineExclude` had no other reader and was removed along with its Lombok accessors.

## Behaviour

Only one case changes.

| `fileFilterDefaultsToInclude` | File has a matching filter | Before | After |
|---|---|---|---|
| `true` | no | all lines | all lines |
| `true` | yes | filter decides | unchanged |
| **`false`** | **no** | **nothing** | **all lines** |
| `false` | yes | strict allow-list | unchanged |

stdin was already correct — `StdinReaderThread.shouldEmit` returns `true` when no filter is set,
without consulting the default. This brings files in line with stdin.

## Why the tests missed it

`FileReaderThreadFilterTest.testNoFilterEmitsEveryLine` covers this exact path, but its fixture
sets `fileFilterDefaultsToInclude=**true**`. The shipped config ships it **false**. The one
combination that broke was the one nothing exercised.

## Test results

```
Tests run: 96, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Two tests added:

| Test | Covers |
|---|---|
| `unmatchedFileEmitsEveryLineEvenWhenDefaultsToExclude` | the regression — every line emitted with no matching filter and the default set to exclude |
| `matchedFileStillHonorsStrictAllowList` | a file that does match keeps strict allow-list behaviour, proving the fix does not weaken filtering |

**Red-check:** restoring the old behaviour fails exactly one test — the regression test
(`expected:<3> but was:<0>`) — and nothing else.

The first red-check run also surfaced **test pollution introduced by the new tests**: they flip
`fileFilterDefaultsToInclude` on the `CtrailProps` singleton, which leaked into
`testMatchIsCaseInsensitiveByConfig`. `setUp` now pins the flag so each test starts from a known
state.

**Smoke test** with the shipped `etc/ctrail.xml`:

- unmatched filename → banner plus the last 10 lines (was: banner only)
- `cityspark-x.log.0`, which matches a shipped filter → only the two `geo-lookup` lines, strict
  filtering intact

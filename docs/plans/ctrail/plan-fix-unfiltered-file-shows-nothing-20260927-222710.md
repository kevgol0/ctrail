# Plan: Fix — a file with no matching `<filefilter>` shows nothing

**Date:** 2026-09-27
**Status:** `Approved ("fix the latent bug on its own branch")` — 2026-09-27
**Branch:** `fix/unfiltered-file-shows-nothing` from `main` @ `db3d661`
**Jira / Confluence:** _n/a — personal tool repo_

---

## Concise View

### The bug

With the **shipped** `etc/ctrail.xml`, tailing any file whose name does not match a
`<filefilter>` prints the startup banner and **nothing else**:

```
$ ctr myapp.log          # 30 lines in the file
ctrail: watching myapp.log - 231 bytes, modified 0s ago
                                                          <- no lines, ever
```

Installing ctrail and pointing it at a log is the tool's primary use. Today that produces an empty
screen unless the user happens to add a matching filter.

### Root cause

`fileFilterDefaultsToInclude` is applied in **two** places. One is correct, the other is not.

| Where | Applies to | Correct? |
|---|---|---|
| `FileSearchFilter._defLineInclude` | a line matching neither list, **on a source that has a filter** | ✅ matches the documented intent |
| `FileTailTracker._defLineExclude` | every line on a source with **no** filter at all | ❌ the bug |

[FileTailTracker.java:73](../../../src/main/java/com/kagr/tools/ctrail/files/FileTailTracker.java#L73) sets `_defLineExclude = !isFileFilterDefaultsToInclude()`. The shipped config
sets `fileFilterDefaultsToInclude=false`, so `_defLineExclude` becomes `true`, and
`shouldExcludeLineDueToSeachTerms` returns `true` for every line on an unfiltered file.

The config's own comment already states the intended scope:

> verdict for a line that matched NEITHER list, **on a source that does have a filter**

So this is the implementation contradicting its own documented contract, not an ambiguous design
call.

### The fix

One statement. When no filter is attached, nothing is excluded:

```java
// no filter on this source: there is nothing to exclude against. The
// fileFilterDefaultsToInclude setting is the verdict WITHIN a filter, and
// FileSearchFilter already applies it; applying it here too hid every line
// of any file that matched no <filefilter>
return false;
```

`_defLineExclude` and its Lombok accessors then have no remaining reader and are removed.

### Why the tests did not catch it

`FileReaderThreadFilterTest.testNoFilterEmitsEveryLine` covers exactly this path — but its fixture
(`ctrail-file-search-filter.xml`) sets `fileFilterDefaultsToInclude=**true**`. The shipped config
ships it **false**. The one combination that breaks is the one nothing exercised.

### Scope

| # | Change | File |
|---|---|---|
| 1 | Return `false` when no filter is attached | `FileTailTracker.java` |
| 2 | Remove the now-dead `_defLineExclude` field and its wiring | `FileTailTracker.java` |
| 3 | Fixture with `fileFilterDefaultsToInclude=false` | `configs/ctrail-file-filter-strict.xml` (new) |
| 4 | Tests for the broken combination | `FileReaderThreadFilterTest` (extend) |
| 5 | Changelog + modifications log | `docs/` |

### Out of scope

- **CVE-2026-45205 / commons-configuration2 2.15.0** — still open, still undiagnosed.
- Redesigning the filtering model. `FileSearchFilter`'s within-a-filter semantics are correct and
  are not touched.
- The `filtering.enabled=false` path — verify it during implementation; fix only if the same
  statement covers it, otherwise report separately.

---

## Detailed View

### Behaviour before and after

| Config | File has matching `<filefilter>` | Before | After |
|---|---|---|---|
| `fileFilterDefaultsToInclude=true` | no | all lines | all lines (unchanged) |
| `fileFilterDefaultsToInclude=true` | yes | filter decides | unchanged |
| **`fileFilterDefaultsToInclude=false`** | **no** | **nothing** | **all lines** |
| `fileFilterDefaultsToInclude=false` | yes | strict allow-list | unchanged (still strict) |

Only the third row changes. A user relying on today's behaviour to hide unfiltered files would be
relying on something the config documents as impossible, so this is a fix and not a breaking
change — but it **is** visible on upgrade and goes in the changelog.

### stdin is already correct

`StdinReaderThread.shouldEmit` returns `true` when `_searchFilter == null`, with no consultation of
the default. Only the file path has the second, wrong application. No change needed there, and the
fix brings files in line with stdin.

## Tests

JUnit 4, per the repo.

New fixture `ctrail-file-filter-strict.xml`: `filtering.enabled=true`,
`fileFilterDefaultsToInclude=false`, and one `<filefilter>` matching a name the test file does
**not** use.

Added to `FileReaderThreadFilterTest`:

| Test | Asserts |
|---|---|
| `unmatchedFileEmitsEveryLineEvenWhenDefaultsToExclude` | the regression test — every line emitted with no matching filter and the default set to exclude. **Fails before the fix.** |
| `matchedFileStillHonorsStrictAllowList` | a file that *does* match keeps strict allow-list behaviour — proves the fix does not weaken filtering |

Plus the existing `testNoFilterEmitsEveryLine` must stay green, guarding the
`defaultsToInclude=true` case.

⚠️ Verified by reverting the one-line fix and watching the new tests go red.

## Build / verify

1. `mvn clean test` — expect **96** (94 + 2), 0 failures.
2. Red-check: restore the buggy `return _defLineExclude;` and confirm exactly the two new tests fail.
3. Smoke test with the **shipped `etc/ctrail.xml`** — the case that exposed this — and confirm the
   file's lines now appear.
4. Confirm a file that *does* match a shipped filter still filters strictly.

## Definition of Done

- [ ] Unfiltered files emit every line regardless of `fileFilterDefaultsToInclude`.
- [ ] Filtered files keep strict allow-list behaviour when the default is exclude.
- [ ] `_defLineExclude` removed, no dead accessors left.
- [ ] Suite green at 96; new tests verified red-then-green.
- [ ] Smoke test with the shipped config shows file content.
- [ ] Changelog and modifications log written.

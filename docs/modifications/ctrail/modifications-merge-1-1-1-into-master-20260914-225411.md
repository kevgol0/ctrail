# Modifications: Merge 1.1.1 bugfix work into master

- **Date:** 20260914-225411
- **Project:** ctrail
- **Plan:** docs/plans/ctrail/plan-merge-1-1-1-into-master-20260914-225411.md
- **Merge commit:** ef62177
- **Pushed:** d43630d..ef62177 → origin/master

## Summary

Merged `origin/worktree-bugfix-1.1.1` (the version 1.1.1 release: filtering,
stdin, stdinfilter, and shutdown defect fixes) into `master` using a real merge
commit (`git merge --no-ff`) so both histories' commits are preserved. Resolved
7 conflicting files, verified the build and full test suite pass (52 tests, 0
failures/errors), and pushed to `origin/master`.

## Branch state before merge

- Mainline branch: `master` (no `main` exists).
- Merge base: `de47d40`.
- `master` had 7 unique commits (README rewrite, install.sh/ctr fixes, "Fix 12
  bugs", stdin example config, PR #9, project instructions/skills).
- `worktree-bugfix-1.1.1` had 2 unique commits (`2cb0bdf` release 1.1.1,
  `ce73bf2` `<stdinfilter>`).
- `.claude/`, `.github/instructions/`, and several `docs/` files existed only on
  master and were never touched by the 1.1.1 branch, so the merge preserved them
  automatically. Confirmed post-merge via `git ls-files`.

## Safety

- Tagged `pre-merge-1.1.1-backup` at master HEAD before merging (rollback point).

## Conflicts resolved (7 files)

1. **pom.xml** — auto-merged to version `1.1.1` (no manual action).
2. **README.md** — kept master's comprehensive rewrite; folded in the 1.1.1
   detailed `<execution>`/`<inputFiles>`/`<coloring>`/`<filtering>` config tables
   and the `## Changelog` link to the new CHANGELOG.md.
3. **bin/install.sh** — kept master's version-resolving, GitHub-Releases install
   script; dropped the old 1.1.1 AWS S3 approach.
4. **CtrailEntryPoint.java** — comment-only; kept 1.1.1's explanatory comment on
   the `-v` exclude-filter toggle.
5. **FileReaderThread.java** — combined both fixes: 1.1.1's read-position
   accounting (records `lastReadPosition` for every consumed line to stop the
   run-loop spin) placed BEFORE master's `-m/--match` `continue`, so filtered
   lines still advance the position. Dropped the unused `java.util.Deque` import.
6. **StdinReaderThread.java** — took 1.1.1's unified `readStdin()` design (fixes
   "cat file | ctr" producing no output and `-m` being ignored when a stdin
   filter is configured). Removed the now-dead `runPassthrough()` method.
7. **CtrailProps.java** — took 1.1.1's `getInstance()` override-based caching
   (`_instanceCfgOverride`), the file-filter dedupe fix (check the regex-form key
   via `fst.getFileName()`), the clearer error log message, and the null/empty
   version guard returning "UNK".
8. **FileSearchFilter.java** — comment-only; kept 1.1.1's `-v` explanation.
9. **LineFormatter.java** — merged designs: kept master's thread-safe
   local-variable `format()` while adopting 1.1.1's `refreshProps()` (config
   reload) and `_keyArray` O(1) key scan, plus `Locale.ROOT` case folding.

## Verification

- IDE file-problems (errors-only) on all resolved files: clean.
- `mvn clean test`: BUILD SUCCESS.
- Surefire totals: **Tests: 52, Failures: 0, Errors: 0, Skipped: 0**.
  (ERROR/WARN lines in the test log are intentional negative-path test scenarios.)

## Rollback

- Local: `git reset --hard pre-merge-1.1.1-backup`.
- Remote (if needed): `git revert -m 1 ef62177` then push.

## Follow-up note (not addressed here)

- On push, GitHub reported 7 Dependabot vulnerabilities (1 critical, 3 high, 3
  moderate) on the default branch. Out of scope for this merge; flag for a
  separate dependency-update task.

# Plan: Rebase the liveness branch onto master (1.1.1)

**Date:** 2026-09-27
**Status:** `Approved ("do your recommendation")` — 2026-09-27
**Branch:** `feat/liveness-tail-n-idle-notice` → rebased onto `origin/master` @ `3ecf55a`
**PR:** [kevgol0/ctrail#11](https://github.com/kevgol0/ctrail/pull/11) — currently `CONFLICTING`
**Jira / Confluence:** _n/a — personal tool repo_

---

## Concise View

### Why

Master gained 5 commits while the liveness branch was in flight, including a **1.1.1 release**
(`2cb0bdf`) that independently fixed filtering, stdin, shutdown and colour defects. PR #11 no
longer merges: 5 conflicts, and one of my three commits is now redundant.

### What happens to each commit

| Commit | Action |
|---|---|
| `d96b0c7` feat: file liveness | **Replay and rework.** The value of the branch. |
| `3e85e71` chore: bump 1.1.0 → 1.2.0 | **Replay, re-resolve** — base is now 1.1.1, target stays 1.2.0. |
| `83b10c3` fix: single-colorpair | **Drop.** Master fixed it first, same root cause and same `extractCount` call. |

### Conflicts and how each resolves

| File | Resolution |
|---|---|
| `StdinReaderThread.java` | **Take master's structure wholesale.** 1.1.1 collapsed three read loops into one `readStdin()` + `shouldEmit()`. Re-apply liveness as a *single* `noteActivity` call before `_output.put(...)`. |
| `LineFormatter.java` | Master added `refreshProps()` with cached fields. `_noticeColor` becomes one of those cached fields instead of a `final` set in the constructor. |
| `CtrailProps.java` | Keep master's `initColoring` (its colorpair fix) and master's new constants; add only the four liveness settings. |
| `pom.xml` | `1.2.0`. |
| `ctrail-single-colorpair.xml` | **Take master's.** Mine is redundant; master's fixture stays. |

### What must be re-verified, not assumed

The 18/18 mutation result does **not** carry over — `StdinReaderThread` has been restructured
underneath those tests. The full suite and the mutation checks are re-run on the new base.

Docs also need correcting: the liveness changelog and plan currently describe the last-line-drop
and single-colorpair bugs as "pre-existing, not fixed here". On this base that is **false** — both
are fixed on master. Leaving it would mislead.

[→ Detailed View](#detailed-view)

---

## Detailed View

### Safety

Tag the pre-rebase state first: `git tag backup/liveness-pre-rebase`. Nothing is force-pushed
until the full suite is green on the new base.

### Mechanics (no interactive rebase available in this environment)

```bash
git tag backup/liveness-pre-rebase HEAD
git rebase --onto origin/master d43630d 3e85e71   # replays d96b0c7 + 3e85e71 only
git branch -f feat/liveness-tail-n-idle-notice HEAD
```

Ending the range at `3e85e71` is what drops `83b10c3` — no interactive edit needed.

### What 1.1.1 changed that touches this work

From `2cb0bdf`:

- **stdin**: one read loop applying match *and* filter together; previously it read nothing at all
  unless `-m` or a stdin filter was set. My three `noteActivity` call sites no longer exist.
- **stdin source name**: now honours `prependFilenameToLine` via `CtrailProps.STDIN_FILTER_NAME`.
  This is the third quirk I reported — fixed upstream.
- **`_output` is a `BlockingDeque` and stdin uses `put()`** for back-pressure, because `add()`
  throws once `maxPendingLines` fills.
- **shutdown**: writer polls instead of parking in `take()`, the flag is volatile, the drain no
  longer sizes itself at `size()-1` (the dropped-last-line bug), drains oldest-first, flushes on
  exit.
- **colours**: single-`<colorpair>` fixed; unrecognised colour handled.

### One deliberate divergence from master's convention

Master moved stdin line emission to `put()` (blocking back-pressure). Liveness **notices keep
`offer()`**: a watchdog must never block or throw on a full queue — dropping a "no movement"
notice is strictly better than stalling the thread whose job is to report that nothing is
happening. Data lines follow master's `put()`; notices do not. Worth a comment in the code.

### Tests

Existing liveness tests carry over, with one expected casualty: `StdinReaderThreadIdleTest`
constructs `StdinReaderThread` with a `Deque` and asserts against the old three-loop behaviour.
It needs updating for the new signature (`BlockingDeque`) and the single loop, keeping the same
guarantees:

- an emitted line counts as movement
- a line dropped by `-m` does **not**
- a line dropped by the filter does **not**
- `_finished` set at EOF

Master's own test suite must also stay green — this branch must not regress 1.1.1's fixes.

### Verify

1. `mvn -o clean test` — master's tests plus the liveness tests, all green.
2. Re-run the mutation checks against the reworked code, especially the stdin `noteActivity` site.
3. Smoke test the packaged jar: file mode and stdin mode, banner, tail-N, idle, resume — and
   confirm 1.1.1's own fixes still hold (plain `cat file | ctr` produces output; the last line is
   no longer dropped).
4. Force-push, refresh the PR body.

## Definition of Done

- [ ] `83b10c3` dropped; two commits replayed onto `3ecf55a`.
- [ ] All 5 conflicts resolved, keeping master's fixes intact.
- [ ] `noteActivity` re-applied to the single stdin loop; only emitted lines count as movement.
- [ ] `_noticeColor` cached via `refreshProps()`.
- [ ] Version 1.2.0 from a 1.1.1 base.
- [ ] Full suite green — master's tests **and** the liveness tests.
- [ ] Mutation checks re-run on the new base.
- [ ] Smoke test confirms liveness works *and* 1.1.1's fixes are intact.
- [ ] Docs corrected: no longer claim the two bugs are unfixed; credit 1.1.1.
- [ ] PR #11 mergeable, body refreshed.

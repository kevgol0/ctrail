# Modifications: File Liveness — Tail-N, Startup Banner, Idle Notices

**Date:** 2026-09-27
**Branch:** `feat/liveness-tail-n-idle-notice` — cut from `d43630d` (1.1.0), **rebased onto `3ecf55a` (1.1.1)** on 2026-09-27
**Plan:** [plan-liveness-tail-n-idle-notice-20260927-184221.md](../../plans/ctrail/plan-liveness-tail-n-idle-notice-20260927-184221.md)

---

## Concise View

### What changed

Three signals that tell you whether a watched source is actually moving, for **files and stdin**:

| Signal | Setting | Default |
|---|---|---|
| Last N lines on open, line-accurate (`tail -n`) | `execution.tailLastLines` | `10` |
| Startup banner with size + last-modified age | `execution.showStartupBanner` | `true` |
| `no movement in Xs`, repeating, plus `resumed after …` | `execution.idleNoticeSeconds` | `30` |
| Color for ctrail's own messages | `coloring.noticeColor` | `cyan` |

Plus a `-n/--lines N` flag mirroring `tail -n`.

[→ Detailed View](#detailed-view)

### Result

- **94 tests pass** — the liveness tests alongside the 1.1.1 suite this now sits on.
- **12/12 mutations detected on the rebased code**, re-run from scratch because the stdin path was
  restructured upstream.
- Smoke-tested against the packaged jar for files, stdin, both CLI flags and back-compat.

### Three bugs found while building this — all fixed upstream, none carried here

The **1.1.1** release (`2cb0bdf`) landed on master while this branch was in flight and fixed all
three independently: the last line dropped at shutdown (`OutputWriterThread` draining
`size() - 1`), a single-`<colorpair>` config loading no colors, and `prependFilenameToLine` being
ignored for stdin. A commit on this branch that duplicated the colorpair fix was dropped during
the rebase.

### One design deviation from the approved plan

The plan specified an `IActivityTracker` interface. Built as a concrete `ActivityState` held by
composition instead: `lombok.accessors.chain = true` makes generated setters return `this`, and a
chained setter cannot implement a `void` interface method. Same behaviour, same single state
machine, one fewer type.

---

## Detailed View

### Files changed

| File | New? | Change |
|---|---|---|
| `files/ActivityState.java` | new | Liveness bookkeeping for one source: name, interval, last-activity, next-due, idle, finished. All mutable fields volatile — written by a reader thread, read by the monitor. |
| `files/IdleMonitorThread.java` | new | Daemon thread, 250 ms tick. Holds the **only** copy of the idle → notice → repeat → resume logic. `checkForIdle` fires and reschedules; static `noteActivity` records movement and emits the resume line. |
| `unit/DurationFormatter.java` | new | `45s`, `4m 12s`, `2h 09m`, `3d 04h`. Shared by banner and notices. |
| `files/FileTailTracker.java` | edit | `seekToLastNLines(int)` — backwards 8 KB chunked scan for the Nth-from-last newline. Constructor prefers it over byte-skip when `tailLastLines > 0`. Holds an `ActivityState`. |
| `files/FileReaderThread.java` | edit | Calls `noteActivity` **before** queueing each emitted line, so a resume notice lands ahead of the data. |
| `files/StdinReaderThread.java` | edit | Holds an `ActivityState` named `stdin`; `noteActivity` in all three read loops; `setFinished(true)` at EOF. |
| `unit/LogLine.java` | edit | `_notice` flag + 4-arg constructor; the 3-arg one delegates, so all existing call sites compile untouched. |
| `unit/LineFormatter.java` | edit | Early branch: notices render in `noticeColor`, with no filename prefix and no keyword matching. |
| `props/CtrailProps.java` | edit | Four new settings read from XML with defaults. |
| `CtrailEntryPoint.java` | edit | Banner emission, activity-source snapshot, monitor lifecycle, `-n/--lines`, `-e` now zeroes tail-N too. |
| `etc/ctrail.xml`, `etc/ctrail-stdin-example.xml` | edit | New keys, commented. Additive only — no existing value changed. |
| `README.md` | edit | Config reference, a "Knowing whether a file is actually moving" section, CLI table, usage examples. |
| 6 test classes + 2 fixtures | new | See below. |

### Three decisions worth knowing about

**Tail-N seeks, it does not pre-read.** The tracker positions the file pointer at the Nth-from-last
line and lets the existing reader loop emit it. History therefore gets coloring, `-m` matching,
file filters and the filename prefix for free, with no duplicated logic.

**The monitor reads a snapshot list, never the live tracker deque.** `FileReaderThread` `take()`s
a tracker out of the deque while reading it; a monitor iterating the deque would intermittently
not see it. `CtrailEntryPoint` builds an `ArrayList` of `ActivityState` at startup and never
mutates it afterwards.

**Only *emitted* lines count as movement.** A line dropped by `-m` or a filter is not movement you
can see, so a filter that drops everything still lets the source go idle. `StdinReaderThreadIdleTest`
pins this in both the match and filter paths.

### Shutdown ordering

`CtrailEntryPoint.shutdown()` stops the monitor **before** the reader and writer. Stopped after
the writer, a notice enqueued during the drain could print behind the last real line.

### Tests

| Class | Tests | Covers |
|---|---|---|
| `files/IdleMonitorThreadTest` | 10 | Whole state machine with hand-set timestamps — no files, no threads, no sleeping. Fires on interval; repeats with growing elapsed; resume; finished skipped; disabled at `0`; full queue does not throw; nulls. |
| `files/FileTailTrackerSeekTest` | 9 | Empty file, trailing newline, no trailing newline, fewer than N, exactly N, single line, CRLF, multi-chunk (5000 lines), non-positive no-op. |
| `files/StdinReaderThreadIdleTest` | 7 | Movement in all three read loops; dropped lines are not movement (match **and** filter paths); finished at EOF; naming. |
| `files/FileReaderThreadIdleTest` | 3 | End-to-end: tail-N history reaches the queue; resume notice precedes the line that ended the silence; a quiet file is announced. |
| `unit/DurationFormatterTest` | 5 | Every format branch plus zero and negative. |
| `unit/NoticeFormattingTest` | 4 | Notice color; a notice containing a keyword does **not** pick up its color; no filename prefix; ordinary lines still colored. |
| `props/LivenessPropsTest` | 4 | Values read from config; all features switchable off; byte-skip intact; a config predating the feature gets the documented defaults. |

Fixtures: `ctrail-liveness.xml`, `ctrail-liveness-disabled.xml`. The first sets
`idleNoticeSeconds=1` so nothing waits 30 s, and sets `noticeColor` to **purple** — deliberately
not the compiled default, so the tests prove a real config read rather than a coincidence.

### Red-then-green verification

18 mutations applied one at a time, each followed by a targeted test run. **All 18 produced a red
suite.** Two survived the first pass; both were genuine gaps, not false alarms:

| Survivor | Cause | Fix |
|---|---|---|
| `noteActivity` removed from a stdin read loop | The filtered read loop had **no test at all** — the earlier bulk edit had also silently skipped `runPassthrough`, which a test then caught | Added `aFilteredLineThatSurvivesCountsAsMovement` and `aLineDroppedByTheFilterIsNotMovement` |
| `setNoticeColor(...)` read removed | The fixture used `cyan`, which is also the compiled default, so the assertion could not tell the two apart | Fixture changed to `purple` |

One testability seam was added rather than weakening an assertion: `noteActivity` gained a
clock-injecting overload, because the resume path could not otherwise be asserted
deterministically.

### Smoke tests (packaged jar)

| Case | Result |
|---|---|
| File: banner, tail-5, idle at 2 s and 4 s, resume, re-idle | ✅ exactly as designed; resume printed ahead of the new line |
| Stdin: slow producer, 6 s gap | ✅ `watching stdin`, idle ×2 with growing elapsed, `resumed after 5s`, then the line |
| `-n 12` | ✅ banner + 12 lines |
| `-n 0` | ✅ falls back to byte-skip |
| `-n abc` | ✅ warns, keeps the configured value |
| `-e` | ✅ whole file |
| All features off | ✅ 50 lines, no banner, no notices — byte-skip behaviour unchanged |
| `-h` | ✅ lists `-n,--lines <N>` |

### Notes on test noise

`ERROR - java.lang.InterruptedException: sleep interrupted` appears during the suite. That is
`FileReaderThread`'s pre-existing `_logger.error` on interrupt, triggered by test teardown
interrupting the reader. Cosmetic, pre-existing, not introduced here.

One unreproducible test failure occurred mid-session against a stale incremental `target/`; it
did not recur across three `mvn clean test` runs or any subsequent targeted run.

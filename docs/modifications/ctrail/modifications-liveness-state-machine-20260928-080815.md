# Modifications: phase 3 — liveness state machine (CTRAIL-4, -10, -17)

**Date:** 2026-09-28
**Branch:** `CTRAIL-4/liveness-state-machine` from `chore/release-1.2.1`
**Jira:** [CTRAIL-4](https://verame.atlassian.net/browse/CTRAIL-4), [CTRAIL-10](https://verame.atlassian.net/browse/CTRAIL-10), [CTRAIL-17](https://verame.atlassian.net/browse/CTRAIL-17)
**Plan:** [plan-fix-code-review-bugs-20260927-235255.md](../../plans/ctrail/plan-fix-code-review-bugs-20260927-235255.md) — phase 3
**Version:** 1.2.1 → **1.2.2**

---

## Concise View

`ActivityState` now owns its transitions as `synchronized` instance methods; the public setters are
gone. 106 tests, 7 of 7 mutations caught.

[→ Detailed View](#detailed-view)

---

## Detailed View

### The shape of the change

Before, `IdleMonitorThread` held static transition logic that mutated a foreign data bag through
public setters. Check-then-act was therefore split across two threads with nothing making it
atomic. Now:

```java
public synchronized boolean noteActivity(Deque<LogLine> out, long now);
public synchronized boolean checkForIdle(Deque<LogLine> out, long now);
```

`IdleMonitorThread` shrank to a loop that supplies the clock. `emitNotice` moved onto
`ActivityState` with it.

### CTRAIL-10: ordering, not just the offer

`_idle` is cleared **only when `emitNotice` returns true**. A notice dropped by a full queue now
leaves the source idle, so it is retried on the next line instead of the state silently claiming a
resume the user never saw.

The counterpart matters too: in `checkForIdle` the **due time advances whether or not the notice was
queued**. Otherwise a full queue would turn the watchdog into a tight re-notify loop. Both are
tested.

### ⚠️ What is not directly covered, and why

Removing `synchronized` changes nothing single-threaded, so no deterministic unit test can catch
it — the mutation survived the first red-check pass. Rather than write a probabilistic stress test
that would flake in CI, the property that *prevents* the race is pinned structurally:
`idleStateIsOnlyMutableThroughSynchronizedTransitions` reflects over the public API and fails if a
public setter reappears or if either transition loses `synchronized`. Verified: stripping
`synchronized` from `checkForIdle` fails it with "checkForIdle must be synchronized".

This is a weaker guarantee than a real concurrency test and is recorded as such.

### Test rework

Removing the setters broke three test classes that had been poking state directly. They were
rewritten to reach the same states through the real API:

* `IdleMonitorThreadTest` → `ActivityStateTest`, driving both transitions with hand-set clocks
* `FileReaderThreadIdleTest` drives a source idle via `checkForIdle` with a future clock instead of
  `setIdle(true)`
* `StdinReaderThreadIdleTest` compares the activity clock before and after with a short sleep,
  instead of pinning it to an epoch-1970 marker with a setter

### Red-check

| Mutation | Result |
|---|---|
| reschedule in `checkForIdle` removed | RED — `aNoticeIsNotRepeatedUntilAnotherFullIntervalHasPassed` |
| `_idle` cleared before queueing | RED — `aResumeNoticeDroppedByAFullQueueIsRetriedOnTheNextLine` |
| reschedule in `noteActivity` removed | RED — `activityReschedulesTheNextNotice` |
| `_finished` guard removed | RED — `finishedSourcesAreNotAnnounced` |
| reader `noteActivity` call removed | RED — `resumeNoticeLandsAheadOfTheLineThatEndedTheSilence` |
| stdin `noteActivity` call removed | RED — 2 tests |
| `synchronized` removed from `checkForIdle` | RED — structural guard |

### Verification

- Suite: **106 passing**.
- Smoke, packaged jar, stdin with a 5s gap: banner, `no movement in 2s`, `no movement in 4s`,
  `resumed after 4s`, then the line. Ordering intact.
- `ctr --version` reports 1.2.2.

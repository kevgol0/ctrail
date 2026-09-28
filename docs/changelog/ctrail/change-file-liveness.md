# Feature: File Liveness Signals

**Added:** 2026-09-27 — `feat/liveness-tail-n-idle-notice`, on top of 1.1.1

## Summary

ctrail can now tell you whether the thing it is watching is actually moving. Previously an idle
file and a broken tail looked identical: a blank screen.

Three signals, at three moments, for both files and stdin:

1. **On open** — a banner naming the source, its size and how long ago it changed.
2. **On open** — the last N lines, using a real line-accurate `tail -n` instead of a byte offset.
3. **While quiet** — `no movement in 30s`, repeating, and `resumed after …` when data returns.

## ⚠️ Behaviour change on upgrade

`tailLastLines` defaults to `10`. An install that never touched `skipAheadInBytes` switches from
"the last ~1000 bytes" to "the last 10 lines". This is the intended improvement — a byte offset
almost always opens mid-line — but it *is* a change. Set `<tailLastLines>0</tailLastLines>` to
keep the old behaviour exactly.

## Configuration

```xml
<execution>
  <!-- lines of history shown when a file is opened, like `tail -n N`;
       0 disables and falls back to skipAheadInBytes -->
  <tailLastLines>10</tailLastLines>

  <!-- one banner line per input at startup: size and last-modified age -->
  <showStartupBanner>true</showStartupBanner>

  <!-- seconds of silence before "no movement in ..."; repeats at this
       interval, and pairs with "resumed after ...". 0 disables -->
  <idleNoticeSeconds>30</idleNoticeSeconds>
</execution>

<coloring>
  <!-- ctrail's own messages; never picks up keyword coloring -->
  <noticeColor>cyan</noticeColor>
</coloring>
```

New CLI flag: `-n, --lines N` — overrides `tailLastLines`. `-e/--entirefile` now zeroes tail-N as
well as the byte skip.

## What it looks like

```
ctrail: watching ctr-smoke.log - 391 bytes, modified 7h 14m ago
ctr-smoke.log:line 46
ctr-smoke.log:line 47
ctr-smoke.log:line 48
ctr-smoke.log:line 49
ctr-smoke.log:line 50
ctrail: ctr-smoke.log - no movement in 2s
ctrail: ctr-smoke.log - no movement in 4s
ctrail: ctr-smoke.log - resumed after 5s
ctr-smoke.log:line 51 with an error in it
```

Stdin gets everything except tail-N, which a pipe cannot support:

```
ctrail: watching stdin
stdin:first
ctrail: stdin - no movement in 2s
ctrail: stdin - no movement in 4s
ctrail: stdin - resumed after 5s
stdin:second
```

## Implementation notes

**One state machine.** `IdleMonitorThread` holds the only copy of the idle → notice → repeat →
resume logic; `FileTailTracker` and `StdinReaderThread` each expose an `ActivityState` for it to
watch. Inlining the checks per reader would have meant two copies that drift apart.

```java
// the elapsed figure is measured from the last line seen, not the last notice,
// so a repeat reads "1m 00s" rather than "30s" all over again
source_.setIdle(true);
source_.setIdleNoticeDueMillis(now_ + source_.getIdleIntervalMillis());
emitNotice(_output, source_.getName() + " - no movement in "
        + DurationFormatter.format(now_ - source_.getLastActivityMillis()));
```

**Tail-N seeks rather than pre-reading.** `FileTailTracker.seekToLastNLines` scans backwards in
8 KB chunks for the Nth-from-last newline and positions the pointer there; the existing reader
loop then emits that history through the normal path, picking up coloring, `-m` matching, file
filters and the filename prefix with no duplicated logic.

**Movement means an emitted line.** A line dropped by a filter is not movement you can see, so a
filter that drops everything still lets the source go idle.

**Notices never take down a thread.** They use `offer` on the bounded output queue, not `add`,
which throws when full — on the watchdog that would silently kill the one thread whose job is
reporting that nothing is happening.

## Test results

```
Tests run: 94, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

94 tests — this branch's liveness tests alongside the 1.1.1 suite it now sits on. Every new
production line was verified by deleting it and watching a test go red: **12/12 mutations detected
on the rebased code**, re-run from scratch because the stdin path was restructured underneath the
original checks.

Smoke-tested against the packaged jar: file mode (banner, tail-N, idle, repeat, resume, re-idle),
stdin mode (banner, idle, resume), `-n 12`, `-n 0`, `-n abc`, `-e`, and an all-features-off
config confirming the original byte-skip behaviour is untouched.

## Bugs found while building this, all fixed upstream

Three defects surfaced during testing. All three were fixed independently in the **1.1.1** release
(`2cb0bdf`) that landed on master while this branch was in flight, so none of them are carried
here:

- **The last line was dropped at shutdown** — `OutputWriterThread` drained `size() - 1`. 1.1.1
  also made the writer poll instead of parking in `take()`, made the flag volatile, drains
  oldest-first and flushes on exit.
- **A config with exactly one `<colorpair>` loaded no colors.** Fixed in 1.1.1 with the same
  `extractCount` call this branch had briefly duplicated; that duplicate commit was dropped during
  the rebase.
- **`prependFilenameToLine` was ignored for stdin.** 1.1.1 made the source name conditional.

This branch is rebased onto that work and its tests run green alongside it.

## Rebase note

Originally cut from `d43630d` (1.1.0). Rebased onto `3ecf55a` (1.1.1) on 2026-09-27. The stdin
read path was restructured upstream from three loops into a single `readStdin()`/`shouldEmit()`
pair, so the liveness hook is now **one** `noteActivity` call rather than three. The full suite
(94 tests) and the mutation checks (12/12) were re-run on the new base.

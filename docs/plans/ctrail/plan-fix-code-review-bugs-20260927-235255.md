# Plan: Fix the code-review bugs (CTRAIL-1 … CTRAIL-15)

**Date:** 2026-09-27
**Status:** `Approved ("go")` — 2026-09-28; phase 0 in progress
**Base:** `main` @ `653f6cf`
**Jira:** [CTRAIL](https://verame.atlassian.net/jira/software/c/projects/CTRAIL) — 15 Bugs, 5 Tasks
**Confluence:** [Plan: Fix the code-review bugs (CTRAIL-1 to CTRAIL-15)](https://verame.atlassian.net/wiki/spaces/CTrail/pages/95125505)

---

## Concise View

### ⚠️ The sequencing problem, before anything else

**CTRAIL-16 (order-dependent suite) has to be fixed first, even though it is a Task and you asked
for bugs first.**

Every fix below will be validated by `mvn test`. Right now that suite passes only in surefire's
default order — reverse order gives **14 errors**. A suite that passes by luck cannot tell me
whether a fix worked or whether I broke something. Fixing 15 bugs against an untrustworthy gate
means re-doing the verification later.

It is one phase, it is small, and it makes everything after it meaningful. **I recommend doing it
first and would rather argue for it than quietly skip it.**

### Proposed phases — one branch and one PR each

| # | Branch | Tickets | Size |
|---|---|---|---|
| 0 | `fix/test-isolation` | CTRAIL-16, CTRAIL-18 | S |
| 1 | `fix/cli-and-config` | CTRAIL-6, CTRAIL-7, CTRAIL-12 | S |
| 2 | `fix/filtering-include-default` | CTRAIL-2 | S |
| 3 | `fix/liveness-state-machine` | CTRAIL-4, CTRAIL-10, CTRAIL-17 | M |
| 4 | `fix/file-lifecycle` | CTRAIL-5, CTRAIL-9, CTRAIL-11, CTRAIL-13, CTRAIL-15 | L |
| 5 | `fix/shutdown-and-stdin-fallback` | CTRAIL-1, CTRAIL-3 | M |
| 6 | `fix/charset` | CTRAIL-14 | L |
| 6b | `feat/tail-last-unified-config` | CTRAIL-8 | M |
| 7 | `chore/hot-path-cleanup` | CTRAIL-19, CTRAIL-20 | S |

Bugs are phases 1–6. Phases 0 and 7 are the Tasks; 0 is argued above, 7 is genuinely last.

### Decisions taken (2026-09-28)

**CTRAIL-5 — rotation: reseek, and say so.** On detecting `length < _lastReadPosition`, reseek to
0 and emit a notice:

```
ctrail: app.log - rotated, following new file
```

Not silent. This is the same principle the whole liveness feature rests on: when ctrail changes
what it is doing, it says so rather than going quiet. It also retires the worst part of CTRAIL-5 —
the tool asserting "no movement" about a file that was rotated out from under it.

**CTRAIL-8 — one setting, two units.** `tailLastLines` and `skipAheadInBytes` collapse into a
single key taking a unit-suffixed value, mirroring `tail -n` and `tail -c`:

```xml
<tailLast>10 lines</tailLast>
<!-- or -->
<tailLast>1000 bytes</tailLast>
```

Both old keys remain as **deprecated aliases** that still parse and map onto it, logging a
deprecation warning.

⚠️ **This dissolves CTRAIL-8's actual defect rather than patching it.** The bug was that
`skipAheadInBytes` became silently unreachable once `tailLastLines` defaulted to a non-zero value.
As a live alias it can no longer be unreachable — there is only one setting to be reachable. The
default value stops mattering for existing configs, because any config that set either key keeps
working.

**Assumption, easily changed:** for a config that sets *neither*, the default is `10 lines` —
status quo since 1.2.0 and the `tail -n` convention. Say the word if you want something else.

**Consequence:** CTRAIL-8 is no longer a one-line change. Parsing, validation, two deprecation
paths and back-compat tests make it a phase of its own (6b), not a passenger in phase 1.

### ⚠️ Two phases I would not bundle in blind

**Phase 4 (file lifecycle)** and **phase 6 (charset)** are the two that change real behaviour
rather than correcting an obvious mistake:

- **CTRAIL-5 rotation handling** means deciding what ctrail *should* do when a file shrinks.
  Reseek to 0 and replay the new file? Reopen by path? Emit a `rotated` notice? That is a product
  decision, not a bug fix, and I want your call before I build it.
- **CTRAIL-14 charset** replaces `RandomAccessFile.readLine()` with explicit byte-then-decode
  reading. It touches the read loop, the seek arithmetic and every filter comparison. It is the
  single highest-regression-risk change in the list, and the current suite has no non-ASCII
  coverage to catch a mistake.

Everything else is a contained correction with a clear right answer.

[→ Detailed View](#detailed-view)

---

## Detailed View

### Phase 0 — `fix/test-isolation` (CTRAIL-16, CTRAIL-18)

Give `CtrailProps` a package-visible `resetForTests()` that clears `_instance` and
`_instanceCfgOverride`; call it from `@After` in every test class that sets `CTRAIL_CFG` or mutates
props. Convert `StdinFilterTest`'s bare mutation to `try/finally`, matching the pattern
`StdinReaderThreadTest` already uses.

Rebuild `OutputWriterThreadTest`'s stream with autoflush **off** and stop flushing inside
`written()`, so only the production `flush()` can make bytes appear.

**Gate:** `mvn test -Dsurefire.runOrder=reversealphabetical` green, and `-Dsurefire.runOrder=random`
green across three consecutive runs. Then add `runOrder=random` to the pom so this cannot regress
silently.

### Phase 1 — `fix/cli-and-config` (CTRAIL-6, -7, -12)

Four independent one-to-five-line corrections:

- **CTRAIL-7** — replace the `isNumeric` guard with `try/catch (NumberFormatException)`, warn and
  return, matching the method's own Javadoc.
- **CTRAIL-6** — apply `-n` before `-e`, so `-e` wins as documented. Add a test for both orders.
- **CTRAIL-12** — pass `Locale.ROOT` to every `String.format` in `DurationFormatter`; add a test
  that runs under a forced non-Latin-digit locale.

### Phase 2 — `fix/filtering-include-default` (CTRAIL-2)

`FileSearchFilter.shouldIncludeLineDueToSeachTerms` returns `true` when the include list is empty,
before consulting `_defLineInclude`. Tests: exclude-only filter with `fileFilterDefaultsToInclude`
both true and false; include-only filter unchanged; both lists populated unchanged.

This completes the fix PR #13 started.

### Phase 3 — `fix/liveness-state-machine` (CTRAIL-4, CTRAIL-10, CTRAIL-17)

Move the transitions onto `ActivityState` as `synchronized` instance methods and delete the public
setters, so check-and-act is atomic:

```java
synchronized boolean noteActivity(long now, Deque<LogLine> out);   // emits resume, clears idle
synchronized boolean checkForIdle(long now, Deque<LogLine> out);   // emits notice, sets idle
```

`IdleMonitorThread` shrinks to a loop. For **CTRAIL-10**, clear `_idle` only after `offer()`
succeeds, so a dropped resume notice is retried on the next line instead of being lost.

**CTRAIL-17:** add the test that actually kills the mutation — advance less than one interval and
assert **no** second notice, then past it and assert exactly one.

### Phase 4 — `fix/file-lifecycle` (CTRAIL-5, -9, -11, -13, -15)

Grouped because they all concern a tracker's lifetime and all touch `FileTailTracker`.

- **CTRAIL-15** — add `FileTailTracker.close()`, call it for every tracker in `shutdown()`, and
  use try/finally in `getFilesFromArgs`.
- **CTRAIL-11** — constructor closes and rethrows instead of logging and continuing with an
  inconsistent tracker.
- **CTRAIL-9** — the `IOException` handler closes the file, marks the `ActivityState` finished, and
  emits a visible `read error, no longer watching` notice.
- **CTRAIL-13** — count `\r` as a terminator in the backwards scan so it matches `readLine()`.
- **CTRAIL-5** — rotation: `getRemainingSize()` detects `length < _lastReadPosition`, reseeks to 0,
  and the reader emits `ctrail: <file> - rotated, following new file`. Tests cover truncate-in-place
  and grow-after-truncate; rename-and-create stays out of scope (it needs reopen-by-path, which is a
  feature, not this fix).

### Phase 5 — `fix/shutdown-and-stdin-fallback` (CTRAIL-1, CTRAIL-3) `[FINISHED]` — 1.3.1, branch `CTRAIL-1/shutdown-and-stdin-fallback`

- **CTRAIL-1** — add `volatile boolean _shutdownRequested`, set inside the `synchronized` block
  before `notifyAll()`, and wait in `while (!_shutdownRequested) { _runtimeHolder.wait(); }`.
- **CTRAIL-3** — key the stdin fallback on `args_.length == 0`; when arguments were given but none
  were readable, log at ERROR and exit non-zero.

**Gate for CTRAIL-1:** 200 trials of `ctr < /dev/null` with zero hangs. The current 1-in-30 rate
means 30 trials is not enough evidence; a clean 200 is.

### Phase 6 — `fix/charset` (CTRAIL-14)

Read bytes and decode explicitly with a configurable charset defaulting to UTF-8, replacing
`RandomAccessFile.readLine()`; pass the same charset to the stdin `InputStreamReader`.

Highest regression risk in the list — it sits under the read loop, the tail-N seek and every filter
comparison. Wants its own PR and its own non-ASCII test fixtures.

### Phase 6b — `feat/tail-last-unified-config` (CTRAIL-8)

New `<tailLast>` key parsed as `<number> <unit>` where unit is `lines` or `bytes`; a bare number is
read as lines. `tailLastLines` and `skipAheadInBytes` map onto it and log a deprecation warning
naming the replacement. `-n/--lines` keeps its current meaning; consider a matching `-c/--bytes`.

Precedence when more than one is present: explicit `<tailLast>` beats either alias; if only the two
aliases are set, `tailLastLines` wins and the conflict is logged.

Tests: each unit parses; a bare number means lines; both aliases still work and warn; a malformed
value falls back to the default with a warning rather than throwing; README and `etc/ctrail.xml`
updated with the new key and the deprecation note.

### Phase 7 — `chore/hot-path-cleanup` (CTRAIL-19, CTRAIL-20)

- **CTRAIL-20** — delete the dead inline `-m` block; hoist the needle to the constructor.
- **CTRAIL-19** — make `getInstance()`'s fast path lock-free via a `volatile` field, or cache props
  in `LineFormatter` / `StdinReaderThread` as `FileReaderThread` already does.

## Tests

Every phase adds tests that fail before the fix and pass after. ⚠️ **Each is verified by reverting
the fix and watching the new test go red** — and, given CTRAIL-17, the mutation must target the
exact line the fix adds, not a neighbour.

No phase is done until the full suite is green **under random run order**, not just the default.

## Build / verify

Per phase: `mvn clean test` (random order) → red-check each new test → `mvn clean package` → smoke
test the affected path against the shipped `etc/ctrail.xml` → PR → transition the Jira ticket.

## Open questions — none blocking

Both blocking questions were answered on 2026-09-28; see **Decisions taken** above.

1. **Scope.** All 8 phases, or bugs only (1–6) with 0 and 7 deferred? I argue phase 0 is not
   optional regardless.
2. **Delivery.** Eight PRs in sequence, or fewer larger ones? Eight is more reviewable; fewer is
   less overhead on a solo repo.

## Definition of Done

> **Tests are not optional** — every fix has a test that fails without it, verified by reverting
> the fix; the full suite green **under random run order**; smoke-tested against the shipped config;
> committed on a feature branch; plan and changelog updated; the Jira ticket transitioned and linked
> to the PR.

- [ ] Phase 0 lands first and the suite is order-independent.
- [ ] CTRAIL-1 … CTRAIL-15 each closed by a test that fails without the fix.
- [ ] No phase regresses another: full suite green at the end of each.
- [ ] Each Jira ticket references its PR and is moved out of Backlog.

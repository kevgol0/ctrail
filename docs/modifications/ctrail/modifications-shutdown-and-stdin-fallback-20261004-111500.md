# Modifications: phase 5 — shutdown and stdin fallback (CTRAIL-1, CTRAIL-3)

**Date:** 2026-10-04
**Branch:** `CTRAIL-1/shutdown-and-stdin-fallback` from `main` @ `fdcddb9`
**Version:** 1.3.0 → **1.3.1**
**Plan:** [plan-fix-code-review-bugs-20260927-235255.md](../../plans/ctrail/plan-fix-code-review-bugs-20260927-235255.md) — phase 5

---

## Concise View

- **CTRAIL-1** — guarded wait on a `volatile` flag; no more lost-wakeup hang. 0/200 hangs.
- **CTRAIL-3** — stdin only when no file is named; all-unreadable files → exit 2 with a message.
- 7 new tests, each red-checked by reverting its fix line. Suite: 125 green.
- ⚠️ Behaviour change: `printf x | ctr typo.log` now exits 2 instead of tailing the pipe.

[→ Detailed View](#detailed-view)

---

## Detailed View

### CTRAIL-1 — lost wakeup ([↑](#concise-view))

`initiateShutdown()` sets `_shutdownRequested` inside the `synchronized` block before
`notifyAll()`. `awaitShutdownInstruction(int)` delegates to two helpers that loop on the flag:
`awaitShutdownIndefinitely()` and `awaitShutdownFor(millis)` (deadline-based so a spurious wakeup
does not reset the timeout). The `InterruptedException` handler now restores the interrupt flag.

The race is made deterministic in `ShutdownWaitTest` by calling `initiateShutdown()` before
`awaitShutdownInstruction()`; the old code blocks forever, so the test has a 5 s timeout.

Gate from the plan: 200 trials of `java -jar target/ctrail-1.3.1.jar < /dev/null`, each under a
10 s alarm — **0 hangs**. No baseline run against 1.3.0 was done this session; the deterministic
unit test is the proof the race existed and is closed.

### CTRAIL-3 — stdin fallback ([↑](#concise-view))

- `initInputReaderThread` throws `NoReadableInputException` when `args_.length > 0` and no tracker
  was created; stdin is chosen only when `args_.length == 0`.
- `main` catches it, prints `ctrail: <message>` to **stderr** and exits **2**. Not logged at ERROR:
  logback's only appender is stdout, which would print it twice and pollute piped output. Logged at
  DEBUG instead.
- `getFilesFromArgs` skip message: INFO → WARN, so it is visible under the shipped `warn` root.

A good file among bad ones still tails the good file — only "none readable" is fatal.

⚠️ Side effect: on a CLI parse error (`ctr --bogus`) `loadArgsAndOverrides` returns the raw args,
so the unknown option is treated as an unreadable file and ctrail exits 2. Before, it silently
tailed stdin. Judged an improvement; not separately tested.

### Deviations from house standards ([↑](#concise-view))

- Tests are **JUnit 4**, not JUnit 5 + Mockito: the whole suite and the pom are JUnit 4, and this
  change matches the surrounding code rather than mixing frameworks.
- `wakeupWithoutRequestKeepsWaiting` reaches the private `_runtimeHolder` by reflection to simulate
  a spurious wakeup.

### Version ([↑](#concise-view))

Patch bump (bug fixes). `pom.xml`, `README.md` install example, `bin/install.sh` usage line,
`.github/instructions/project.instructions.md` current-version row, `CHANGELOG.md`.

### Not done ([↑](#concise-view))

- CTRAIL-8 (phase 6b) — next. ⚠️ Its Jira ticket's agreed design (nested `<tailLast><count>`
  `<unit>`) supersedes the plan's unit-suffixed string; the plan text still shows the old design.
- No PR opened, Jira not transitioned.

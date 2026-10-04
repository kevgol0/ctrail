# Change: shutdown wait and stdin fallback (CTRAIL-1, CTRAIL-3) — 1.3.1

## Summary

- **CTRAIL-1** — the shutdown wait is now a guarded wait on a `volatile _shutdownRequested` flag,
  fixing a lost-wakeup hang and a spurious-wakeup teardown.
- **CTRAIL-3** — stdin is tailed only when no file argument is given. Named-but-unreadable files
  exit with status 2 and a stderr message; skipped files log at WARN.

## Code

```java
// initiateShutdown()
synchronized (_runtimeHolder)
{
	_shutdownRequested = true;
	_runtimeHolder.notifyAll();
}

// awaitShutdownIndefinitely(), caller holds _runtimeHolder
while (!_shutdownRequested)
{
	_runtimeHolder.wait();
}
```

```java
// initInputReaderThread()
if (args_.length > 0 && _fileTrackers.isEmpty())
{
	throw new NoReadableInputException(...);   // main(): stderr + exit 2
}
if (args_.length == 0) { /* stdin reader */ }
```

## Tests

| Test | Covers | Red-checked by |
|---|---|---|
| `ShutdownWaitTest.shutdownRequestedBeforeWaitIsNotLost` | lost wakeup | removing `_shutdownRequested = true` → times out |
| `ShutdownWaitTest.wakeupWithoutRequestKeepsWaiting` | spurious wakeup | `while` → `if` → fails |
| `ShutdownWaitTest.timedWaitReturnsAfterTimeoutWithoutRequest` | timed-wait contract | — (guards the rewrite) |
| `StdinFallbackTest` (4 tests) | missing file, directory, good+bad mix, no args | restoring the old `size() <= 0` branch → fails |

- Full suite: **125 passed, 0 failed** (random run order).
- `java -jar target/ctrail-1.3.1.jar < /dev/null` × 200: **0 hangs**.
- Smoke: typo + pipe → exit 2 with message; no args + pipe → tails stdin; good + bad file → tails
  the good one with a WARN for the bad one.

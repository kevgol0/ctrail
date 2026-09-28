# Fix: OutputWriterThread's flush was untested (CTRAIL-18)

**Changed:** 2026-09-28 — `fix/test-isolation`

## Summary

`OutputWriterThread.run()` ends with `_sout.flush()`, present because a redirected `PrintStream`
does not auto-flush. Deleting that line left the whole test class green. It is now covered: the
same deletion fails **5 of 6** tests.

No production change — this is a test-harness fix.

## Why the old harness could not see it

Two layers defeated the check:

1. `OutputWriterThreadTest` built its stream as `new PrintStream(_bytes, true, ...)` — autoflush **on**
2. `written()` called `_sout.flush()` itself before reading

So the test constructed precisely the auto-flushing case the production line does **not** apply to.

## The part that was not obvious

Turning autoflush off was **not enough** — the first attempt at this fix still left the mutation
alive. `PrintStream` pushes its internal char buffer down to the underlying `OutputStream` on every
`println` regardless of autoflush; only the final `out.flush()` is skipped, and on a bare
`ByteArrayOutputStream` that is a no-op because the bytes are already there.

The sink itself has to hold bytes back. A `BufferedOutputStream` now sits between them:

```java
_bytes    = new ByteArrayOutputStream();
_buffered = new BufferedOutputStream(_bytes, 8192);   // holds bytes until flushed
_sout     = new PrintStream(_buffered, false, StandardCharsets.UTF_8.name());
```

`written()` reads `_bytes` without flushing, so a visible byte proves the writer flushed.

## Added test

`testDrainIsFlushedToANonAutoFlushingStream` names the contract directly — the `ctr file | less`
and `ctr file > out.txt` case, where an unflushed drain is never seen.

## Verification

```
Tests run: 97, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Red-check, production `_sout.flush()` deleted:

```
Tests run: 6, Failures: 5
  testDrainEmitsEveryPendingLine:134 line-one missing
  testDrainIsFlushedToANonAutoFlushingStream:202 drained output must be flushed, not left in the buffer
  testDrainOfSingleLine, testDrainPreservesOrder, testNormalRunLoopWritesLines
```

Previously that same deletion left all 5 green.

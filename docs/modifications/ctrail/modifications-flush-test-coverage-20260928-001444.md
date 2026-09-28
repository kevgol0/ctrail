# Modifications: cover OutputWriterThread's flush (CTRAIL-18)

**Date:** 2026-09-28
**Branch:** `fix/test-isolation` from `main` @ `653f6cf`
**Jira:** [CTRAIL-18](https://verame.atlassian.net/browse/CTRAIL-18)
**Plan:** [plan-fix-code-review-bugs-20260927-235255.md](../../plans/ctrail/plan-fix-code-review-bugs-20260927-235255.md) — phase 0

---

## Concise View

Test-harness only; no production change. The production `_sout.flush()` went from uncovered
(deleting it left 5 tests green) to covered (deleting it fails 5 of 6).

⚠️ **Phase 0 shrank from two tickets to one.** CTRAIL-16 (order-dependent suite) was re-measured
and does **not** reproduce — see below.

[→ Detailed View](#detailed-view)

---

## Detailed View

### CTRAIL-16 withdrawn

Re-measured on `main` @ `653f6cf`: **9 runs, 3 orders (reverse / random / alphabetical), all 96
green.** The suite is not order-dependent.

The original "14 errors under reverse order" was measured while seven code-review subagents were
running Maven concurrently against the same `target/`. Their own reports record the interference
("target/ was cleaned mid-session (not by me)", "surefire cannot create temp files"). Concurrent
`mvn clean` on a shared build directory produces exactly the observed `NoClassDefFoundError`
signature, which was misread as a poisoned static initialiser.

A tell was visible and not chased at the time: the default-order control run in that same command
printed no summary line at all.

The ticket is updated to `[NOT REPRODUCIBLE]`. The underlying smell — a JVM-wide singleton mutated
by tests without restore — is real but latent, and was overstated as a blocker.

### Files changed

| File | Change |
|---|---|
| `files/OutputWriterThreadTest.java` | sink now buffers; `written()` no longer flushes; one test added |

### The non-obvious part

The first attempt — switching the `PrintStream` to autoflush off and dropping the flush from
`written()` — **did not work**; the mutation still survived. `PrintStream` flushes its char buffer
down to the underlying stream on every `println` regardless of autoflush, and on a bare
`ByteArrayOutputStream` the final `flush()` is a no-op because the bytes have already landed.

A `BufferedOutputStream` between the two is what makes the sink genuinely withhold bytes. Worth
recording, because "turn autoflush off" is the obvious fix and it is insufficient.

### Verification

- Suite: **97 passing** (96 + the new test).
- Red-check: deleting `OutputWriterThread.java:162` fails 5 of 6 tests. Before this change the same
  deletion left all 5 green.

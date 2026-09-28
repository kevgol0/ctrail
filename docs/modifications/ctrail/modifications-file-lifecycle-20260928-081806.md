# Modifications: phase 4 — file lifecycle (CTRAIL-5, -9, -11, -13, -15)

**Date:** 2026-09-28
**Branch:** `CTRAIL-5/file-lifecycle` from `main` @ `4fd6a7d`
**Version:** 1.2.2 → **1.2.3**
**Plan:** [plan-fix-code-review-bugs-20260927-235255.md](../../plans/ctrail/plan-fix-code-review-bugs-20260927-235255.md) — phase 4

---

## Concise View

Five defects, all concerning how long a tracker lives and what happens when its file changes under
it. 112 tests. No stacking this time — branched straight from `main`.

[→ Detailed View](#detailed-view)

---

## Detailed View

### CTRAIL-5 — rotation, announced

Per the decision taken when planning: **reseek, but not silently.** `handleRotation` detects
`length < _lastReadPosition`, resets to 0 and emits `rotated, following new file`. The reader calls
it before measuring remaining bytes.

⚠️ **Rename-and-create is out of scope** and stated as such in the Javadoc — the open
`RandomAccessFile` keeps the old inode, so following that needs a reopen by path, which is a
feature.

### CTRAIL-13 — the scan had two bugs, not one

The first attempt (treat `\r` as a break, pair `\r\n` by looking back one byte) **failed both
tests**:

1. the EOF probe only skipped a trailing `\n`, so a file ending in `\r` counted its final
   terminator as a line start
2. pairing by decrementing the cursor *before* computing the result put the position on the `\n`
   itself, producing an empty first line

Rewritten: the EOF probe steps over `\r`, `\n` and `\r\n`, and the loop carries a `previousByte`
across chunk boundaries so a `\r` counts as a break only when it is not the first half of a `\r\n`
already counted. That also fixes the case where the pair straddles the 8 KB window, which the
look-back form could not see.

### CTRAIL-11 — refusing to exist beats existing broken

The constructor now closes and throws `IllegalStateException` rather than logging and continuing.
A tracker whose pointer and recorded position disagree is worse than no tracker.

### CTRAIL-9 / CTRAIL-15 — handles

The `IOException` handler closes the file, marks the `ActivityState` finished so the monitor stops
announcing it, and prints to stdout. `shutdown()` closes every tracker. `getFilesFromArgs` wraps
construction so a throw cannot orphan the handle.

### Verification

- Suite: **112 passing**, including the 9 pre-existing seek tests unchanged.
- End-to-end rotation against the packaged jar:

```
rot.log:line 40
ctrail: rot.log - rotated, following new file
rot.log:AFTER-ROTATION line
ctrail: rot.log - no movement in 2s
```

  Before: `no movement in 2s / 4s` forever and the new line never appeared.

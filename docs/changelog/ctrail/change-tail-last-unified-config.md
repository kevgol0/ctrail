# Change: unified `<tailLast>` setting (CTRAIL-8) — 1.4.0

## Summary

`tailLastLines` and `skipAheadInBytes` collapse into `<tailLast><count/><unit/></tailLast>`. The
old keys are deprecated aliases with their legacy meaning, so `skipAheadInBytes` can no longer be
silently unreachable. New `-c/--bytes`. `count=0` means start at the end; `count=all` means the
whole file.

## Code

```java
// CtrailProps: replaces _tailLastLines / _skipAheadInBytes
@Getter private int _tailLastCount = DEFAULT_TAIL_LAST_COUNT;   // 10
@Getter private TailUnit _tailLastUnit = TailUnit.LINES;
@Getter @Setter private boolean _readEntireFile = false;        // -e, count=all, skipAheadInBytes=0

// initTailLast(): explicit key → applyTailLast(); else aliases → applyLegacyTailKeys(); else default.
// Values are read as strings and parsed locally, so a bad value cannot abort config loading.
```

```java
// FileTailTracker.positionForTailLast()
if (props.isReadEntireFile())                         seekTo(0);
else if (unit == LINES && count > 0)                  seekToLastNLines(count);
else  /* N bytes, or the end for 0 */                 seekTo(unit == BYTES ? max(0, length - count) : length);
```

## Tests

| Test class | Covers |
|---|---|
| `TailLastPropsTest` (13) | explicit beats aliases, `all`, bad unit, bad count (and loading continues), default, `0`, negative, each legacy mapping, unit parsing |
| `FileTailTrackerPositionTest` (6) | N lines, N bytes, bytes > file, 0 lines, 0 bytes, whole file; pointer and recorded position agree |
| `CtrailCliOptionsTest` (+3, 1 rewritten) | `-c`, `-n` beats `-c`, `-n 0` accepted, `-e` beats `-n` and `-c` |
| `LivenessPropsTest` (updated) | `ctrail-file-regex.xml` (skipAheadInBytes only) now gets 1000 bytes, not 10 lines — the CTRAIL-8 fix |

- Full suite: **147 passed, 0 failed** (random run order).
- 9 mutations of the new code, each turned at least one test red.
- Smoke (packaged jar, `-DCTRAIL_CFG`): shipped config → 10 lines, no WARN; legacy
  `skipAheadInBytes=40` only → deprecation WARN + last 40 bytes; `-c 16` → 2 rows; `-n 3 -c 16` →
  WARN + 3 rows; `-n 0` → 0 rows; `-e` → 200 rows.

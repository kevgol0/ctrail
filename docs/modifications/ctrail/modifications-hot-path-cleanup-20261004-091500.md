# Modifications — phase 7: hot-path cleanup (CTRAIL-19, CTRAIL-20)

**Branch:** `CTRAIL-19/hot-path-cleanup`
**Version:** 1.4.0 → 1.4.1
**Tests:** 151 passing (was 147)

---

## Concise View

Two contention/allocation findings from the code review, plus one flaky test helper found while
red-checking them.

| Ticket | Change | Red-check |
|---|---|---|
| [CTRAIL-19](#ctrail-19--getinstance-serialised-every-caller) | `CtrailProps.getInstance()` is no longer `synchronized`; lock-free fast path over a `volatile` cache | 2/2 mutations RED |
| [CTRAIL-20](#ctrail-20--the--m-needle-was-folded-per-line) | `-m` needle folded once in the constructor; dead inline match block deleted | 2/2 mutations RED |
| — | [`FileReaderThreadFilterTest` helper](#test-helper--fixed-sleep-replaced-with-a-condition-poll) waited on a 400ms sleep; now waits on the real condition | class time 2.8s → 0.8s |

No behaviour change is intended by any of the three. All 151 tests pass.

---

## Detailed View

### CTRAIL-19 — `getInstance()` serialised every caller

[↑ concise](#concise-view)

**Finding.** `CtrailProps.getInstance()` was `synchronized`. It is called from both reader threads
and from the idle monitor, on paths that run per line and per 250ms tick, but the lock exists only
to guard a one-time build. Every caller queued behind every other for a field read.

**Fix.** Split the method. The fast path reads a `volatile` cached instance and returns it when the
`ctrail.cfg` override is unchanged; only a miss enters `synchronized rebuild(...)`, which re-checks
under the lock.

```java
public static CtrailProps getInstance()
{
    final String cfgOverride = System.getProperty(CTRAIL_CFG_KEY);
    final CtrailProps cached = _instance;
    if (cached != null && StringUtils.equals(cfgOverride, _instanceCfgOverride))
    {
        return cached;
    }
    return rebuild(cfgOverride);
}
```

Both `_instance` and `_instanceCfgOverride` are `private static volatile`. The rebuild-on-override-change
behaviour the tests rely on is preserved — that is what the second mutation below checks.

**Tests.** New `CtrailPropsConcurrencyTest`, 3 tests:

| Test | Asserts |
|---|---|
| `getInstanceMustNotBeSynchronized` | reflection guard — the modifier does not come back |
| `concurrentCallersAllSeeTheSameInstance` | 16 threads × 500 calls, one identity |
| `changingTheOverrideStillRebuilds` | the fast path does not cache across an override change |

**Red-check.**

| Mutation | Result |
|---|---|
| restore `synchronized`, drop the fast path | RED — `getInstanceMustNotBeSynchronized` |
| cache without comparing the override | RED — `changingTheOverrideStillRebuilds` |

### CTRAIL-20 — the `-m` needle was folded per line

[↑ concise](#concise-view)

**Finding.** With `useCaseSensitiveSarch` false (the default), the command-line match ran twice per
line — once inline in the read loop and once inside `shouldEmit` — and each run lowercased both the
line *and* the search term. Three allocations per line where one is needed; the inline copy was also
dead, since `shouldEmit` already rejected the same lines.

**Fix.** Deleted the inline block. Hoisted the fold into the constructor:

```java
_caseSensitive = _props.isLineSearchCaseSensitiveMatching();
_needle = match_ == null ? null : (_caseSensitive ? match_ : match_.toLowerCase(Locale.ROOT));
```

`shouldEmit` now folds only the line. The config cannot change for the life of the thread, so
caching the flag is safe.

**Tests.** Added `testMatchIsCaseInsensitiveByConfigWithAnUppercaseNeedle` — `-m "ALPHA"` against a
lowercase line. The existing test used a *lowercase* needle against an *uppercase* line, so it only
ever exercised folding the line; the needle fold survived mutation until this mirror case was added.

**Red-check.**

| Mutation | Result |
|---|---|
| `shouldEmit` ignores `_needle` | RED — `testMatchIsCaseInsensitiveByConfig` |
| `_needle = match_` (no fold) | RED — `...WithAnUppercaseNeedle` (**green before the new test**) |

### Test helper — fixed sleep replaced with a condition poll

[↑ concise](#concise-view)

**Found while red-checking CTRAIL-20.** `runReaderBriefly` started the reader, slept a flat 400ms,
then interrupted and asserted. On a cold JVM one run of the class took 3.0s wall and the new test
failed *with the fix in place* — the reader had not drained the fixture inside the window, so the
assertion saw zero lines. Re-running it in isolation passed. Every test in the class shared that
helper, so every one of them was a load-dependent coin flip.

**Fix.** `waitUntilDrained` polls two conditions at 20ms: the tracker reports no unread bytes, and
the output queue size has been unchanged for 3 consecutive samples, with a 5s ceiling. Both halves
are needed — `readToFilePosition` records the read position *before* it queues the line, so
"nothing left to read" alone can be one `put()` short of the line being asserted on.

Side effect: the class runs in 0.789s instead of ~2.8s.

**Not claimed:** this removes a *known* flake source in one test class. It is not a sweep of
timing assumptions across the suite.

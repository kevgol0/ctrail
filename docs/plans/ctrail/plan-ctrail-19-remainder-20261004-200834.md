# Plan — CTRAIL-19 remainder: remove the per-line config lookup

**Date:** 2026-10-04
**Base:** `main` @ 1.4.1 (PR #24 merged), 151 tests green
**Target version:** 1.4.2

---

## Concise View

### What is left of CTRAIL-19

PR #24 removed the `CtrailProps.class` monitor. Two things I flagged on the ticket remain:

1. The per-line `System.getProperty` lookup inside `getInstance()`.
2. The corollary — a mid-run rebuild discarding the startup CLI overrides.

[→ detail](#what-is-left-of-ctrail-19-1)

### The finding that reframes both

**Nothing in `src/main` ever calls `System.setProperty(CTRAIL_CFG)`.** Only tests do. And
`CtrailEntryPoint` settles all config — load, then `-e/-f/-v/-n` — *before* it constructs the
writer or any reader.

So the per-line lookup can never return a different answer after startup, and the corollary is
**unreachable in production**. The fix is therefore not to make rebuild preserve overrides; it is to
stop asking per line. Caching `_props` at construction closes the performance half and makes the
correctness half structurally impossible.

[→ detail](#the-finding-that-reframes-both-1)

### Two things I found while reading, which I should have caught earlier

| | |
|---|---|
| **CTRAIL-20 is only half done.** | `StdinReaderThread.shouldEmit` still folds the needle **and** the line on every line — the exact duplicate CTRAIL-20 was about. I fixed the file path and closed the ticket. The ticket's own description predicted this divergence. |
| **`FileSearchFilter` folds every term, every line.** | Not in any ticket. With *T* configured terms it is *T* wasted `toLowerCase` allocations per line, on top of the line fold — a larger cost than the `getInstance()` call this plan is about. |

[→ detail](#two-things-i-found-while-reading-1)

### Proposed phases

| # | Change | Risk |
|---|---|---|
| 1 | `LineFormatter` — cache at construction, delete per-line `refreshProps()` | Low |
| 2 | `StdinReaderThread` — cache `_props`, hoist needle (closes CTRAIL-20's stdin twin) | Low |
| 3 | `FileSearchFilter` — pre-fold term lists, cache the case flag | **Medium — needs your decision** |
| 4 | Red-check every change; full suite | — |
| 5 | 1.4.1 → 1.4.2, changelog, modifications log, PR, Jira | — |

[→ detail](#proposed-phases-1)

### Two decisions I need from you

1. **Is phase 3 in scope?** It is the biggest win but it touches `FileSearchFilter`'s exposed
   mutable term lists, which `CtrailProps` and several tests write to directly.
   **Recommendation: yes, with the conservative design in the detail below.**
2. **Ticket hygiene.** CTRAIL-19 is `Done`. **Recommendation: reopen CTRAIL-19** for phases 1–2 and
   **raise a new ticket** for phase 3 and the stdin gap, so the CTRAIL-20 miss is on the record
   rather than buried in a reopened ticket.

[→ detail](#two-decisions-i-need-from-you-1)

---

## Detailed View

### What is left of CTRAIL-19

[↑ concise](#what-is-left-of-ctrail-19)

The merged fix made the fast path lock-free:

```java
public static CtrailProps getInstance()
{
    final String cfgOverride = System.getProperty(CTRAIL_CFG_KEY);   // still per call
    final CtrailProps cached = _instance;
    if (cached != null && StringUtils.equals(cfgOverride, _instanceCfgOverride))
    {
        return cached;
    }
    return rebuild(cfgOverride);
}
```

What is gone is the monitor. What remains per line is a `System.getProperty` hash lookup, a
`StringUtils.equals`, and a volatile read — at these call sites:

| Call site | Frequency |
|---|---|
| `LineFormatter:62` (`refreshProps`) | once per **output** line |
| `FileSearchFilter:139` | once per input line |
| `FileSearchFilter:184`, `:189` | twice more per input line |
| `StdinReaderThread:188` | once per **stdin** input line |

`FileReaderThread` already caches `_props` in its constructor and is not on this list.

### The finding that reframes both

[↑ concise](#the-finding-that-reframes-both)

Grepping `System.setProperty(CTRAIL_CFG_KEY)` returns **zero hits in `src/main`** and many in
`src/test`. The property is set once on the JVM command line and never changes while ctrail runs.

`CtrailEntryPoint`'s constructor fixes the ordering:

```java
loadProps();                                    // builds the singleton
final String[] remainingArgs = loadArgsAndOverrides(args_);   // applies -e, -f, -v, -n
initConsoleWriterThread();                      // builds LineFormatter
initInputReaderThread(remainingArgs);           // builds the readers
```

Every consumer is constructed after config is final. Two consequences:

- The per-line lookup is pure overhead that cannot change its answer. Caching is not a behaviour
  change in production.
- The corollary ("a rebuild discards `-e/-n/-f/-v`") needs a mid-run property change to fire, which
  no production path performs. It is a test-only hazard.

**I am therefore not proposing override-preservation machinery.** It would add complexity for an
unreachable path, and it would fight the tests that deliberately rely on rebuild semantics
(`CtrailPropsRegressionTest`, `CtrailPropsConcurrencyTest.changingTheOverrideStillRebuilds`). I plan
to record the invariant in a comment and close the corollary as "unreachable by design", not
"fixed". Say so if you would rather it were genuinely fixed.

### Two things I found while reading

[↑ concise](#two-things-i-found-while-reading-which-i-should-have-caught-earlier)

**CTRAIL-20's stdin twin.** `StdinReaderThread.shouldEmit`:

```java
if (_match != null)
{
    final boolean caseSensitive = CtrailProps.getInstance().isLineSearchCaseSensitiveMatching();
    final String needle = caseSensitive ? _match : _match.toLowerCase(Locale.ROOT);
    final String haystack = caseSensitive ? line_ : line_.toLowerCase(Locale.ROOT);
    ...
```

Three allocations plus a config lookup per line — byte-for-byte the pattern CTRAIL-20 removed from
`FileReaderThread`. I fixed one of the two paths and marked the ticket Done. The ticket text warned
about exactly this: *"applied to one copy and not the other would silently diverge the file path
from the stdin path."* That is now the state of the code.

**`FileSearchFilter` per-term folding.** Both `shouldIncludeLineDueToSeachTerms` and
`shouldExcludeLineDueToSeachTerms` fold each term inside the per-line loop:

```java
for (int i = 0; i < getIncludeTerms().size(); i++)
{
    final String includeTerm = getIncludeTerms().get(i);
    final String normalizedTerm = caseSensitive ? includeTerm : includeTerm.toLowerCase(Locale.ROOT);
```

The terms are constant; only the line varies. With the shipped `etc/ctrail.xml` that is 2–3 wasted
allocations per line per filter. This is not in any ticket and is a larger cost than the
`getInstance()` call — which is why I want it in scope rather than noted and dropped.

### Proposed phases

[↑ concise](#proposed-phases)

#### Phase 1 — `LineFormatter`

Cache in the constructor; delete `refreshProps()` and its per-line call from `format()`. Replace the
Javadoc, which currently claims the reference compare is what keeps the method cheap — it optimises
the cheap half and leaves the lookup running.

*Verification concern, already checked:* every test builds a fresh `LineFormatter` **after** setting
`CTRAIL_CFG` (`CaseSensitiveTest:37→40`, `IgnoreCaseTest:41→44`, `LastWordMatchTest:40→44`,
`NoticeFormattingTest:53→56`). None swaps config on a live formatter, so none depends on the
per-line refresh.

#### Phase 2 — `StdinReaderThread`

Cache `_props`; hoist `_needle` and `_caseSensitive` into the constructor exactly as
`FileReaderThread` now does. This is a copy of the merged CTRAIL-20 fix onto the path I missed.

#### Phase 3 — `FileSearchFilter`

The constraint: `FileSearchFilter` is constructed *inside* `CtrailProps` parsing
(`CtrailProps:697/795/837`), so it cannot call `getInstance()` in its own constructor — the
singleton is not published yet. Its term lists are then populated through the exposed mutable list
(`loadFilterTerms`, `CtrailProps:745/758`), and tests add terms the same way
(`filter.getIncludeTerms().add("keep")`). Pre-folding at construction time would therefore fold an
empty list.

**Conservative design — fold lazily, invalidate on size change:**

- Keep `getIncludeTerms()` / `getExcldueTerms()` exactly as they are, so no caller changes.
- Hold `_foldedIncludes` / `_foldedExcludes` arrays plus the list size they were built from.
- On entry to each matcher, rebuild a folded array only when the recorded size differs from the
  live list size. Steady state is one comparison per line, zero allocations.
- Cache `caseSensitive` alongside and rebuild the folded arrays if it flips.

This keeps the public surface identical and does not require finding every writer. Its weakness is
honest: it detects *size* changes, not in-place replacement of a term. Nothing in the codebase
replaces a term in place, and I will assert that in a test.

*Rejected alternative:* `addIncludeTerm()` / `addExcludeTerm()` methods with folding at insert.
Cleaner, but it means changing `CtrailProps.loadFilterTerms` and every test that appends to the
list — a wider blast radius than this ticket justifies.

`isEnabledExcludeFiltering()` **stays** a live read. It is genuinely mutable via `-v` and the cost
is one lookup, not a per-term loop.

#### Phase 4 — verification

Red-check each change by deleting the line it covers and watching the test go red:

| Change | Mutation to confirm |
|---|---|
| `LineFormatter` caches | construct with config A, format, assert A's colors |
| stdin needle hoisted | `_needle = _match` (no fold) → uppercase `-m` vs lowercase line |
| stdin props cached | existing stdin tests must stay green |
| folded term arrays | flip `caseSensitive` handling → mixed-case term test |
| folded arrays invalidate | add a term after first use → must be honoured |

The last one matters most: it is the failure mode the lazy design introduces, so it gets a test
before the design ships.

New test classes: `LineFormatterCachingTest`, `FileSearchFilterFoldingTest`. Extend
`StdinReaderThreadTest` for the needle case rather than a new class.

#### Phase 5 — release

1.4.1 → 1.4.2; `CHANGELOG.md` section; `project.instructions.md` version line; modifications log at
`docs/modifications/ctrail/`; branch off `main`; PR; Jira comments, remote links and transitions
per phase.

### Two decisions I need from you

[↑ concise](#two-decisions-i-need-from-you)

**1. Phase 3 in or out.** In: the largest per-line win in this plan, at the cost of a lazily
invalidated cache whose weakness I have described. Out: phases 1–2 are low-risk and still close
CTRAIL-19, and I raise phase 3 as its own ticket. *Recommendation: in.*

**2. Ticket hygiene.** CTRAIL-19 and CTRAIL-20 are both `Done`, and the code does not match that for
either. *Recommendation:* reopen CTRAIL-19 → In Progress for phases 1–2; raise one new ticket
covering the `FileSearchFilter` folding and the `StdinReaderThread` half of CTRAIL-20, linked to
CTRAIL-20, stating plainly that CTRAIL-20 was closed with one of its two paths unfixed.

---

**Nothing is edited until you say go.**

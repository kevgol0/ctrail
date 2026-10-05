# Modifications — CTRAIL-19 remainder and CTRAIL-21

**Branch:** `CTRAIL-19/per-line-config-lookup`
**Version:** 1.4.1 → 1.4.2
**Tests:** 161 passing (was 151); 4 of 4 mutations red

---

## Concise View

Removes the last per-line config reads, and the per-line term folding found while reading for them.

| Ticket | Change | Red-check |
|---|---|---|
| [CTRAIL-19](#ctrail-19--config-captured-at-construction) | `LineFormatter` and `StdinReaderThread` capture config at construction | 1/1 |
| [CTRAIL-21](#ctrail-21--terms-folded-once) | `FileSearchFilter` folds its terms once and caches them | 2/2 |
| [CTRAIL-21](#ctrail-21--the-stdin-half-of-ctrail-20) | `StdinReaderThread` needle hoisted — the half of CTRAIL-20 left unfixed | 1/1 |

The corollary on CTRAIL-19 — a mid-run rebuild discarding CLI overrides — is **closed as unreachable,
not fixed**. See [why](#the-corollary-is-unreachable-not-fixed).

---

## Detailed View

### CTRAIL-19 — config captured at construction

[↑ concise](#concise-view)

**Finding.** After 1.4.1 removed the lock, `getInstance()` still ran a `System.getProperty` lookup,
a `StringUtils.equals` and a volatile read on every line, at `LineFormatter:62` (per output line)
and `StdinReaderThread:188` (per stdin input line).

**What makes caching safe.** `CtrailEntryPoint`'s constructor fixes the ordering:

```java
loadProps();                                                  // builds the singleton
final String[] remainingArgs = loadArgsAndOverrides(args_);   // applies -e, -f, -v, -n
initConsoleWriterThread();                                    // builds LineFormatter
initInputReaderThread(remainingArgs);                         // builds the readers
```

Every consumer is constructed after config is final, and **nothing in `src/main` ever calls
`System.setProperty(CTRAIL_CFG)`** — only tests do. So the per-line lookup could not return a
different answer.

**Change.** `LineFormatter`'s fields are now `final`, set in the constructor; `refreshProps()` and
its per-line call are deleted. `StdinReaderThread` holds `_props` and uses it for the charset, the
source name, the idle interval and the match.

**Tests.** New `LineFormatterCachingTest` (3): config captured at construction, a formatter built
after a swap does see the new config (which makes the first a statement about caching rather than
about fixtures), and a reflection guard that `refreshProps` has not returned.

### CTRAIL-21 — terms folded once

[↑ concise](#concise-view)

**Finding.** Both `FileSearchFilter` matchers folded every configured term *inside* the per-line
loop (`:146`, `:193`). Terms are constant for the life of a run; only the line varies.

**Constraint.** `FileSearchFilter` is constructed *inside* `CtrailProps` parsing
(`CtrailProps:697/795/837`), so it cannot call `getInstance()` in its own constructor — the
singleton is not published yet. Terms then arrive through the exposed mutable list
(`loadFilterTerms`, and tests). Folding at construction would fold an empty list.

**Design.** A `FoldedTerms` holder carries the folded array, the source list size it was built from,
and the case-sensitivity verdict it was built under. It is published through a single `volatile`
reference write, so a reader thread cannot see the array and its provenance disagree. A matcher
rebuilds only when the live list size or the verdict differs.

**Known weakness, tested rather than hidden.** Size is the staleness signal, so an in-place
*replacement* of a term would not be detected. Nothing in the codebase replaces a term in place.
`aTermAddedAfterTheFirstMatchIsStillHonored` pins the case that does occur.

`isEnabledExcludeFiltering()` deliberately stays a live read — `-v` genuinely mutates it, and it
costs one lookup, not a per-term loop.

### CTRAIL-21 — the stdin half of CTRAIL-20

[↑ concise](#concise-view)

CTRAIL-20 removed the per-line needle fold from `FileReaderThread` and was closed. `StdinReaderThread`
kept it. The ticket's own description had predicted exactly this: *"applied to one copy and not the
other would silently diverge the file path from the stdin path."*

The stdin path now folds the needle in the constructor, as the file path does. It ships with **both**
tests — uppercase needle and uppercase line. The file-side fix originally shipped with only the
second, which is why its needle fold survived mutation until the mirror was added.

### The corollary is unreachable, not fixed

[↑ concise](#concise-view)

CTRAIL-19 noted that a mid-run rebuild discards the startup CLI overrides. Triggering it needs a
mid-run `CTRAIL_CFG` change, which no production path performs. Building override-preservation would
add machinery for an unreachable path and would fight the tests that rely on rebuild semantics
(`CtrailPropsRegressionTest`, `CtrailPropsConcurrencyTest.changingTheOverrideStillRebuilds`).

Capturing `_props` per thread removes the hazard structurally instead: a thread cannot have its
config swapped mid-run because it no longer asks. **This is a narrowing of the exposure, not a fix
of the rebuild semantics**, and CTRAIL-19's comment thread says so.

### Verification

| Mutation | Result |
|---|---|
| `_needle = match_` in `StdinReaderThread` | RED — `testMatchIsCaseInsensitiveWithAnUppercaseNeedle` |
| fold cache ignores list size | RED — `aTermAddedAfterTheFirstMatchIsStillHonored` |
| `folded[i] = term` (no fold) | RED — `aMixedCaseTermMatchesCaseInsensitively` |
| notice color re-read per line | RED — `configIsCapturedAtConstructionNotPerLine` |

161 tests pass from a clean build.

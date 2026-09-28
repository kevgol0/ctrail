# Modifications: phase 2 — CTRAIL-2 closed as working-as-designed

**Date:** 2026-09-28
**Branch:** `CTRAIL-2/exclude-only-filter` from `main` @ `653f6cf`
**Jira:** [CTRAIL-2](https://verame.atlassian.net/browse/CTRAIL-2)
**Plan:** [plan-fix-code-review-bugs-20260927-235255.md](../../plans/ctrail/plan-fix-code-review-bugs-20260927-235255.md) — phase 2
**Changelog:** [change-filter-default-semantics.md](../../changelog/ctrail/change-filter-default-semantics.md)

---

## Concise View

**No production behaviour changed.** CTRAIL-2 reproduces but is deliberate; the work became
documentation plus tests that pin the contract.

[→ Detailed View](#detailed-view)

---

## Detailed View

### What happened

The fix was written first: `shouldIncludeLineDueToSeachTerms` returning `true` when the include
list is empty. It worked, and its red-check was clean — but it broke
`FileSearchFilterTest.testDefaultIncludeBehaviour`, a pre-existing test whose docstring states the
opposite intent:

> With no include terms configured, the fileFilterDefaultsToInclude setting decides.

That is a conflict of intent, not a bug. Rewriting that test to make the new fix pass would have
been overriding a documented decision, so the work stopped and the question went back to the user.
**Option B was chosen: revert, close as working-as-designed, document.**

### Argument that was made and not taken

`fileFilterDefaultsToInclude` is global, so under the current semantics a config cannot contain both
a strict allow-list filter and a deny-list filter. Per-filter semantics would fix that. Recorded
here and in the changelog as a possible future feature; deliberately not built.

### Files changed

| File | Change |
|---|---|
| `props/FileSearchFilter.java` | comment only — explains the empty-includes case at the decision point and names the test that pins it |
| `README.md` | "Filters: allow-list or deny-list, not both" with a truth table and the global-flag caveat |
| `etc/ctrail.xml` | expanded `fileFilterDefaultsToInclude` comment |
| `props/FilterDefaultSemanticsTest.java` | new — 4 tests |
| `configs/ctrail-filter-default-semantics.xml` | new fixture |

### A fixture bug worth noting

The first fixture wrote `<filename>excludes-only\.log</filename>`, pre-escaping the dot.
`FileSearchFilter` runs the name through `toRegEx` itself, producing `excludes-only\\\.log$`, which
matched nothing. Filenames in `<filefilter>` are plain, not regex-escaped by the author.

### Verification

- Suite: **100 passing**.
- `testDefaultIncludeBehaviour` untouched and green.
- The reverted fix had a clean red-check before reverting: removing the empty-include guard failed
  exactly `excludesOnlyFilterKeepsEverythingItDoesNotExclude` and nothing else. Recorded because it
  confirms the two semantics really are mutually exclusive.

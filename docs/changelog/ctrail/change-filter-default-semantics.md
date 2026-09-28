# Docs: filter defaults are allow-list or deny-list, not both (CTRAIL-2)

**Changed:** 2026-09-28 — `CTRAIL-2/exclude-only-filter`

## Outcome: working as designed, documented rather than changed

CTRAIL-2 reported that an excludes-only `<filefilter>` emits **zero lines** under the shipped
config. That reproduces, but it is **deliberate**, not a defect — and a pre-existing test says so:

```java
/**
 * With no include terms configured, the fileFilterDefaultsToInclude setting
 * decides.
 */
@Test
public void testDefaultIncludeBehaviour()
{
    assertTrue (new FileSearchFilter("d.log", true ).shouldIncludeLineDueToSeachTerms("anything"));
    assertFalse(new FileSearchFilter("d.log", false).shouldIncludeLineDueToSeachTerms("anything"));
}
```

A fix was written and then **reverted**: making an empty `<includes>` mean "no constraint" would
have required rewriting that test, i.e. overriding a documented intent to suit the change.

## What the contract actually is

`fileFilterDefaultsToInclude` decides for a line matching neither list — **including** when the
include list is empty.

| `fileFilterDefaultsToInclude` | filter with `<includes>` | filter with only `<excludes>` |
|---|---|---|
| `false` (shipped default) | strict allow-list | **emits nothing** |
| `true` | includes win, rest shown | deny-list |

To express *"hide DEBUG, show everything else"*:

```xml
<fileFilterDefaultsToInclude>true</fileFilterDefaultsToInclude>
<filefilter>
  <filename>app.log</filename>
  <excludes><keyword>DEBUG</keyword></excludes>
</filefilter>
```

## ⚠️ The real limitation, now written down

The setting is **global**. A single config cannot hold both a strict allow-list filter and a
deny-list filter — every filter shares the same default. That is the genuine constraint behind the
original report, and it is now stated in the README rather than discovered by surprise.

Making it per-filter (e.g. a `defaultsToInclude` attribute on `<filefilter>`) would remove the
limitation, but that is a feature, not a bug fix, and is not done here.

## Changes

| File | Change |
|---|---|
| `props/FileSearchFilter.java` | comment at the decision point explaining the empty-includes case and pointing at the test that pins it |
| `README.md` | new "Filters: allow-list or deny-list, not both" section with the truth table |
| `etc/ctrail.xml` | expanded comment on `fileFilterDefaultsToInclude` |
| `props/FilterDefaultSemanticsTest.java` | new — 4 tests pinning the behaviour so CTRAIL-2 is not re-filed |
| `configs/ctrail-filter-default-semantics.xml` | new fixture covering both filter shapes |

No production behaviour changed.

## Test results

```
Tests run: 100, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

`testDefaultIncludeBehaviour` is untouched and still passes.

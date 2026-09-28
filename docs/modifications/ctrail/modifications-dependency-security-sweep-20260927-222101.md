# Modifications: dependency security sweep

**Date:** 2026-09-27
**Branch:** `chore/dependency-security-sweep` from `main` @ `eda6a8c`
**Plan:** [plan-dependency-security-sweep-20260927-221853.md](../../plans/ctrail/plan-dependency-security-sweep-20260927-221853.md)
**Changelog:** [change-dependency-security-sweep.md](../../changelog/ctrail/change-dependency-security-sweep.md)

---

## Concise View

Five version bumps in `pom.xml`. **No source changes.** Clears 7 of 8 Dependabot alerts — the
CRITICAL and all three HIGHs among them.

| Package | From | To |
|---|---|---|
| `commons-configuration2` | 2.7 | 2.10.1 |
| `logback-classic` | 1.2.3 | 1.2.13 |
| `commons-beanutils` | 1.9.4 | 1.11.0 |
| `commons-io` | 2.7 | 2.14.0 |
| `commons-lang3` | 3.12.0 | 3.18.0 |

Replaces Dependabot PRs #5 and #8, which together cleared only 2 of 8.

⚠️ **CVE-2026-45205 stays open** — it needs commons-configuration2 2.15.0, which fails with
`NoSuchMethodError` even with every other dependency bumped. Root cause not diagnosed; left for its
own investigation rather than guessed at.

[→ Detailed View](#detailed-view)

---

## Detailed View

### Files changed

| File | Change |
|---|---|
| `pom.xml` | five `<version>` values |
| `docs/plans/ctrail/plan-dependency-security-sweep-20260927-221853.md` | new |
| `docs/changelog/ctrail/change-dependency-security-sweep.md` | new |

### Verification

**Suite:** `Tests run: 94, Failures: 0, Errors: 0, Skipped: 0` — unchanged from before the bumps.

**Version selection was empirical, not from release notes.** Nine `mvn clean test` runs mapped the
safe ceiling:

- config2 2.8.0, 2.9.0, 2.10.1 → all PASS
- config2 **2.11.0** → FAIL, `NoClassDefFoundError: org/apache/commons/lang3/SystemProperties`
  (needs lang3 ≥ 3.13)
- config2 **2.15.0** with every other dep bumped → FAIL, `NoSuchMethodError` in
  commons-configuration, 74 errors

**Java 8 bytecode check.** The suite runs on Corretto 21 while the pom targets 1.8, so a green
suite alone proves nothing for a Java 8 consumer. Class-file major version of each upgraded jar:

```
commons-configuration2   2.10.1   major=52  OK Java 8
logback-classic          1.2.13   major=50  OK Java 8
commons-beanutils        1.11.0   major=52  OK Java 8
commons-io               2.14.0   major=52  OK Java 8
commons-lang3            3.18.0   major=52  OK Java 8
```

**Smoke tests**, packaged jar, output identical to pre-bump:

- file mode: banner, tail-5, idle at 2s and 4s, resume, re-idle
- stdin mode: banner, idle ×2, resume
- plain pipe, no `-m`, no filter: alpha/bravo/charlie — every line including the last, which guards
  the 1.1.1 shutdown fix

### Sequencing on the Dependabot PRs

#5 and #8 are **left open until this merges**. Closing them first would leave a window where
nothing tracks those two vulnerabilities. Once this lands their bumps are subsumed and they can be
closed pointing here.

# Security: dependency sweep — 7 of 8 Dependabot alerts cleared

**Changed:** 2026-09-27 — `chore/dependency-security-sweep`

## Summary

Five dependency bumps clearing **7 of 8** open Dependabot alerts, including the CRITICAL and all
three HIGHs. No source changes.

| Package | From | To | Clears |
|---|---|---|---|
| `commons-configuration2` | 2.7 | 2.10.1 | CVE-2022-33980 (**CRITICAL**), CVE-2024-29131, CVE-2024-29133 |
| `logback-classic` | 1.2.3 | 1.2.13 | CVE-2023-6378 (**HIGH**) |
| `commons-beanutils` | 1.9.4 | 1.11.0 | CVE-2025-48734 (**HIGH**) |
| `commons-io` | 2.7 | 2.14.0 | CVE-2024-47554 (**HIGH**) |
| `commons-lang3` | 3.12.0 | 3.18.0 | CVE-2025-48924 (MEDIUM) |

## Why not the two Dependabot PRs

[#5](https://github.com/kevgol0/ctrail/pull/5) (commons-configuration2 → 2.8.0) and
[#8](https://github.com/kevgol0/ctrail/pull/8) (logback → 1.2.13) both pass — verified against
current `main`, not their 2022/2023 bases — but between them they clear only **2 of 8** alerts. The
other six are in packages Dependabot never opened a PR for. One tested commit covers all of it and
goes further on commons-configuration2 (2.10.1 rather than 2.8.0, picking up two more CVEs).

## :warning: One alert remains open

**CVE-2026-45205** (MEDIUM, commons-configuration2) needs **2.15.0**, which breaks this codebase.
Not shipped, and not worked around:

- `2.11.0` → `NoClassDefFoundError: org/apache/commons/lang3/SystemProperties` (needs lang3 ≥ 3.13)
- `2.15.0`, even with every other dependency bumped to the versions above →
  `NoSuchMethodError` inside commons-configuration, 74 test errors

**The root cause is not diagnosed.** Likely a newer commons-beanutils or commons-text expectation,
but that is a guess. It needs its own investigation rather than a version nudge.

## How the versions were chosen

Every version here came from an actual `mvn clean test` run against `main`, not from release notes:

| Combination | Result |
|---|---|
| baseline | PASS — 94 tests |
| logback 1.2.13 alone | PASS — 94 |
| config2 2.8.0 alone (PR #5) | PASS — 94 |
| logback 1.2.13 + config2 2.8.0 (both PRs) | PASS — 94 |
| config2 2.9.0 alone | PASS — 94 |
| config2 2.10.1 alone | PASS — 94 |
| config2 2.11.0 alone | **FAIL** — 74 errors, `NoClassDefFoundError` |
| config2 2.15.0 + all deps bumped | **FAIL** — 74 errors, `NoSuchMethodError` |
| **this change** | **PASS — 94** |

## Java 8 compatibility

The suite runs on Corretto 21 while the pom targets Java 8, so a green suite alone would not prove
a Java 8 consumer still works. Checked the class-file major version of every upgraded jar
(52 = Java 8, 55 = Java 11):

```
commons-configuration2   2.10.1   major=52  OK Java 8
logback-classic          1.2.13   major=50  OK Java 8
commons-beanutils        1.11.0   major=52  OK Java 8
commons-io               2.14.0   major=52  OK Java 8
commons-lang3            3.18.0   major=52  OK Java 8
```

Nothing requires Java 9+. Whether Java 8 is still a target worth holding is a separate question.

## Test results

```
Tests run: 94, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Smoke-tested against the packaged jar, output byte-identical to pre-bump in all three cases — the
liveness features lean on commons-configuration2 and commons-io, which is where a silent
behavioural regression would show:

- file mode: banner, tail-N, idle ×2, resume, re-idle
- stdin mode: banner, idle ×2, resume
- plain pipe with no `-m` and no filter: every line including the last (guards the 1.1.1 fix)

## Untouched

`commons-cli`, `slf4j-api`, `jcl-over-slf4j`, `lombok`, `junit` — no alerts against them, so
bumping would add risk for no security gain.

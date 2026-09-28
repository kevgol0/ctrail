# Plan: Dependency security sweep — clear 7 of 8 Dependabot alerts

**Date:** 2026-09-27
**Status:** `Approved ("follow the recommendation")` — 2026-09-27
**Branch:** `chore/dependency-security-sweep` from `main` @ `eda6a8c`
**Supersedes:** Dependabot PRs [#5](https://github.com/kevgol0/ctrail/pull/5) and [#8](https://github.com/kevgol0/ctrail/pull/8)
**Jira / Confluence:** _n/a — personal tool repo_

---

## Concise View

### Why not just merge the two Dependabot PRs

They pass (verified against current `main`, not their 2022/2023 bases) but clear only **2 of 8**
alerts. The other six are in packages Dependabot never opened a PR for. One tested commit beats
two stale branches that leave six alerts open.

### The bumps

| Package | From | To | Clears |
|---|---|---|---|
| `commons-configuration2` | 2.7 | **2.10.1** | CVE-2022-33980 (CRITICAL), CVE-2024-29131, CVE-2024-29133 |
| `logback-classic` | 1.2.3 | **1.2.13** | CVE-2023-6378 (HIGH) |
| `commons-beanutils` | 1.9.4 | **1.11.0** | CVE-2025-48734 (HIGH) |
| `commons-io` | 2.7 | **2.14.0** | CVE-2024-47554 (HIGH) |
| `commons-lang3` | 3.12.0 | **3.18.0** | CVE-2025-48924 (MEDIUM) |

**7 of 8 cleared**, including the CRITICAL and all three HIGHs. This exact combination was already
run green: 94 tests, 0 failures.

### ⚠️ The one left open, and why

**CVE-2026-45205** (MEDIUM, commons-configuration2) needs **2.15.0**, which breaks this codebase:

- `2.11.0` → `NoClassDefFoundError: org/apache/commons/lang3/SystemProperties` (needs lang3 ≥ 3.13)
- `2.15.0`, even with every other dependency bumped → `NoSuchMethodError` inside
  commons-configuration. **Root cause not diagnosed.** Probably a newer commons-beanutils or
  commons-text expectation, but that is a guess and is not acted on here.

Deliberately out of scope. It needs its own investigation, not a version nudge.

### ⚠️ Caveat this plan does not resolve

Tests run on **Corretto 21** while the pom targets **Java 8**. A dependency shipping Java 11
bytecode would pass here and still break a real Java 8 consumer. This plan **adds a bytecode check**
on the upgraded jars to close that gap empirically, but whether Java 8 is still a real target is a
question for the user, not something to assume.

[→ Detailed View](#detailed-view)

---

## Detailed View

### Evidence behind the version choices

Every number below came from an actual `mvn clean test` run against current `main`, not from
release notes:

| Combination | Result |
|---|---|
| baseline (as-is) | PASS — 94 tests |
| logback 1.2.13 alone | PASS — 94 |
| config2 2.8.0 alone | PASS — 94 |
| logback 1.2.13 + config2 2.8.0 (= both PRs) | PASS — 94 |
| config2 2.9.0 alone | PASS — 94 |
| config2 2.10.1 alone | PASS — 94 |
| config2 2.11.0 alone | **FAIL** — 74 errors, `NoClassDefFoundError: lang3/SystemProperties` |
| config2 2.15.0 + all deps bumped | **FAIL** — 74 errors, `NoSuchMethodError` in commons-configuration |
| **config2 2.10.1 + logback 1.2.13 + beanutils 1.11.0 + io 2.14.0 + lang3 3.18.0** | **PASS — 94** |

The last row is what this plan ships.

### Scope

| # | Change | File |
|---|---|---|
| 1 | Five version bumps | `pom.xml` |
| 2 | Bytecode-target check on the upgraded jars | verification step, no file |
| 3 | Changelog | `docs/changelog/ctrail/change-dependency-security-sweep.md` |
| 4 | Modifications log | `docs/modifications/ctrail/` |

No source changes. No behaviour changes intended — and if the suite disagrees, that is the signal
to stop.

### Out of scope

- **CVE-2026-45205 / commons-configuration2 2.15.0** — see above.
- Raising the repo off Java 8 / JUnit 4.
- The `commons-cli`, `slf4j-api`, `jcl-over-slf4j`, `lombok` and `junit` pins — no alerts against
  them; bumping them would add risk for no security gain.

### Sequencing note on the Dependabot PRs

#5 and #8 are **left open until this lands**. Closing them first would leave a window where
nothing tracks those two vulnerabilities. Once this merges their bumps are subsumed and they can be
closed with a pointer to the replacement.

## Tests

No new tests — this is a dependency change, and the existing 94 are the regression net. The suite
must stay at **94 passing, 0 failures**; any change in that number stops the work.

Additional verification beyond the suite:

1. **Bytecode target** of each upgraded jar (major 52 = Java 8, 55 = Java 11), to check the
   Corretto-21-vs-target-8 gap.
2. **Smoke test** the packaged jar in both modes — the liveness features exercise
   commons-configuration2 and commons-io heavily, which is where a silent behavioural regression
   would surface.

## Build / verify

1. `mvn clean test` — expect 94 passing.
2. Bytecode check on the five upgraded jars.
3. `mvn clean package`, then smoke test file mode and stdin mode.
4. Re-check the alert list to confirm 7 closed, 1 remaining.

## Definition of Done

- [ ] Five bumps applied, no source changes.
- [ ] Full suite green at 94.
- [ ] Bytecode target recorded for every upgraded jar.
- [ ] Smoke test passes in both modes.
- [ ] Changelog and modifications log written, naming the CVEs cleared and the one left.
- [ ] PR opened; #5 and #8 left open until it merges.

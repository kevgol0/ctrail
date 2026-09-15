# Modifications: Rewrite project instructions as ctrail-specific

- **Date:** 20260914-233400
- **Project:** ctrail
- **Plan:** docs/plans/ctrail/plan-de-dakota-local-skills-20260914-230452.md (follow-up item)
- **Confluence:** _not published — see "Publication" below_
- **Branch:** master
- **File:** `.github/instructions/project.instructions.md` (72 lines -> 256 lines)

## Summary

`.github/instructions/project.instructions.md` was a copy of the Verame.Dakota working
agreement and described a project that does not exist here. It has been rewritten to
describe **ctrail specifically**, while cross-project rules stay in `~/.claude/CLAUDE.md`
and the generalized skills in `~/.claude/skills/`. The file states explicitly that it wins
over the global rules where they disagree.

## What was wrong

| Old (Dakota) | Reality for ctrail |
|---|---|
| "JUnit 5 with Mockito", "RestAssured test per endpoint" | **JUnit 4**; no Mockito, no RestAssured, **no endpoints** — it is a CLI |
| `dakota-infrastructure-builder/scripts/create-prs.sh` batch PR script | Single repo; no such script |
| "Verame.Dakota parent repo containing 9 nested git repos" | One repo, no nesting |
| "`gh` CLI is configured for GHE at verame.ghe.com" | ctrail is on **github.com** — see the `gh` finding below |
| "check for known Micronaut/YAML breaking changes" | No Micronaut, no YAML; config is **XML via commons-configuration2** |
| "update these instructions in `.junie/guidelines.md`" | That path does not exist; updates go in this file |
| `PROJECT` placeholder throughout | `PROJECT` = `ctrail` |

## What the file now contains

- **What ctrail is** — Java 8, Maven + shade plugin, entry point
  `com.kagr.tools.ctrail.CtrailEntryPoint`, artifact `target/ctrail-<version>.jar`, version
  `1.1.1`, single repo, default branch **`master`** (there is no `main`).
- **Dependency list and full source-layout map** (`files/`, `props/`, `unit/`) with a
  one-line description of each class.
- **Config resolution order** (`-DCTRAIL_CFG` -> `./ctrail.xml` -> `/etc/ctrail.xml`) and
  the `<execution>`/`<coloring>`/`<filtering>` structure, including `<stdinfilter>` and the
  deprecated legacy `stdin` filefilter.
- ⚠️ **`CtrailProps` singleton caveat** — it caches on the `CTRAIL_CFG` system property, and
  surefire's `forkMode=always` / `reuseForks=false` exists to stop singleton state leaking
  between test classes. Flagged as "do not optimize away".
- **Build/test/run commands**, the **52-test baseline**, and the note that test logs
  intentionally contain ERROR/WARN lines from negative-path cases (exit code is the signal).
- **Tests section** rewritten for JUnit 4, with the fixture layout
  (`configs/`, `sources/`, `expected/`) and the "delete the covered line and watch it go
  red" verification rule.
- **Tool Usage Priority** retained (IDE first, Copilot CLI, terminal last).
- **New "Known environment gotchas"** — the IDE terminal silently strips blank lines from
  heredocs, and file tools can serve stale cache; verify writes against disk with
  `wc -l` / `grep -c '^$'`. Learned the hard way while writing the skills earlier today.
- **Plan-first gate** pointing at the `project-plans` skill: plans are Confluence pages
  under `java / ctrail / <plan title>`, markdown as fallback, and **never use the Atlassian
  MCP — it is broken**.
- **Document paths table** with `PROJECT` = `ctrail`, plus the note that top-level
  `CHANGELOG.md` tracks released versions.
- **Coding standards** kept and made concrete: StringUtils-only, no streams, 5-7 line
  commented blocks, Javadoc, `final` params and the codebase's trailing-underscore
  parameter convention (`line_`), Lombok retained, logging tiers.
- **Threading rule** specific to this codebase: producers must use `put()` on the bounded
  `BlockingDeque`, never `add()` (which throws when full and previously killed the reader
  thread), and `LineFormatter.format()` must stay thread-safe via locals.
- **Iteration limit (2)**, self-review as a separate step, knowledge/knowledge-gap paths.

## Finding: `gh` is not usable for this repo as configured

While verifying a claim rather than assuming it, `gh auth status` returned:

```
verame.ghe.com
  - Logged in to verame.ghe.com account kevin (keyring)
  - Active account: true
```

`gh` is authenticated to **verame.ghe.com only** — an unrelated host. ctrail lives on
**github.com** (`kevgol0/ctrail`). A `gh pr create` here would target the wrong host or
fail. My first draft of the instructions asserted `gh` was configured for github.com; that
was wrong and was corrected before commit.

The file now instructs: run `gh auth login --hostname github.com` or prefix with
`GH_HOST=github.com`, confirm with `gh auth status`, and note that plain `git push`/`pull`
work independently via the `github.com-kevgol0` SSH alias.

## Verification

- File on disk: **256 lines, 54 blank lines**, frontmatter `applyTo: '**'` preserved.
- Remaining matches for `dakota|verame|micronaut|mockito|restassured|junit 5|create-prs`
  are all **deliberate negations** ("there is no Micronaut", "do not write JUnit 5",
  "ignore any instruction referencing create-prs.sh"), included as guardrails so a future
  agent inheriting the old rules knows they do not apply.
- No production code touched; no build impact.

## Publication

Not published to Confluence. Per the `project-plans` skill the target would be
`java / ctrail / …`, but the Confluence site and space for this personal project are still
unresolved — the earlier "create a new space for java-development/ctrail" request was never
pinned down to a site/space key, and no credentials were verified. Markdown is therefore
the fallback here, stated explicitly rather than left silently blank.

## Rollback

`git revert` the commit, or restore the previous file from history:
`git show <prev>:.github/instructions/project.instructions.md`.

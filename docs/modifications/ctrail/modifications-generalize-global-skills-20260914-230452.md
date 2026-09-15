# Modifications: Generalize Claude skills and make them global

- **Date:** 20260914-230452
- **Project:** ctrail
- **Plan:** docs/plans/ctrail/plan-de-dakota-local-skills-20260914-230452.md
- **Branch:** master

## Summary

The `.claude/skills/` directory in this repo held four skills copied verbatim
from the Verame.Dakota project. They described another org's Jira project,
Confluence space, service topology and test stack — none of which apply to
ctrail. Per user direction the skills were **generalized rather than deleted**
and **installed globally** at `~/.claude/skills/`, so they apply to every
project; the project-local Dakota copies were removed from this repo.

## Why this mattered

Beyond the naming, the skills were **actively wrong** for this repo. They
mandated "JUnit 5 + Mockito" unit tests and a "RestAssured integration test per
endpoint". ctrail's suite is **JUnit 4** (`org.junit.Test`, `Assert`, `Before`,
`Ignore`), has **no Mockito and no RestAssured** on the classpath, and has **no
endpoints** — it is a command line tool. An agent following the old skills would
have written tests that do not compile. Every generalized skill now instructs the
reader to inspect the project's actual test stack before assuming a framework.

They also embedded another org's operational details: Jira project key `DAK`,
Confluence space `Nebraska (NE)` (id `753670`, homepage `753823`), cloudId
`b3967305-…`, `verame.atlassian.net`, and hard-coded issue-type ids.

## Files changed

### Global (`~/.claude/skills/`) — new, not in this repo

| Skill | Origin | Change |
|---|---|---|
| `project-plans/SKILL.md` | `dakota-project-plans` | Renamed + generalized |
| `code-reviews/SKILL.md` | `dakota-code-reviews` | Generalized |
| `confluence-pages/SKILL.md` | `dakota-confluence-pages` | Generalized |
| `jira-tickets/SKILL.md` | `dakota-jira-tickets` | Generalized |

- **project-plans** — kept the plan path contract
  (`docs/plans/PROJECT/plan-TITLE-YYYYMMDD-HHMMSS.md`), status tags, 12-section
  skeleton, DoD spine and the 2-iteration limit. Dropped DAK/NE links, the
  9-module port/debug table and the multi-repo merge-gate rationale (reframed as
  "consider carefully whether PR-merged belongs"). Added the "nothing enforces
  the plan gate but you" caveat and the test-stack discovery warning.
- **code-reviews** — kept the house Java standards (StringUtils-only, no streams,
  5–7 line commented blocks, Javadoc, `final` params, logging tiers, Lombok), the
  C#/D#/Q# labels with blocker/should-fix/nit severity, and the 8-section
  modifications-log structure. Replaced Micronaut/MDC/`MdcAwareExecutor`/
  `dakota-shared-components`/OpenAI/HIBP specifics with "the project's own
  CLAUDE.md overrides this checklist". Retained the Lombok-on-
  `@ConfigurationProperties` carve-out; added leaked secrets as an automatic
  blocker.
- **confluence-pages** — site and space are now **resolved at runtime** from
  `CONFLUENCE_EMAIL` / `CONFLUENCE_API_TOKEN` plus a space-key lookup, replacing
  hard-coded ids. Kept the page contract, design-doc template, storage-format
  HTML block reference (panels, status lozenges, task/decision lists, ToC macro,
  nesting rules) and the create/update REST recipes. **"Never use the Atlassian
  MCP — it is broken" promoted to a prominent top-of-file rule.** Added a
  "publishing isn't possible" fallback clause.
- **jira-tickets** — project key, site, cloudId and issue-type ids are now
  discovered via `createmeta` instead of hard-coded; the fixed issue-type id
  table was removed. Kept field discovery, ticket contract, sizing/decomposition
  guidance, mandatory issue linking (with verification), ticket anatomy, the five
  per-type templates, ADF formatting, and REST-preferred/`acli`-fallback recipes.
  Same "never use MCP" rule; DoD now defers to the project's real test stack.

### This repo

- **Deleted** `.claude/skills/` (5 files): `dakota-code-reviews/SKILL.md`,
  `dakota-confluence-pages/SKILL.md`,
  `dakota-confluence-pages/evals/evals.json`, `dakota-jira-tickets/SKILL.md`,
  `dakota-project-plans/SKILL.md`.
- **`.gitignore`** — added `*.iml` (the list had `.idea` but not `*.iml`).
- **Un-staged** `ctrail.iml`, which had been accidentally added to the index.
- `.claude/settings.json` left unchanged.

## Verification

- `~/.claude/skills/` contains exactly four `SKILL.md` files; every frontmatter
  `name` matches its directory name (`code-reviews`, `confluence-pages`,
  `jira-tickets`, `project-plans`).
- Grep across the global skills for
  `dakota|verame|nebraska|b3967305|753670|753823|DAK-[0-9]|808[1-5]` →
  **no matches**.
- `git status` clean apart from the intended deletions, the `.gitignore` edit and
  the docs; `ctrail.iml` no longer appears (now ignored).
- No production code touched — no build impact.

## Follow-up (approved, not yet done)

`.github/instructions/project.instructions.md` carries the same contamination:
`dakota-infrastructure-builder/scripts/create-prs.sh`, the "Verame.Dakota parent
repo with 9 nested git repos", `verame.ghe.com`, Micronaut version checks, and
the wrong JUnit 5 / Mockito / RestAssured test mandate. The user approved fixing
it; that is the next task.

## Rollback

- Deleted skills remain in git history: `git show <prev>:.claude/skills/…`.
- Global skills are plain files under `~/.claude/skills/` and can be deleted
  without affecting any repository.

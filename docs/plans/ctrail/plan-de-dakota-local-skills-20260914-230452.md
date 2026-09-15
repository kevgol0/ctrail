# Plan: De-Dakota the local Claude skills

- **Status:** APPROVED — EXECUTED
- **Created:** 20260914-230452
- **Project:** ctrail
- **Author:** GitHub Copilot (agent)

> **Revision after user direction:** the original draft proposed *deleting*
> `jira-tickets` and `confluence-pages`. The user instead directed:
> **generalize all four skills and make them globally available.** They were
> rewritten org-agnostic, installed to `~/.claude/skills/`, and the
> project-local Dakota copies were removed from this repo.

## Goal

The `.claude/skills/` content was copied from the Verame.Dakota project and did
not describe ctrail. Replace it with generalized, project-agnostic skills that
live in the global Claude skills directory so they load for every project.

## Findings (verified)

1. **Partial rename was already staged** — `dakota-code-reviews`,
   `dakota-confluence-pages`, `dakota-jira-tickets` renamed to drop the
   `dakota-` prefix (and `confluence-pages/evals/evals.json` deleted).
   `dakota-project-plans/` still carried the old name. All *bodies* were 100%
   Dakota.
2. **jira-tickets** (458 lines) hard-coded another org's Jira: project key `DAK`,
   `verame.atlassian.net`, a cloudId, issue-type IDs, and credential env vars.
3. **confluence-pages** (193 lines) hard-coded Confluence space `Nebraska (NE)`,
   space id `753670`, homepage `753823`, and the same cloudId.
4. **code-reviews** — the coding-standard half was reusable; the rest was
   Micronaut `@Value`, MDC/`X-Correlation-ID`, `MdcAwareExecutor`,
   `dakota-shared-components`, OpenAI/HIBP clients, `.air/review/review-prompt.md`.
5. **dakota-project-plans** — plan contract, status tags and skeleton reusable;
   DAK/NE links, `### dakota-<module>` headings, the 9-module port/debug table
   and the multi-repo merge-gate rationale were not.
6. ⚠️ **The test rules were factually wrong for ctrail.** The skills mandated
   "JUnit 5 + Mockito" and "RestAssured per endpoint". ctrail's suite is
   **JUnit 4** (`org.junit.Test`/`Assert`/`Before`/`Ignore`), has **no Mockito
   and no RestAssured**, and has **no endpoints** — it is a CLI. Following them
   would have produced tests that do not compile.

## Changes executed

1. Created the global skills directory `~/.claude/skills/` (did not exist).
2. **`project-plans`** (renamed from `dakota-project-plans`) — kept the plan path
   contract, status tags, section skeleton and DoD spine. Dropped DAK/NE links,
   module/port table, multi-repo rationale. Added an explicit warning to
   discover the project's real test stack before writing the test section, and
   the "nothing enforces the plan gate but you" caveat.
3. **`code-reviews`** — kept the house Java standards (StringUtils-only, no
   streams, 5–7 line commented blocks, Javadoc, logging tiers, Lombok), the
   C#/D#/Q# label scheme with blocker/should-fix/nit severity, and the 8-section
   modifications-log format. Replaced Dakota patterns with "the project's own
   CLAUDE.md overrides this checklist", kept the Lombok /
   `@ConfigurationProperties` carve-out, and added the leaked-secret blocker.
4. **`confluence-pages`** — site/space now resolved at runtime from
   `CONFLUENCE_EMAIL` / `CONFLUENCE_API_TOKEN` plus a space-key lookup, instead
   of hard-coded ids. Kept the page contract, design-doc template, storage-format
   HTML block reference, and REST publishing recipe. **Promoted "never use the
   Atlassian MCP — it is broken" to a prominent top-of-file rule.**
5. **`jira-tickets`** — project key, site, cloudId and issue-type ids are now
   discovered via `createmeta` rather than hard-coded. Kept field discovery,
   ticket contract, sizing/decomposition, mandatory issue linking, anatomy,
   per-type templates, ADF guidance, REST-preferred / `acli`-fallback recipes.
   Same "never use MCP" rule. DoD now says to use the project's real test stack.
6. Deleted `.claude/skills/` from the ctrail repo (5 files) — superseded by the
   global copies and wrong for this project.
7. Added `*.iml` to `.gitignore` and un-staged an accidentally added
   `ctrail.iml` (the ignore list had `.idea` but not `*.iml`).

## Out of scope (flagged, not changed)

`.github/instructions/project.instructions.md` is still Dakota-contaminated: it
references `dakota-infrastructure-builder/scripts/create-prs.sh`, the
"Verame.Dakota parent repo with 9 nested git repos", `verame.ghe.com`, Micronaut
version checks, and the same wrong JUnit 5/Mockito/RestAssured mandate. The user
approved fixing it — tracked as the next step, not done in this pass.

## Verification

- `~/.claude/skills/` contains exactly four `SKILL.md` files; each frontmatter
  `name` matches its directory name.
- Grep for `dakota|verame|nebraska|b3967305|753670|753823|DAK-[0-9]|808[1-5]`
  across the global skills: **no matches**.
- No production code touched, so no build impact.

## Rollback

The deleted skill files remain in git history (`git show HEAD~1:.claude/skills/…`).
The global skills are plain files under `~/.claude/skills/` and can be removed
without affecting any repo.

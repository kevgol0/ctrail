---
name: dakota-project-plans
description: "Use when writing, drafting, or structuring a project/implementation plan for the Dakota project — before any code execution or file modification (the non-negotiable plan-first gate), or when scoping large multi-phase work that needs Jira/Confluence traceability. Covers the plan file naming contract, status tags, the standard section skeleton, Definition of Done wording, phase markers, and how plans link to Jira (DAK) and Confluence (NE). Trigger on requests to write a plan, scope a feature, or plan an implementation."
---

# Writing project plans for Dakota

Plans are written **before** any code execution or file modification and the author waits
for explicit confirmation ("go", "approved", "proceed") before continuing. This is
non-negotiable — never proceed without an approved plan. If a problem is discovered
mid-task, **stop**, write up what was found, and ask for instructions rather than pressing
on. (A `PreToolUse` hook gates Edit/Write on a recent plan file.)

## When to use this skill

- Before touching code/files for any non-trivial change.
- When scoping a feature, refactor, or investigation.
- When large work needs to be tracked in Jira (see [[dakota-jira-tickets]]) and published
  to Confluence.

## File contract

- Path: `docs/plans/PROJECT/plan-TITLE-YYYYMMDD-HHMMSS.md` (PROJECT is usually `DAKOTA`).
- **TITLE has no special characters** — kebab-case words only.
- Timestamp from the shell: `date +%Y%m%d-%H%M%S`.
- Companion file naming (for reference): Jira-focused plan → `jira-TITLE.md`;
  Confluence design → `confluence-TITLE-design.md`, both under `docs/plans/DAKOTA/`.

## Status tags

Put a `**Status:**` line in the metadata block and advance it:

- `SCOPED` — drafted, no go-ahead yet.
- `AWAITING "go"` — draft complete, waiting on approval before Jira/implementation.
- `Approved ("go")` — user approved; Jira issues created; implementation may proceed.

Within a plan that has phases, mark a phase `[FINISHED]` **only after its tests pass**
(per the working agreement's Tests rule). Small single-shot plans use the status line
alone and omit phase markers.

## Section skeleton

Full skeleton below; collapse or drop sections for small plans (a one-file fix needs only
metadata, goal, changes, tests, verify). Order matters — decisions and scope come early so
a reviewer can approve without reading the whole plan.

1. **Title + metadata** — Date; `Status:`; Jira link(s) (primary + related epic);
   `Confluence:` (`_TBD — publish, linked to DAK-N_` or the real URL); author if non-user.
2. **Confirmed decisions / scope** — numbered list of user-agreed constraints and calls;
   state what is explicitly OUT or DEFERRED.
3. **Problem / goal** — the "why": the gap or need, in a few sentences.
4. **Technical design / background** — architecture and API facts that ground the work;
   "how it works today" for refactors; concrete class/method/file references.
5. **Detailed changes** — per module or concern (`### dakota-<module>`), with sub-sections
   per file, new-vs-edit flags, method signatures, and code snippets for load-bearing bits.
6. **Tests** — enumerate the test classes/methods to add and how to run them; state the
   coverage rule (every new logic-bearing method gets a unit test; endpoints get
   RestAssured integration tests when feasible).
7. **Build / verify** — sequential `mvn`/`npm`/`docker` steps and the smoke-test
   expectation (log excerpts, final ✅/❌ verdict).
8. **Jira / Confluence artifacts** — for epic-level plans, the ticket breakdown (defer the
   actual ticket anatomy to [[dakota-jira-tickets]]); links to sister plans by relative
   path; the Confluence publication target.
9. **Open questions** — numbered and titled (e.g. "Q1. Weighting across signals"); note
   which block implementation vs. are follow-ups.
10. **Out of scope** — always explicit.
11. **Follow-ups** — non-blocking items for later.

## Definition of Done (standard wording)

> Unit tests (must); integration tests (preferred); verified on the target
> environment; committed on the feature branch; plan/changelog updated.

Extend per plan (e.g. "IaC applies in each env") but keep this spine.

**"PR merged" is deliberately NOT part of the DoD.** Dakota is a root repo plus ten
independent module repos (each with its own remote) sharing one long-lived branch name,
and PRs are cut on request, not per change — so gating "done" on a merge left tickets stuck at In Progress long after
the work was built, deployed and verified, which made the board describe the process
instead of the software. What matters for done is **verified behavior plus a commit
that holds it**; review and merge are release steps, tracked on the branch/PR, not
per ticket. Do not reintroduce a merge gate here.

## Linking conventions

- Jira: `[DAK-N](https://verame.atlassian.net/browse/DAK-N)`.
- Sister plans: relative path, e.g. `docs/plans/DAKOTA/plan-xyz-….md`.
- Confluence: space **Nebraska (NE)** — note the space is named Nebraska, not Dakota.
- The originating plan is referenced back from the modifications log and any PR.

## Workflow

1. Timestamp, create the plan file at the path above with `Status: SCOPED`.
2. Fill the skeleton; keep decisions/scope early and explicit.
3. Set `Status: AWAITING "go"` and **stop for approval** — do not edit code first.
4. On "go": set `Status: Approved ("go")`, create Jira issues via [[dakota-jira-tickets]],
   publish/link Confluence, then implement phase by phase, marking phases `[FINISHED]` as
   their tests pass.
5. Record the work in a modifications log (see [[dakota-code-reviews]] for the log shape)
   and update the plan/Confluence.

## Dakota reference

| Module | Port | Debug |
|---|---|---|
| dakota-resume-ingestion | 8081 | 5005 |
| dakota-resume-orchestrator | 8082 | 5006 |
| dakota-candidate-catalog | 8083 | 5007 |
| dakota-resume-scoring | 8084 | 5008 |
| dakota-job-posting-management | 8085 | 5009 |
| dakota-client-frontend | 3000 | — |
| dakota-admin-frontend | 3001 | — |

`dakota-shared-components` (common library, builds first) and
`dakota-infrastructure-builder` (AWS provisioner) have no ports. Shared external clients
(OpenAI, HIBP) live in `dakota-shared-components`, not per service.

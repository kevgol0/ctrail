---
name: dakota-code-reviews
description: "Use when reviewing Dakota code — a diff, a branch, or a PR — or when recording code-review findings. Encodes Dakota's house coding standards (StringUtils-only, no streams, small commented blocks, Javadoc, logging tiers, JUnit+RestAssured, shared clients, correlation-id/MDC), the C#/D#/Q# finding-label scheme with blocker/should-fix/nit severity, and the modifications-log output format. Trigger on requests to review code, review a PR, or critique a change for Dakota. Complements the built-in /code-review and /review."
---

# Reviewing code for Dakota

## When to use this skill

Reviewing a Dakota diff/branch/PR, or recording review findings. Pair it with the
built-ins:

- **`/code-review`** — run first for the mechanical correctness/simplification pass over
  the working diff. This skill layers Dakota's house standards, finding labels, and output
  format on top of what it surfaces.
- **`/review`** — the path for a full GitHub PR review.

This skill is the Dakota-specific lens and the record-keeping format; it does not replace
those tools' diff analysis.

## Review checklist (Dakota house standards)

Source of truth: `.air/review/review-prompt.md` and the project CLAUDE.md. Check, grouped:

**Correctness**
- Logic is right; null/empty/edge cases handled; no obvious bugs.
- Tests exist for the changed behavior and pass (see Tests below).
- "Leave it better than you found it" — flag dead code, muddled naming, needless cleverness.

**Standards**
- **StringUtils only:** use `org.apache.commons.lang3.StringUtils` / `Strings` — no raw
  `String` method where a StringUtils equivalent exists.
- **No streams:** use explicit iteration, not the Streams API.
- **Small commented blocks:** logic organized into ~5–7 line blocks, each with a comment
  describing what the block does.
- **Javadoc** on all public Java methods; **inline comments on all YAML** config.
- **Lombok kept:** `@Data`, `@Builder`, etc. must not be removed.
- Methods small and readable; correctness/readability over cleverness.

**Logging** (log a lot, via Lombok `@Slf4j`)
- ERROR/WARN — for operators: failures and anomalous behavior.
- INFO — for analysts/operators: informative, business-level.
- DEBUG/TRACE — for developers: verbose, follow-along detail.

**Tests**
- JUnit 5 + Mockito unit test per logic-bearing method (a must).
- RestAssured integration test per endpoint (strongly preferred).
- Tests green before a phase is marked `[FINISHED]`.

**Dakota patterns**
- Shared external clients (OpenAI, HIBP) belong in `dakota-shared-components`, not copied
  per service.
- Correlation ID: `X-Correlation-ID` header propagated via MDC across services.
- Async work wrapped in `MdcAwareExecutor` (never a bare JDK executor — loses MDC).
- Cross-service duplicated constants (e.g. `CORRELATION_ID_ATTR`) flagged for
  consolidation into `dakota-shared-components`.
- Config via Micronaut `@Value` beans; env-var-driven with sensible defaults.
- Before recommending dependency/config changes, check for known Micronaut/YAML breaking
  changes in the relevant version range.

## Finding labels & severity

Label each finding by category (the established convention):

- **C#** — Correctness / bug.
- **D#** — Duplication / consolidation.
- **Q#** — Quality / simplification / readability.

Each finding carries a **severity**:

- **blocker** — must fix before merge (bugs, missing tests on new logic, broken standards
  that affect behavior).
- **should-fix** — fix in this PR barring a good reason (standards violations, duplication).
- **nit** — optional polish.

Finding shape: `**C1** (blocker) — path/File.java:120 — <what> — <why it matters> —
<suggested fix>`.

## Recording findings — modifications log

Record review output in
`docs/modifications/PROJECT/modifications-code-review-TITLE-YYYYMMDD-HHMMSS.md` (TITLE has
no special characters; timestamp via `date +%Y%m%d-%H%M%S`). Use the standard 8-section
modifications structure:

1. **Header** — title; Date `YYYY-MM-DD HH:MM`; `Plan:` reference; branches touched; PRs.
2. **Recap** — what the change is / why (context from the plan).
3. **Scope** — any review-scope decisions or user directives.
4. **Findings / changes** — per module or file (`### dakota-<module>` →
   `File: path (+/- lines)`), each tagged with its C#/D#/Q# label and severity, with a
   concise what/why and a snippet for non-trivial cases.
5. **Tests** — which tests changed/added, counts, coverage, links to test classes.
6. **End-to-end verification** — repro steps, log excerpts, final ✅/❌ verdict.
7. **Risk / follow-ups** — non-blocking items, environmental issues, audit recommendations.
8. **Deliverables** — artifacts produced (this log, changelog files, updated plan).

This log is the source material for the PR description.

## Posting to the PR (only when asked)

By default, **write the modifications log** and summarize findings in chat. If the user
asks to comment on the PR, post the C#/D#/Q# findings as inline comments via the `gh` CLI
(or `/code-review --comment`). Do not post to a PR unprompted.

## Workflow

1. Identify the diff/branch/PR under review and the originating plan (if any).
2. Run `/code-review` for the mechanical pass; collect its findings.
3. Apply the Dakota checklist above; add/relabel findings as C#/D#/Q# with severity.
4. Write the modifications-code-review log; summarize blockers first in chat.
5. Post inline PR comments only if asked. Re-review after fixes (respect the 2-iteration
   limit — after two passes without resolution, stop and summarize for direction).

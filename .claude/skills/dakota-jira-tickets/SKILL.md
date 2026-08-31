---
name: dakota-jira-tickets
description: "Use when writing, drafting, creating, splitting, or sizing Jira tickets for the Dakota project (DAK) — epics, stories, tasks, bugs, or sub-tasks — or when a large plan needs Jira traceability. Covers the required/optional ticket contract, appropriate sizing/decomposition, ticket anatomy and per-type templates (with sample data and sample API calls), field discovery, ADF description formatting, mandatory issue linking, and the creation path via the `acli` Atlassian CLI. Trigger on requests to write a ticket, file a bug, break work into stories, create an epic, or attach Jira to a plan."
---

# Writing Jira tickets for Dakota

The Dakota Jira project is **`DAK`** ("Dakota") on **verame.atlassian.net**.

- **cloudId:** `b3967305-ab86-4a1e-86a4-21b202d86db7`
- **Project key:** `DAK` (project id `10001`, classic software project)
- **Issue types available** (do not assume others exist — verified 2026-07-01):

  | Type | id | Hierarchy | Use for |
  |------|------|-----------|---------|
  | Epic | 10000 | 1 (parent) | Umbrella for work too big for one story; spans multiple stories. |
  | Story | 10005 | 0 | A vertical, user-visible slice of functionality. |
  | Task | 10006 | 0 | A concrete piece of work that isn't user-facing functionality. |
  | Bug | 10008 | 0 | A defect / error in existing behavior. |
  | Sub-task | 10007 | -1 (child) | An implementation step inside a Story/Task/Bug. |

- **No Spike type exists.** Model a spike/investigation as a **Task** with a `spike`
  label and a timebox in the description.

> Always re-discover field requirements at create time. Prefer the **Jira REST API via `curl`**
> (token-cheapest — Basic auth with `CONFLUENCE_EMAIL` + `CONFLUENCE_API_TOKEN` from `~/.bashrc`,
> the same token works for Jira); `acli` is the fallback where REST is awkward. Never use the
> Atlassian MCP (broken). Never hard-code field IDs beyond the issue-type ids above — required
> fields and components can change, so re-run field discovery (below) before creating tickets.

## When to use this skill

The user asks to write / draft / create / file / split / size Jira work, or a large plan
(per the working agreement) needs to be tracked in Jira before work starts.

## Field discovery (do this before drafting field values)

Don't guess field IDs and don't fall back to writing structured data as prose unless a
step below tells you no field exists. Before drafting a ticket that needs Acceptance
Criteria, Story point estimate, or Priority:

```bash
source "$HOME/.bashrc"
AUTH="$CONFLUENCE_EMAIL:$CONFLUENCE_API_TOKEN"; JIRA="https://verame.atlassian.net/rest/api/3"

# List all fields and grep for the ones we care about (names vary by project config)
curl -sS -u "$AUTH" "$JIRA/field" | python3 -c '
import sys, json
for f in json.load(sys.stdin):
    if any(k in f["name"].lower() for k in ["acceptance", "story point", "priority"]):
        print(f["id"], "-", f["name"])
'

# Confirm which of those fields are actually on the DAK create screen for this issue type
curl -sS -u "$AUTH" \
  "$JIRA/issue/createmeta?projectKeys=DAK&issuetypeNames=Story&expand=projects.issuetypes.fields" \
  | python3 -c 'import sys,json;d=json.load(sys.stdin);print(list(d["projects"][0]["issuetypes"][0]["fields"].keys()))'
```

Cache the resolved field IDs for the session (they don't change ticket-to-ticket), e.g.:

- `customfield_XXXXX` → Story point estimate
- `customfield_YYYYY` → Acceptance Criteria (only if this exists in the DAK scheme —
  many Jira Cloud projects don't have one by default)
- `priority` → built-in field, always available (`{"name": "Medium"}` etc.)

**If a dedicated Acceptance Criteria field does not exist** in the create metadata, that's
the one case where AC stays as a checklist in the ADF description — note in the ticket
that no AC field is configured, don't silently drop it. Story points and priority are
built-in-or-commonly-configured enough that they should almost always resolve to a real
field; if `createmeta` genuinely has neither, size the ticket in the description as a
last resort and say so.

## Ticket contents contract

Every ticket **must** have:

- **Title** (Jira "Summary") — one scoped line.
- **Description** — see anatomy below.

Every ticket **may** have, when it reduces ambiguity for the implementer (use judgment —
don't pad a ticket with sections that add nothing):

- **Sample data** — realistic, synthetic fixtures the work reads or produces.
- **Sample API calls** — the concrete request/response the work exercises.
- **Links to children / related items** — epic link, sub-tasks, `blocks` / `relates to`.
  These are **real Jira links created via the API**, not text — see "Mandatory issue
  linking" below.
- **Links to repos or branches** — the module repo, feature branch, or PR (this one stays
  as a URL in the description; Jira has no first-class field for it here).
- **Links to Confluence** — the plan/design page backing this work (also a description URL).
- **Expected tests** — the tests that must exist for the ticket to be "done".

Rule of thumb: include an optional element only when it makes the ticket more actionable.
A one-line config bump doesn't need sample data; a scoring-algorithm change does.

## Mandatory issue linking

**Every ticket this ticket relates to must get a real Jira issue link, not a mention in
the description.** "Related" includes: the parent epic, sibling stories carved from the
same decomposition, a bug found while doing the story, a ticket this one blocks or is
blocked by, and a ticket it duplicates. If you'd write "see DAK-65" anywhere in a
description, that's the signal to also create a link for DAK-65.

- **Epic → Story/Task/Bug**: set via the `parent` field at create time (see payload
  below), not the generic issue-link API.
- **Story/Task/Bug/Sub-task ↔ anything else** (`Relates`, `Blocks`, `Duplicates`): create
  with the `issueLink` endpoint (or `acli jira workitem link create`) **immediately after
  both issues exist** — don't defer it, and don't consider the ticket finished until the
  link is created and verified with a link-type list check.
- When creating a **tree in one pass** (epic + stories + sub-tasks), create every issue
  first, collect the keys, then run a second pass that creates every link. This avoids
  guessing keys ahead of time.
- Before creating a `Relates`/`Blocks` link, confirm the link type name against
  `GET /issueLinkType` (or `acli jira workitem link type`) since the exact outward name
  matters to the API call.
- **Verify, don't assume**: after creating links, do a quick `GET /issue/{key}?fields=issuelinks`
  (or `acli ... view --json`) on at least one ticket in the batch to confirm the links
  actually landed before reporting completion to the user.

## Appropriate sizing & decomposition

Create the work the change actually needs — not always one ticket, not always many.

- **One ticket** when the work is a single cohesive deliverable: one concern, one (or a
  tightly related) module, demoable in a single slice, ≤ ~1 day.
- **Decompose** when any of these hold:
  - It spans multiple modules or multiple unrelated concerns.
  - It mixes new functionality **and** a bug fix (split the bug into its own Bug).
  - It has multiple distinct clusters of acceptance criteria.
  - A single "story" can't be demoed end-to-end in one slice.
  - It's more than ~1 day of work.

Decomposition shape:

```
Epic (DAK-…)            umbrella, outcome-level — "Candidate bulk upload"
 ├─ Story (DAK-…)       vertical slice — "Upload accepts a zip of PDFs"
 │   ├─ Sub-task        impl step — "S3 multipart handler"
 │   └─ Sub-task        impl step — "SQS fan-out message"
 ├─ Story (DAK-…)       vertical slice — "Per-file ingestion status UI"
 └─ Bug   (DAK-…)       defect found in scope — "Duplicate key on re-upload"    ── Relates: linked to the Story above
```

Every sibling produced by one decomposition pass gets linked to every other sibling it
has a real relationship with (not a full mesh for its own sake — link what actually
relates, e.g. the Bug found during a Story links `Relates` back to that Story).

Prefer **vertical slices** (each story delivers observable value) over horizontal layers
("all the DAOs", "all the controllers"). Sub-tasks are the place for horizontal impl steps.

## Anatomy of a good ticket

- **Summary** — human readable, one line, scoped. "Candidate upload rejects oversized PDFs"
  not "Fix upload."
- **Description** — three short parts: **Context/why**, **Problem or goal**, **Proposed
  approach**. For a Bug: **Steps to reproduce**, **Actual**, **Expected**.
- **Acceptance criteria** — set in the dedicated Acceptance Criteria field discovered
  above, as a clear testable checklist. Only fall back to a description checklist if
  field discovery confirms DAK has no such field configured (see "Field discovery").
- **Definition of Done** — ties to the working agreement's Tests rules:
  - Unit tests are a must (JUnit 5 + Mockito) for logic-bearing methods.
  - Integration tests strongly preferred (RestAssured) for endpoints / external
    integrations (DynamoDB/SQS/S3/LLM) when feasible.
  - Verified on the target environment — for anything deployed, that means the
    BUILT/DEPLOYED/HIT gate, not a green local build.
  - Committed on the feature branch; plan/changelog updated.
  - **Not "PR merged".** Dakota is a root repo plus ten independent module repos
    sharing one long-lived branch name, and PRs are cut on request rather than per change, so a merge
    gate kept verified, deployed work sitting at In Progress — the board ended up
    describing the process instead of the software. Done means **verified behavior
    plus a commit that holds it**; review and merge are release steps tracked on the
    branch/PR. Don't reintroduce a merge gate, and don't block a close on one.
- **Sample data** (optional) — see below.
- **Sample API calls** (optional) — see below.
- **Components / labels** — set the component(s) matching the affected Dakota module(s)
  when components are configured in the project; otherwise use labels (e.g. the module
  name: `resume-scoring`, `candidate-catalog`).
- **Estimate & priority** — set the real `Story point estimate` custom field and the
  built-in `priority` field discovered above (not a number typed into the description).
- **Links** — Epic link via the `parent` field, `blocks` / `relates to` via real issue
  links (see "Mandatory issue linking"), plus the Confluence plan page and branch/PR URL
  as plain links in the description (Jira has no dedicated field for those two).

## Sample data

Include a small, realistic fixture when the ticket reads or produces structured data.
Keep it minimal but complete, and **synthetic** — never paste real candidate PII.
Use a fenced code block with the right language so it renders as a code block in the ADF
description (see "Description formatting").

```json
// Candidate record (anonymized) — dakota-candidates DynamoDB item
{
  "candidateId": "cand-000123",
  "displayName": "Candidate 000123",
  "yearsExperience": 7,
  "skills": ["java", "aws", "micronaut"],
  "resumeS3Key": "resumes/cand-000123.pdf"
}
```

```json
// ResumeScore result the scoring service should return
{
  "candidateId": "cand-000123",
  "jobPostingId": "job-42",
  "score": 0.81,
  "signals": { "skillMatch": 0.9, "experienceMatch": 0.7 },
  "rationale": "Strong Java/AWS overlap; slightly under required years."
}
```

## Sample API calls

Show the exact call under test — correct service **port** (from CLAUDE.md), the
correlation-id header the platform propagates, the request body, and the expected
response. This lets the implementer and reviewer see the contract without guessing.

Dakota service ports: ingestion `8081`, orchestrator `8082`, candidate-catalog `8083`,
scoring `8084`, job-posting `8085`.

```bash
# Score a candidate's resume against a job posting
curl -sS -X POST http://localhost:8084/api/scoring/score \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: 11111111-2222-3333-4444-555555555555" \
  -d '{ "candidateId": "cand-000123", "jobPostingId": "job-42" }'
```

```json
// Expected 200 response
{ "candidateId": "cand-000123", "jobPostingId": "job-42", "score": 0.81 }
```

For a Bug, show the call that currently misbehaves plus the actual vs. expected response.

## Description formatting (ADF)

Jira renders the description as Atlassian Document Format (ADF). With `acli`, pass a rich
body via `--description-file` pointing at an **ADF JSON** file so headings, checklists, and
the sample data / sample API code blocks render correctly (a `codeBlock` node with a
`language` attr for the JSON/bash snippets, `heading` nodes for sections, `bulletList` for
AC only when no AC field exists). A plain `--description` string becomes one paragraph —
acceptable for a trivial ticket, but prefer an ADF `--description-file` whenever the ticket
has code blocks or multiple sections.

## Per-type templates

**Epic**
```
Summary: <outcome-level capability>
Description:
  Context: <why this matters / business driver>
  Goal: <the capability delivered when all child stories ship>
  Out of scope: <explicit exclusions>
Child stories: <list, created as separate Story issues with parent = this Epic>
Links: Confluence plan <URL>.
```

**Story**
```
Summary: <user-visible slice>
Description:
  Context: <why>
  Goal: As a <role>, I want <capability>, so that <benefit>.
  Approach: <brief technical direction>
Fields:
  Acceptance Criteria (field): <observable condition 1>; <observable condition 2>
  Story point estimate (field): <points>
  Priority (field): <priority>
Sample data (optional): <fixture the story reads/produces>
Sample API calls (optional): <request + expected response>
Definition of Done: unit tests (must), integration tests (preferred), verified on the
  target environment, committed on the feature branch. (NOT "PR merged" — see above.)
Links: parent = Epic <DAK-…>; issue links to any related/blocking tickets; plan
  <Confluence URL>; branch/PR <URL> (description text).
```

**Bug**
```
Summary: <symptom, scoped>
Description:
  Steps to reproduce: 1) … 2) …
  Actual: <what happens>
  Expected: <what should happen>
  Environment: <local/LocalStack/deployed, version/commit>
Fields:
  Acceptance Criteria (field): Defect no longer reproduces; regression test added
  Priority (field): <priority>
Sample data (optional): <input that triggers the defect>
Sample API calls (optional): <the misbehaving call, actual vs expected response>
Links: issue link (Relates) to the Story/Task it was found in, if any; branch/PR <URL>.
```

**Task**
```
Summary: <concrete, non-user-facing piece of work>
Description:
  Context: <why> — Goal: <what done looks like> — Approach: <brief>
Fields:
  Acceptance Criteria (field): <verifiable outcome>
  Story point estimate (field): <points, if sized>
  Priority (field): <priority>
Definition of Done: tests where logic-bearing; verified; committed on the feature branch.
Links: parent = Epic <DAK-…> (if part of one), plan <URL>.
```

**Spike (Task + `spike` label)**
```
Summary: Spike: <question to answer>
Description:
  Question: <what we need to learn>
  Timebox: <e.g. 1 day>
  Deliverable: <decision doc / recommendation / linked Confluence page>
Labels: spike
```

## Creating tickets:

**Prefer the Jira REST API via `curl`** (token-cheapest) for create/link/comment; **`acli` is
the fallback** where REST is awkward (e.g. bulk trees). The Atlassian **MCP does not work — do
not use it.**

## Jira REST API (curl) — preferred

Basic auth with the `CONFLUENCE_EMAIL` + `CONFLUENCE_API_TOKEN` env vars (`~/.bashrc`; the same
Atlassian API token works for Jira). Pipe responses through `python3`/`jq` to read back only
the created key — not the ~250-line issue object `acli --json` dumps.

```bash
source "$HOME/.bashrc"
AUTH="$CONFLUENCE_EMAIL:$CONFLUENCE_API_TOKEN"; JIRA="https://verame.atlassian.net/rest/api/3"

# Create — build the payload from a file (ADF description). issueType ids are in the table
# above. Story points / priority use the real field IDs resolved during field discovery;
# swap in the actual customfield_XXXXX for this Jira instance.
cat > /tmp/issue.json <<'JSON'
{ "fields": {
    "project": { "key": "DAK" },
    "issuetype": { "id": "10006" },
    "summary": "<one-line summary>",
    "labels": ["infrastructure-builder"],
    "parent": { "key": "DAK-77" },
    "priority": { "name": "Medium" },
    "customfield_XXXXX": 3,
    "description": { "type":"doc","version":1,"content":[
      {"type":"paragraph","content":[{"type":"text","text":"Context: …"}]} ] }
} }
JSON
curl -sS -u "$AUTH" -X POST "$JIRA/issue" -H "Content-Type: application/json" \
  --data @/tmp/issue.json | python3 -c 'import sys,json;print(json.load(sys.stdin)["key"])'

# Comment
curl -sS -u "$AUTH" -X POST "$JIRA/issue/DAK-65/comment" -H "Content-Type: application/json" \
  -d '{"body":{"type":"doc","version":1,"content":[{"type":"paragraph","content":[{"type":"text","text":"…"}]}]}}' \
  -o /dev/null -w '%{http_code}\n'

# Link (inward blocks outward): type name from GET /issueLinkType. Create this for EVERY
# ticket referenced as related, blocking, or duplicate — not just the ones convenient to link.
curl -sS -u "$AUTH" -X POST "$JIRA/issueLink" -H "Content-Type: application/json" \
  -d '{"type":{"name":"Relates"},"inwardIssue":{"key":"DAK-65"},"outwardIssue":{"key":"DAK-80"}}' \
  -o /dev/null -w '%{http_code}\n'

# Verify links landed before reporting completion
curl -sS -u "$AUTH" "$JIRA/issue/DAK-65?fields=issuelinks" \
  | python3 -c 'import sys,json;d=json.load(sys.stdin);print([l.get("type",{}).get("name") for l in d["fields"]["issuelinks"]])'
```

Secret hygiene: token comes only from the env var — never write it to a repo file or a payload
that gets logged. Flag any exposed token for rotation.

## acli (Atlassian CLI) — fallback

The official Atlassian CLI is installed (`acli`, v1.3.x) and authenticated to
**verame.atlassian.net**. Verify with `acli jira auth status` before a session that creates
tickets. All Jira work-item commands live under `acli jira workitem`.

`acli`'s simple flags (`--summary`, `--description`, `--label`, `--parent`) don't cover
custom fields like Story point estimate or a project-specific Acceptance Criteria field.
For those, use `--from-json` with the custom field included in the payload, or fall back
to the REST API above — don't drop the field just because the simple flag doesn't support it.

**Create one ticket:**

```bash
# Simple: summary + type + project (DAK)
acli jira workitem create --project DAK --type Story \
  --summary "Upload accepts a zip of PDFs" \
  --description "Context: … Goal: … Approach: …"

# Rich description (ADF or plain text) from a file — use this to embed sample-data /
# sample-API code blocks (write ADF JSON or plain text to the file first).
acli jira workitem create --project DAK --type Bug \
  --summary "Duplicate key on re-upload" \
  --description-file ./ticket-body.adf.json \
  --label resume-ingestion

# Sub-task or epic child: --parent takes the parent/epic key
acli jira workitem create --project DAK --type Sub-task \
  --parent DAK-123 --summary "S3 multipart handler"

acli jira workitem create --project DAK --type Story \
  --parent DAK-100 --summary "Per-file ingestion status UI"   # DAK-100 is the Epic
```

Useful flags: `--assignee @me` (or an email), `--label a,b`, `--json` (capture the created
key for scripting), `--editor` (open $EDITOR for summary+description), `--generate-json`
then `--from-json file.json` (structured single-item creation with a full ADF description
and custom fields, e.g. story points and priority).

**Bulk create** (epic + stories + sub-tasks in one go):

```bash
acli jira workitem create-bulk --generate-json > issues.json   # get the template
# edit issues.json (array of {summary, projectKey, issueType, description, label,
# parentIssueId, assignee}); parentIssueId wires children to their parent/epic
acli jira workitem create-bulk --from-json issues.json --yes
# CSV also works: --from-csv issues.csv (same columns)
```

**Link issues (do this for every related ticket, not just the epic parent):**

```bash
acli jira workitem link type                       # list valid link types first
acli jira workitem link create --out DAK-2 --in DAK-1 --type Blocks   # DAK-2 blocks DAK-1
# --type accepts the outward description (e.g. "Blocks", "Relates"). Bulk via --from-json.
```

Other handy commands: `acli jira workitem edit`, `... comment`, `... transition`,
`... attachment`, `... view --json`, `... search` (JQL).

> `acli` has no `--cloudid` flag — it resolves the site from the authenticated account,
> so confirm `acli jira auth status` shows `verame.atlassian.net` before creating.

## Workflow

1. **Confirm project & types** — `acli jira project view DAK` (or `acli jira workitem
   search --jql "project = DAK"`) to confirm the project and re-check issue types.
2. **Discover fields** — resolve Story point estimate, Acceptance Criteria (if it exists),
   and Priority field IDs per "Field discovery" above.
3. **Size & decompose** the work per the sizing rules — decide single ticket vs.
   epic+stories(+sub-tasks/bug/spike), and note every relationship between the tickets
   you're about to create (epic link, relates, blocks, duplicates).
4. **Draft** the ticket(s) using the templates — always Title + Description; fill
   Acceptance Criteria, Story point estimate, and Priority as real fields (not description
   text); add sample data, sample API calls, and expected tests where they help — and
   **show the user for approval** before creating anything in Jira.
5. **Create** in Jira (epic → stories → sub-tasks) with `acli jira workitem create`
   (or `create-bulk` for the whole tree), using `--description-file` for ADF bodies with
   sample-data / API code blocks. Create the Epic first so children can reference its key.
6. **Link every related ticket** — Epic link is set via `parent` at create time; every
   other relationship (`Relates`, `Blocks`, `Duplicates`) gets a real issue link created
   right after both issues exist, plus the Confluence plan page and branch/PR links added
   to the description. Verify links landed with a follow-up `GET`/`view --json` before
   moving on.
7. **Report** the created issue keys back to the user, along with the links created.

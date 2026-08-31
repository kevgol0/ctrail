---
name: dakota-confluence-pages
description: "Use when writing, drafting, creating, or updating Confluence pages for the Dakota project — design/architecture docs, technical write-ups, plans-as-pages, runbooks, or knowledge-base content in the Nebraska (NE) space on verame.atlassian.net. Covers the page-contents contract, a design/architecture-doc template, Confluence HTML/ADF body formatting (panels, code blocks, tables, task lists, tables of contents), and publishing programmatically via the Confluence REST API (curl + CONFLUENCE_* env creds). Trigger on requests to write a Confluence page, publish a design doc, document a decision or architecture, or turn a plan into a Confluence page."
---

# Writing Confluence pages for Dakota

Dakota's Confluence content lives in the **Nebraska** space (**`NE`**) on
**verame.atlassian.net**.

- **cloudId:** `b3967305-ab86-4a1e-86a4-21b202d86db7`
- **Space:** name `Nebraska`, key `NE`, id `753670`, homepage id `753823`

> **Publishing is programmatic via the Confluence REST API** (`curl`, token-cheapest — see
> below). `acli` is **view-only** for Confluence (`acli confluence page` exposes only `view`)
> so it's a read fallback, and the Atlassian **MCP integration is broken and must not be used**.
> Still draft the body first and get the user's OK before creating/updating a page.

## When to use this skill

The user asks to write / draft / create / update a Confluence page, publish a design or
architecture doc, document a decision or a runbook, capture knowledge, or turn a Dakota
plan (`docs/plans/DAKOTA/…`) into a shareable Confluence page in the NE space.

## Page contents contract

Every page **must** have:

- **Title** — a specific, searchable line. People find pages by title, so make it say what
  the page *is*: "Resume scoring — identity-alignment design" beats "Scoring doc".
- **Body** — a scannable structure (see the template) with real headings, not a wall of text.

Every page **should** have, when it reduces ambiguity for a reader landing cold:

- **A one-paragraph summary up top** — what this page is and who it's for, so a reader knows
  in five seconds whether they're in the right place.
- **Status / metadata** — Draft vs. Reviewed vs. Approved, owner, last-updated, related
  DAK tickets and the source plan/PR. A status panel or a small table at the top carries this.
- **Links out** — the Jira epic/stories (`DAK-…`), the source plan file, the module repo /
  branch / PR. A design doc that doesn't link to its tickets or code strands the reader.

Rule of thumb: a page should stand on its own for someone who wasn't in the meeting. If a
section wouldn't help that person, cut it — don't pad with empty headings.

## Design / architecture doc template

This is the primary shape for Dakota. Adapt sections to the change; drop any that don't earn
their place. Render it with real Confluence headings (`<h1>`/`<h2>`), a status panel, and a
table of contents so long pages stay navigable.

```
Title: <Component/feature> — <what this doc decides>   e.g. "Resume scoring — identity-alignment design"

[status panel]  Status: Draft | In review | Approved   ·   Owner: <name>   ·   Updated: <date>
[links line]    Jira: DAK-… (epic), DAK-…   ·   Plan: <docs/plans path or link>   ·   Repo/PR: <url>

## Summary
One short paragraph: what this page proposes and who should read it.

## Context / problem
Why this work exists. The current behavior, the pain, the constraint. Enough for a reader
with no prior context to understand the "why".

## Goals / non-goals
- Goals: the outcomes this design must achieve.
- Non-goals: what is explicitly out of scope, so reviewers don't argue the wrong thing.

## Proposed design
The approach. Break into sub-sections per concern (data model, service interactions, API,
failure handling). Reference the affected Dakota modules by name and port (ingestion 8081,
orchestrator 8082, candidate-catalog 8083, scoring 8084, job-posting 8085).

## Architecture / flow
A diagram (Mermaid or an attached image) plus a short narration of the request/data flow —
which service calls which, what crosses S3/DynamoDB/SQS/the OpenAI API, where the
correlation id propagates.

## Alternatives considered
Each option, and why it lost. This is the highest-value section of a design doc — it shows
the decision was deliberate and stops the debate from reopening later.

## Risks / open questions
Known unknowns, migration concerns, things needing a decision. Track open questions as a
task list so they're actionable.

## Rollout / testing
How it ships and how it's verified — feature flag, migration steps, the unit (JUnit 5 +
Mockito) and integration (RestAssured) tests that must exist, per the working agreement.

## References
Links to tickets, the source plan, related pages, external docs.
```

## Body formatting (HTML — recommended)

Confluence pages accept the body as **HTML (storage format)**, `markdown`, or `adf`.
**Prefer HTML**: it's round-trip safe (re-fetching and re-saving won't mangle it),
preserves inline comments, and gives you Confluence's rich blocks through `data-type`
attributes. Use `markdown` only for a throwaway, all-prose page. The pieces you'll actually
reach for on a Dakota design doc:

- **Headings / prose:** `<h1>`–`<h6>`, `<p>`, `<ul>`/`<ol>`/`<li>`, `<table>` with
  `<thead>`/`<tbody>`/`<tr>`/`<th>`/`<td>`.
- **Code blocks** (config, JSON payloads, curl): `<pre><code class="language-json">…</code></pre>`.
  Use the right language class so scoring payloads and YAML render highlighted.
- **Panels** for callouts:
  `<div data-type="panel-info|panel-note|panel-warning|panel-success|panel-error"><p>…</p></div>`.
  A `panel-info` makes a clean status/summary box at the top of the page.
- **Status lozenges** for the doc state:
  `<span data-type="status" data-color="green|yellow|red|blue|neutral|purple">Approved</span>`.
- **Task lists** for open questions / action items:
  `<ul data-type="task-list"><li data-type="task-item"><input type="checkbox"> Decide retry policy</li></ul>`.
- **Decision list** to record decisions:
  `<ul data-type="decision-list"><li data-type="decision-item" data-state="DECIDED">Use SQS fan-out</li></ul>`.
- **Expand** for long detail you want collapsed: `<details><summary>Full schema</summary><p>…</p></details>`.
- **Table of contents / other native macros:** an extension node —
  `<div data-type="extension" data-extension-type="com.atlassian.confluence.macro.core"
  data-extension-key="toc" data-parameters="{}"></div>`. Add a ToC to any page longer than
  a couple of screens.
- **Mermaid diagrams:** Confluence renders Mermaid via the Mermaid macro if the site has it;
  otherwise attach the diagram as an image, or paste the rendered SVG/PNG. When unsure whether
  the macro is installed, put the Mermaid source in a `language-mermaid` code block and note it.

Follow ADF nesting rules (the tool rejects invalid nesting with a descriptive error): task/
decision items and headings are inline-only; list items can't contain headings/tables/panels;
panels can't contain tables/expands/other panels. Never wrap the body in `<html>`/`<head>`/
`<body>`, and don't invent opaque ids (`data-local-id`, media ids) — only copy them from
fetched content, and omit them on new nodes.

## Creating / updating pages — programmatic (Confluence REST API via curl)

Publish with the Confluence Cloud REST API using Basic auth from the `CONFLUENCE_EMAIL` +
`CONFLUENCE_API_TOKEN` env vars (set in `~/.bashrc`; source it first in a non-interactive
shell). This is the token-cheapest path — pipe every response through `jq`/`python3` so you
read back only `id` + `_links.webui`, not the whole page object. `acli confluence page view`
stays available as a read-only fallback; the MCP is not used.

1. **Auth check + orient.** Source creds and confirm 200, then read a parent/existing page:
   ```bash
   source "$HOME/.bashrc"
   AUTH="$CONFLUENCE_EMAIL:$CONFLUENCE_API_TOKEN"; BASE="https://verame.atlassian.net/wiki"
   curl -sS -u "$AUTH" "$BASE/rest/api/user/current" -o /dev/null -w '%{http_code}\n'  # expect 200
   # find an existing page by title (avoid duplicates):
   curl -sS -u "$AUTH" "$BASE/api/v2/spaces/753670/pages?title=My%20Title" \
     | python3 -c 'import sys,json;[print(p["id"],p["title"]) for p in json.load(sys.stdin)["results"]]'
   ```
2. **Build the payload from a file** (never inline a huge body on the command line). Write the
   storage-format HTML to a temp file, then assemble JSON with `python3`/`jq` so quoting is safe:
   ```bash
   # body.html holds the storage-format HTML (no <html>/<body> wrapper)
   python3 - "$PWD/body.html" > /tmp/page.json <<'PY'
   import json,sys
   body=open(sys.argv[1]).read()
   print(json.dumps({
     "spaceId":"753670",
     "status":"current",
     "title":"<TITLE>",
     "parentId":"753823",          # NE homepage, or a specific parent page id
     "body":{"representation":"storage","value":body}
   }))
   PY
   ```
3. **Create** (`POST /wiki/api/v2/pages`), returning only id + URL:
   ```bash
   curl -sS -u "$AUTH" -X POST "$BASE/api/v2/pages" \
     -H "Content-Type: application/json" --data @/tmp/page.json \
     | python3 -c 'import sys,json;d=json.load(sys.stdin);print(d["id"], d["_links"]["base"]+d["_links"]["webui"])'
   ```
4. **Update** an existing page — v2 requires the current `version.number` + a bump:
   ```bash
   VNUM=$(curl -sS -u "$AUTH" "$BASE/api/v2/pages/<PAGE_ID>" | python3 -c 'import sys,json;print(json.load(sys.stdin)["version"]["number"])')
   # PUT /api/v2/pages/<PAGE_ID> with id, status:"current", title, body, and version:{number: VNUM+1}
   ```
   When editing existing content, fetch the current body first and preserve `data-local-id`s
   and inline comments rather than regenerating from scratch.
5. **Cross-link.** Add the published URL to the backing DAK ticket(s) and reference the page
   from the source plan file, so the plan ↔ ticket ↔ page triangle is complete.

Secret hygiene: the token comes only from the env var — never write it to a repo file, a
committed doc, or a payload that gets logged. If a token is ever printed/exposed, flag it for
rotation.

## Workflow

1. **Confirm space & placement** — NE space (id `753670`), and the parent page the doc belongs
   under. Read existing pages first (REST title query above, or `acli confluence page view`);
   prefer updating over creating a near-duplicate.
2. **Draft** the page using the template — always Title + summary + scannable structure; add
   the status panel, links line, alternatives, and a ToC where they help. **Show the user the
   draft for approval** (mirrors the plan-first working agreement).
3. **Build the body as HTML** (storage format) with the right blocks (panels, code blocks,
   task lists, ToC); write it to a file for the payload.
4. **Publish via REST** — `POST`/`PUT` `/wiki/api/v2/pages` with the `CONFLUENCE_*` creds;
   read back only id + URL.
5. **Link** the page to its DAK tickets and source plan/PR, and report the page URL to the user.

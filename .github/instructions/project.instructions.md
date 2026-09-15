---
applyTo: '**'
---

# ctrail — project instructions

These rules are specific to **ctrail**. Cross-project working agreements live in
`~/.claude/CLAUDE.md`, and the reusable skills (`project-plans`, `code-reviews`,
`confluence-pages`, `jira-tickets`) live in `~/.claude/skills/`. Where this file and a
global rule disagree, **this file wins for ctrail**.

---

## What ctrail is

A single-repo **Java 8 command line tool** — a `tail -f` replacement that colors and
filters log output. It is **not** a service: there are no HTTP endpoints, no Micronaut, no
Spring, no database, no cloud infrastructure.

| Fact | Value |
|---|---|
| Group / artifact | `com.kagr.tools` / `ctrail` |
| Current version | `1.1.1` |
| Java | 8 (`maven-compiler-plugin` source/target `1.8`) |
| Build | Maven; `maven-shade-plugin` produces an executable fat jar |
| Entry point | `com.kagr.tools.ctrail.CtrailEntryPoint` |
| Artifact | `target/ctrail-<version>.jar` |
| Repo | single git repo, remote `git@github.com-kevgol0:kevgol0/ctrail.git` |
| Default branch | **`master`** (there is no `main`) |

### Dependencies

`lombok` (provided), `slf4j-api` + `logback-classic` + `jcl-over-slf4j`,
`commons-configuration2`, `commons-beanutils`, `commons-cli`, `commons-io`,
`commons-lang3`. JUnit 4 at test scope.

### Source layout

```
com.kagr.tools.ctrail
├── CtrailEntryPoint      CLI parsing (commons-cli), wiring, startup
├── ConsoleColors         ANSI color constants
├── IShutdownManager      shutdown signalling contract
├── files/
│   ├── FileReaderThread    tails files, applies -m and filters, enqueues lines
│   ├── FileTailTracker     per-file handle + read position
│   ├── StdinReaderThread   single read-loop over piped input
│   └── OutputWriterThread  drains the queue, formats and prints
├── props/
│   ├── CtrailProps         singleton config loaded from XML
│   └── FileSearchFilter    per-source include/exclude keyword matching
└── unit/
    ├── LineFormatter       keyword -> color, builds the output line
    └── LogLine             one line + its source + its filter
```

### Configuration

XML via `commons-configuration2`, resolved in this order — **at least one must exist or
ctrail will not start**:

1. `-DCTRAIL_CFG=/path/to/config.xml` (system property)
2. `./ctrail.xml` (current directory)
3. `/etc/ctrail.xml`

Config covers `<execution>`, `<inputFiles>`, `<coloring>` (colorpairs: keyword ->
fgcolor/flcolor) and `<filtering>` (`<filefilter>` per file, plus `<stdinfilter>` for
piped input; a `<filefilter>` named `stdin` is the deprecated legacy form).

⚠️ **`CtrailProps.getInstance()` is a singleton that caches on the `CTRAIL_CFG` system
property.** It only rebuilds when that property changes. Tests that need a different
config must set the property — and surefire is deliberately configured with
`forkMode=always` / `reuseForks=false` so singleton state cannot leak between test
classes. Do not "optimize" that away.

---

## Build, test, run

```bash
mvn clean test      # run the unit suite
mvn package         # shaded executable jar at target/ctrail-<version>.jar
./bin/ctr           # launcher script
./bin/install.sh    # installs jar + /etc/ctrail.xml + launcher from GitHub Releases
```

- Prefer the **IDE build** (see Tool Usage Priority) to compile and collect errors; drop to
  `mvn` when you need the full surefire run.
- After each build, summarize: error count, warning count, root cause of any failure, and a
  ranked list of suggested fixes (most likely first).
- **Baseline: 52 tests, 0 failures, 0 errors.** If your change lowers that count, say so.
- Test logs intentionally contain `ERROR`/`WARN` lines from negative-path cases — a green
  exit code, not the absence of ERROR lines, is the pass signal.

### Tests — ctrail reality

⚠️ **ctrail uses JUnit 4.** `org.junit.Test`, `org.junit.Assert`, `org.junit.Before`,
`org.junit.Ignore`. There is **no Mockito**, **no RestAssured**, and **no endpoint** to
integration-test. Do not import them, and do not write JUnit 5 (`org.junit.jupiter.*`)
tests — they will not run.

- Every logic-bearing method gets a JUnit 4 unit test. Trivial accessors, Lombok-generated
  code and plain data holders are exempt.
- Test fixtures live in `src/test/resources/` — `configs/` (XML configs), `sources/`
  (sample logs), `expected/` (golden output).
- ⚠️ "Hard to reach in a test" is a design problem to fix, not grounds to skip.
- ⚠️ Verify a new test by deleting the line it covers and watching it go red.
- Tests must pass before a phase is marked `[FINISHED]`.

---

## Tool Usage Priority

For every action (reading, searching, editing, refactoring, building, running, debugging),
select tools in this strict order. Only fall to the next tier when the current tier cannot
perform the task, and state which tier you used and why you dropped down.

1. **IntelliJ IDE first (mandatory default)** — index-aware, syntax-aware, safer:
   - Find/navigate: IDE file search, symbol search, in-file text/regex search — not
     `find`/`grep`.
   - Read/edit: IDE read and in-place edit/replace — not `cat`/`sed`.
   - Refactor: IDE rename refactoring for any symbol rename — never manual find-and-replace.
   - Inspect: IDE file-problems/inspections before and after changes.
   - Build: IDE build/rebuild to compile and collect errors.
   - Run/debug: IDE run configurations and debugger.
2. **GitHub Copilot CLI second** — for AI-assisted shell tasks or command explanation.
3. **Terminal last resort** — prefer running it through the IDE terminal.

### ⚠️ Known environment gotchas (verify your writes)

- The IDE terminal **silently strips blank lines** from heredoc input. A file written that
  way is valid text but broken markdown.
- File read/create tools can serve **stale cache** — reporting success or "file exists"
  while the disk says otherwise.
- After any non-trivial file write, **verify against disk** (e.g. `wc -l`,
  `grep -c '^$'`) rather than trusting the tool's echoed result.

---

## Plan first (non-negotiable)

Before any code execution or file modification, write a plan and **wait for explicit
confirmation** ("go", "approved", "proceed"). Never proceed without one. If you discover a
problem mid-task, **stop**, write up what you found, and ask for instructions.

Follow the `project-plans` skill: plans are built as **Confluence pages under
`java / ctrail / <plan title>`**, with a local markdown file as the fallback. When
publishing is not possible, write the markdown file and **say why it was not published**.

🚫 **Never use the Atlassian MCP integration — it is broken.** Use the Confluence REST API
via `curl` (`CONFLUENCE_EMAIL` / `CONFLUENCE_API_TOKEN`); `acli` is a read-only fallback.

### Document paths (PROJECT = `ctrail`)

| Artifact | Path |
|---|---|
| Plan (local copy/fallback) | `docs/plans/ctrail/plan-TITLE-YYYYMMDD-HHMMSS.md` |
| Modifications log | `docs/modifications/ctrail/modifications-TITLE-YYYYMMDD-HHMMSS.md` |
| Changelog (one file per feature) | `docs/changelog/ctrail/<change\|feature\|fix>-TITLE.md` |
| Internal state | `docs/state/ctrail/` (never surface unless asked) |

`TITLE` is kebab-case with no special characters; timestamp from `date +%Y%m%d-%H%M%S`.
Never append to a monolithic changelog. The modifications log is the source material for
PR descriptions.

There is also a top-level `CHANGELOG.md` for released versions — keep it current on a
release.

---

## Git & PR

- **Single repository.** There are no nested sub-repos, no multi-repo PR script, and no
  enterprise GitHub host. Ignore any instruction referencing `create-prs.sh`,
  `verame.ghe.com`, or a parent repo with nested modules — none of that applies here.
- The default branch is **`master`**. If work starts on `master`, cut an appropriately
  named branch; if already on a branch, keep working there.
- Commit and cut PRs **only when asked**.
- ⚠️ **`gh` is not ready for this repo out of the box.** `gh auth status` shows it
  authenticated to **`verame.ghe.com` only** — a different, unrelated host. ctrail lives on
  **github.com** (`kevgol0/ctrail`). Before using `gh` here, either run
  `gh auth login --hostname github.com` or prefix commands with `GH_HOST=github.com`, and
  confirm with `gh auth status`. Do not assume a `gh` command targets the right host.
- Plain `git push`/`pull` work independently of `gh`, via the `github.com-kevgol0` SSH host
  alias in `~/.ssh/config`.
- Do not commit IDE artifacts — `*.iml` and `.idea` are gitignored; keep it that way.
- Releases publish the shaded jar, `ctrail.xml` and `ctr` to GitHub Releases, which is
  where `bin/install.sh` downloads from.

---

## Coding standards

- **Method size / structure:** small, readable, single responsibility. Correctness and
  readability over cleverness.
- **Code blocks:** organize logic into ~5–7 line blocks, each preceded by a short comment
  describing what the block does.
- **Javadoc** on all public methods; **inline comments on all XML config**.
- **Strings:** use `org.apache.commons.lang3.StringUtils` / `Strings` exclusively — no raw
  `String` method where a StringUtils equivalent exists.
- **Never use streams.** Always explicit iteration. (The existing code is Java 8 but
  deliberately stream-free.)
- **Naming:** descriptive, no unexplained abbreviations; camelCase members, PascalCase
  types. Parameters `final`; the codebase uses a trailing-underscore convention for
  parameters (`line_`, `tracker_`).
- **Lombok:** keep it — `@Getter`/`@Setter`/`@Slf4j`/`@NonNull` are used throughout and
  must not be stripped.
- **Logging:** log a lot, via Lombok `@Slf4j`.
  - ERROR/WARN — operators: failures and anomalous behavior.
  - INFO — analysts/operators: informative, business-level.
  - DEBUG/TRACE — developers: verbose, follow-along detail.
- **Threading:** `FileReaderThread` / `StdinReaderThread` produce into a bounded
  `BlockingDeque`; `OutputWriterThread` consumes. Use `put()` (blocking back-pressure),
  never `add()` — the queue is bounded by `maxPendingLines` and `add()` throws when full,
  which previously killed the reader thread. Keep `LineFormatter.format()` thread-safe by
  using locals, not shared mutable fields.

### Dependency & config changes

Before suggesting a dependency or config change, check for known breaking changes in the
relevant version range — for ctrail that means **commons-configuration2, commons-cli,
logback/slf4j**, not Micronaut or YAML. Note that GitHub Dependabot currently reports
open vulnerabilities on this repo; treat dependency bumps as their own tracked task.

---

## Operational rules

### Iteration limit

Maximum **2 iterations** per user request. If unresolved after two attempts, stop,
summarize the current state, and wait for direction. Do not start a third pass alone.

### Quality bar

Every change is an opportunity to leave the code better than you found it. Do not generate
code and leave it untested — always attempt to verify what you changed. Perform a code
review of your own changes as a **separate, distinct step** (see the `code-reviews` skill).

---

## Context & learning

- **Project knowledge:** record durable facts about ctrail in
  `docs/knowledge/ctrail/knowledge-YYYYMMDD-HHMMSS.md` with a clear title and summary.
  Internal use only unless asked.
- **Knowledge gaps:** if you hit a suspected gap (e.g. a library version with breaking
  changes), log it in `docs/knowledge/ctrail/knowledge-gaps-YYYYMMDD-HHMMSS.md` and ask
  before filling it in.
- **Updating these instructions:** if you learn something durable about ctrail or the
  tooling, update **this file** (`.github/instructions/project.instructions.md`) and log
  the change in
  `docs/modifications/ctrail/modifications-instructions-update-YYYYMMDD-HHMMSS.md`.
- **External tools:** when a task depends on an external tool (the `gh` CLI, the Confluence
  REST API, and so on), read its documentation rather than guessing at flags or behavior.
  If still unsure, ask. Do not assume.

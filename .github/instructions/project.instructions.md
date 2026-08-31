---
applyTo: '**'
---
## Tool Usage Priority

For every action (reading, searching, editing, refactoring, building, running, debugging), select tools in this strict order. Only fall to the next tier when the current tier cannot perform the task, and state which tier you used and why you dropped down.

1. IntelliJ IDE first (mandatory default). Use the IDE's own capabilities before anything else, because they are index-aware, syntax-aware, and safer:
   - Find/navigate: IDE file search, symbol search, and in-file text/regex search — not find/grep.
   - Read/edit: IDE read and in-place edit/replace — not cat/sed.
   - Refactor: IDE rename refactoring for any symbol rename — never a manual find-and-replace.
   - Inspect: IDE file-problems/inspections to check a file before and after changes.
   - Build: IDE build/rebuild (single-file or project) to compile and collect errors — not a raw mvn/npm invocation.
   - Run/debug: IDE run configurations and debugger.
2. GitHub Copilot CLI second. If the IDE cannot do it, use the Copilot CLI (copilot / gh copilot) for AI-assisted shell tasks or command suggestion/explanation.
3. Terminal last resort. Only when neither the IDE nor the Copilot CLI can accomplish the task, run a raw terminal command — and prefer running it through the IDE's terminal so output stays tracked.

---

## Build & Execution

- Plan before acting. Before any code execution or file modification, write a markdown plan to ./docs/plans/PROJECT/plan-TITLE-YYYYMMDD-HHMMSS.md. Wait for explicit user confirmation ("go", "approved", "proceed") before continuing. This is NON-NEGOTIABLE. NEVER PROCEED WITHOUT A PLAN. The title should not contain any special characters.
- Document after: produce a log of what you did, and it should be in ./docs/modifications/PROJECT/modifications-TITLE-YYYYMMDD-HHMMSS.md. The title should not have any special characters. This should be used to generate PRs.
- Log analysis. After each build, summarize: error count, warning count, root cause of any failure, and a ranked list of suggested fixes (most likely first).
- Ecosystem awareness. Before suggesting dependency changes or config updates, check for known Micronaut/YAML breaking changes in the relevant version range.
- Good quality of code: Every change is an opportunity to clean up the code or make it better than how you found it. Make sure code is succinct, direct, and useful. Do not auto-edit. Always write a plan and ask about it. If you find something, stop processing and produce an instruction set of what you found, asking for instructions.
- Working code: Don't generate code and never test. Always attempt to verify what you are working on.

---

## Change Tracking & Documentation
- Git:
  - If the project being worked on is on main, cut a branch appropriately named.
  - if the project being worked on is already on a branch, keep working there.
  - when asked, check files in and cut a PR. Use GH tools to generate PR's
  - PR creation script: Use dakota-infrastructure-builder/scripts/create-prs.sh for batch PR creation across sub-repos. Supports --create-pr (push + open GH PRs), --project <short-name> for single repos, --branch to specify source branch. Short names: shared-components, infra-builder, ingestion, catalog, scoring, orchestrator, job-posting, client-ui, admin-ui.
  - Project structure: Verame.Dakota is a parent repo containing 9 nested git repos (each with its own verame.ghe.com remote). Sub-repos must be committed and pushed independently before the parent. The gh CLI is configured for GHE at verame.ghe.com.
- Changelog location: ./docs/changelog/PROJECT/change-TITLE. The title should not contain any special characters.
  - One file per feature (e.g., feature-auth-service.md).
  - Never append to a monolithic changelog file.
  - Each file must contain: change summary, relevant code snippets, and test results.
- Test requirements (non-negotiable):
  - Every method: JUnit 5 unit test with Mockito.
  - Every endpoint: RestAssured integration test.
  - Tests must pass before a phase is marked [FINISHED].

## Coding Standards
- Method sizes: Try to keep methods small and readable. Prioritize correctness and readability over cleverness.
- Javadoc on all public Java methods. Inline comments on all YAML configurations.
- String manipulation: Use org.apache.commons.lang3.StringUtils and org.apache.commons.lang3.Strings exclusively. Do not use raw Java String methods where a StringUtils equivalent exists.
- Implicit: never use streams. Always use iterations.
- Code blocks: organize code into blocks, b/w 5-7 lines long, where each block is a block of logic... group the appropriate blocks. There should be a comment describing what that block does.
- logging: log a LOT. Always use lombok. Logging should follow the following framework:
  - ERROR/WARN: debuggings and system operators. Used for failures and anomalous behavior.
  - INFO: used for business analysts and standard operators. Should be informative.
  - DEBUG/TRACE: for developers, should be verbose and allow developers to follow along.

---

## Operational State & Error Recovery

State directory: ./docs/state/PROJECT/ (internal only -- never surface these paths in user-facing output unless asked).

### Iteration Limit
Maximum 2 iterations per user request. If the issue is unresolved after 2 attempts, stop, summarize the current state from the progress log, and wait for user direction. Do not attempt a third pass autonomously.

## Context and learning
- Update instructions: If you learn something new about the project, interaction with the user/tools, or any noteworthy interaction notes, update these instructions in .junie/guidelines.md and log the change in ./docs/modifications/PROJECT/modifications-instructions-update-YYYYMMDD-HHMMSS.md.
- Project-specific knowledge: If you learn something new about the project, add it to ./docs/knowledge/PROJECT/knowledge-YYYYMMDD-HHMMSS.md with a clear title and summary. This is for internal use only and should not be shared with users unless explicitly asked.
- Knowledge cutoff: If you encounter an issue that may be due to a knowledge gap (e.g., a new Micronaut version with breaking changes), log the gap in ./docs/knowledge/PROJECT/knowledge-gaps-YYYYMMDD-HHMMSS.md and ask for user input on how to proceed. Do not attempt to fill the gap autonomously.
- External tools: when building plans on external tools, do not hallucinate or assume (openAI tools, GH CLI, Pawnd etc.). Read their documentation. If you still don't know, ask the user for direction. Do not attempt to guess or assume how they work. Always verify with documentation or user input.

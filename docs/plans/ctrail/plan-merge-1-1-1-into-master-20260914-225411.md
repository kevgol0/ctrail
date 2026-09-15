# Plan: Merge 1.1.1 bugfix work into master

- **Status:** AWAITING APPROVAL
- **Created:** 20260914-225411
- **Project:** ctrail
- **Author:** GitHub Copilot (agent)

## Goal

Pull the latest from origin and merge the version 1.1.1 work
(`origin/worktree-bugfix-1.1.1`) into the local mainline branch `master`, then
publish the result to `origin/master`. Preserve all existing repo files,
including the `.claude/` skills, `.github/instructions/`, and `docs/` files that
currently exist only on `master`.

## Current state (verified)

- Local branch: `master`, clean working tree, up to date with `origin/master`.
- Mainline branch is `master`; there is no `main`.
- `git fetch --all --prune` already run.
- Branches have **diverged** from merge base `de47d40`:
  - `master` has 7 unique commits (README rewrite, install.sh/ctr fixes,
    12-bug fix, stdin example config, PR #9, project instructions).
  - `origin/worktree-bugfix-1.1.1` has 2 unique commits
    (`2cb0bdf` release 1.1.1 filtering/stdin/shutdown fixes,
    `ce73bf2` `<stdinfilter>` for piped input).
- The `.claude/`, `.github/instructions/`, and `docs/` files exist **only** on
  master (added after the merge base). The 1.1.1 branch never had or deleted
  them, so a normal merge will NOT remove them.
- Overlapping files changed on both sides (conflict candidates):
  `pom.xml`, `README.md`, `etc/ctrail.xml`, `bin/ctr`, `bin/install.sh`,
  and Java under `props/` and `files/`.

## Decisions (from user)

- Merge into `master` (not a new `main`).
- Use a real **merge commit** (`git merge`) to keep the correct commits from
  both histories — no rebase / no history rewrite.
- Keep the master-only instruction/skill/docs files (handled automatically).
- Push `master` to `origin` after a successful, verified merge.

## Steps

1. Create a safety backup ref/tag of current `master` (`pre-merge-1.1.1-backup`).
2. Ensure local `master` matches `origin/master` (fast-forward if needed).
3. Run `git merge --no-ff origin/worktree-bugfix-1.1.1` with a descriptive
   message.
4. If conflicts arise, resolve each file, favoring:
   - 1.1.1's functional bugfix/stdinfilter code for Java + `etc/ctrail.xml`.
   - master's newer README/docs where they are strictly additive.
   - `pom.xml` version → `1.1.1`.
   Resolve using the IDE where practical.
5. Confirm master-only files (`.claude/`, `.github/instructions/`, `docs/`)
   still present post-merge.
6. Build + run tests via Maven to verify the merged tree compiles and passes.
7. Report error/warning counts and root causes if any.
8. On green: push `master` to `origin/master`.
9. Write modifications log to
   `./docs/modifications/ctrail/`.

## Test plan

- `mvn -q clean test` (or IDE build + test run) must pass.
- Verify the merged code contains both the 1.1.1 stdin-filter feature and the
  master-side README/instructions.

## Rollback

- If the merge goes wrong before pushing:
  `git merge --abort` (mid-merge) or
  `git reset --hard pre-merge-1.1.1-backup`.
- After push (if needed): revert the merge commit with `git revert -m 1 <sha>`
  or force-restore from the backup tag (coordination required).

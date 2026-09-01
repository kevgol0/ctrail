# Plan: Fix Commits, README, and Install Script

**Date:** 2026-09-01  
**Status:** [DRAFT]

---

## Phase 1: Amend Commit Message & Push [PENDING]

**What:** Amend the message on `834a981` to be more descriptive, then push to `origin/master`.

- `git commit --amend` with a better message covering: project instructions, install script version bump, pom.xml lombok update, case-insensitive search improvements, LineFormatter refactor
- `git push origin master` (force push since we're amending)

---

## Phase 2: Fix `bin/install.sh` [PENDING]

**What:** Fix bugs and modernize the install script.

| Issue | Fix |
|---|---|
| Typo: `/ec/$FILE` → `/etc/$FILE` | Line 13 |
| Hardcoded version `1.1.0` | Parameterize from pom.xml or accept as arg |
| AWS S3 source | Switch to GitHub releases: `https://github.com/kevgol0/ctrail/releases/download/v${VERSION}/ctrail-${VERSION}.jar` |
| Install path `/usr/local/lib` | Change to `/usr/local/share/` for macOS |

Also download `ctr` script from GitHub releases.

---

## Phase 3: Fix `bin/ctr` [PENDING]

**What:** Update the launcher script to use the installed jar path.

- Change `./target/ctrail-1.0.0.jar` → `/usr/local/share/ctrail.jar`
- Remove version hardcoding (symlink handles it)

---

## Phase 4: Rewrite `README.md` [PENDING]

**What:** Full rewrite with sections in this order:

1. **Project description** — what ctrail is
2. **Configuration** — config file search order, XML reference with available colors, filtering, execution settings
3. **Usage** — CLI options (`-e`, `-m`, `-f`, `-v`, `-h`, `--version`), examples (pipe from stdin, tail files)
4. **Installation** — prerequisites (Java 8+), `install.sh` usage, manual install steps
5. **Building** — `mvn package`, output jar location

---

## Phase 5: Commit & Push [PENDING]

- Commit README, install.sh, ctr fixes as separate commits
- Push to master

---

## Definition of Done

- [ ] Commit `834a981` has a descriptive message
- [ ] `bin/install.sh` has no typos, uses GitHub releases, parameterized version, correct macOS paths
- [ ] `bin/ctr` points to installed jar location
- [ ] `README.md` covers config/usage/install/build
- [ ] All changes pushed to `origin/master`

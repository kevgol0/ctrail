# Modifications: Fix Commits, README, and Install Script

**Date:** 2026-09-01

## Changes

### 1. Amended commit `834a981` → `618a18d`
- Rewrote vague message to describe all changes: project instructions, dependency updates, search improvements

### 2. `bin/install.sh` — rewritten
- **Bug fix:** `/ec/$FILE` → `/etc/$FILE`
- **Parameterized version:** accepts CLI arg or reads from `pom.xml`
- **Source:** AWS S3 → GitHub Releases (`github.com/kevgol0/ctrail/releases`)
- **Path:** `/usr/local/lib` → `/usr/local/share/` (macOS convention)
- Added `set -e`, proper quoting, cleanup of temp files

### 3. `bin/ctr` — updated
- Jar path: `./target/ctrail-1.0.0.jar` → `/usr/local/share/ctrail.jar`
- Proper quoting, `$@` instead of `$*`

### 4. `README.md` — full rewrite
- Sections: Configuration (with XML reference) → Usage (CLI options table, examples) → Installation (prerequisites, install.sh, file locations) → Building

## Commits
| Hash | Message |
|------|---------|
| `618a18d` | Add project instructions, update dependencies, and improve search |
| `5af10ae` | Fix install.sh and ctr launcher |
| `f7759c2` | Rewrite README with config, usage, install, and build sections |

All pushed to `origin/master`.

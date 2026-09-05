# Modifications: Add stdin example config

**Date:** 2026-09-03  
**Branch:** `feature/stdin-example-config`

## Changes

| File | Action | Description |
|------|--------|-------------|
| `etc/ctrail-stdin-example.xml` | Added | Example config with a `<filename>stdin</filename>` filefilter showing include/exclude keyword filtering for piped input |
| `README.md` | Updated | Added stdin-with-config usage example and a "Stdin filtering" subsection pointing to the example config |

## Build

- `mvn package` — **0 errors, 0 warnings**

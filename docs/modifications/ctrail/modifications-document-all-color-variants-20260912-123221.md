# Modifications: Document All Color Variants

**Date:** 2026-09-12  
**Branch:** `feature/stdin-example-config`

## Changes

| File | Change |
|------|--------|
| `src/main/java/.../CtrailProps.java` | Added 41 new `case` entries in `getColorCode()` for bold, bright, background, bold-bright, background-bright, and orange color names |
| `README.md` | Replaced single-line color list with a full table of all 7 categories + 5 XML config examples |
| `etc/ctrail.xml` | Expanded the color-list comment to include all supported variants |

## Verification

- `mvn package` — build succeeded, all tests passed.

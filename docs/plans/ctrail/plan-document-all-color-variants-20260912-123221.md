# Plan: Document All Color Variants

## Concise View

### Problem
The README and `etc/ctrail.xml` only document regular + underlined colors. `ConsoleColors.java` defines 7 additional categories (bold, bright, background, bold-bright, background-bright, orange, purple) but `getColorCode()` doesn't map them and the docs don't mention them.

### Scope
**Documentation-only** — update README.md and `etc/ctrail.xml` to list all color variants that `getColorCode()` currently supports, plus the new ones we'll add.

**Code change** — add missing `case` entries in `CtrailProps.getColorCode()` so the new color names actually work at runtime.

### Changes

| # | File | What |
|---|------|------|
| 1 | `CtrailProps.java` | Add `case` entries for `*_BOLD`, `*_BRIGHT`, `*_BACKGROUND`, `*_BOLD_BRIGHT`, `*_BACKGROUND_BRIGHT`, and `ORANGE` |
| 2 | `README.md` | Replace the single "Available colors" line with a full table of all variants + config examples |
| 3 | `etc/ctrail.xml` | Update the color-list comment to include all variants |

### Verification
- Build with `mvn package` to confirm compilation
- Run existing unit tests

---

## Detailed View

### 1. CtrailProps.getColorCode() — new cases

Add switch cases for every constant in `ConsoleColors.java` that isn't already handled:

- **Bold:** `BLACK_BOLD`, `RED_BOLD`, `GREEN_BOLD`, `YELLOW_BOLD` (alias for current `GREEN` which oddly maps to `GREEN_BOLD`), `BLUE_BOLD`, `PURPLE_BOLD`, `CYAN_BOLD`, `WHITE_BOLD`
- **Bright:** `BLACK_BRIGHT`, `RED_BRIGHT`, `GREEN_BRIGHT`, `YELLOW_BRIGHT`, `BLUE_BRIGHT`, `PURPLE_BRIGHT`, `CYAN_BRIGHT`, `WHITE_BRIGHT`
- **Background:** `BLACK_BACKGROUND`, `RED_BACKGROUND`, `GREEN_BACKGROUND`, `YELLOW_BACKGROUND`, `BLUE_BACKGROUND`, `PURPLE_BACKGROUND`, `CYAN_BACKGROUND`, `WHITE_BACKGROUND`
- **Bold Bright:** `BLACK_BOLD_BRIGHT`, `RED_BOLD_BRIGHT`, `GREEN_BOLD_BRIGHT`, `YELLOW_BOLD_BRIGHT`, `BLUE_BOLD_BRIGHT`, `PURPLE_BOLD_BRIGHT`, `CYAN_BOLD_BRIGHT`, `WHITE_BOLD_BRIGHT`
- **Background Bright:** `BLACK_BACKGROUND_BRIGHT`, `RED_BACKGROUND_BRIGHT`, `GREEN_BACKGROUND_BRIGHT`, `YELLOW_BACKGROUND_BRIGHT`, `BLUE_BACKGROUND_BRIGHT`, `PURPLE_BACKGROUND_BRIGHT`, `CYAN_BACKGROUND_BRIGHT`, `WHITE_BACKGROUND_BRIGHT`
- **Special:** `ORANGE` (maps to `ConsoleColors.ORANGE`, which is bold yellow `\033[1;33m`)

Note: `GREEN` currently maps to `GREEN_BOLD` — this appears intentional and will be preserved.

### 2. README.md

Replace the one-liner:
> **Available colors:** `BLACK`, `BLUE`, ...

With a structured table showing all 7 categories, plus XML config examples demonstrating bold, bright, background, and combined usage.

### 3. etc/ctrail.xml

Expand the comment block inside `<linecolors>` to list all supported color names.

# Changelog

## Unreleased

### Added

* **`<filtering><stdinfilter>`** — filters piped input, so `cat app.log | ctr` and
  `kubectl logs -f pod | ctr` honor the same `<includes>`/`<excludes>` machinery files have always
  had. It takes no `<filename>`; there is only one standard in.

  Previously the only way to filter a pipe was an undocumented special case: a `<filefilter>` whose
  `<filename>` was literally `stdin`, matched by exact key lookup. So `stdin` worked while `stdin*`
  and `STDIN` were silently ignored. That form still works for back-compat but is deprecated;
  `<stdinfilter>` wins when both are present.

### Fixed

* **`-f false` did not disable filtering for piped input.** Files were gated on
  `isEnabledFileFiltering()`, the stdin path was not, so `-f false` turned filters off for files and
  left them on for pipes. Both paths now resolve through `CtrailProps.resolveStdinFilter()`.
* **`prependFilenameToLine` was ignored for piped input.** The source name was hardcoded, so stdin
  emitted `stdin: some line` even with the setting false, while files correctly emitted bare lines.


## 1.1.1

Bug-fix release. No config file changes are required; `<filtering><excludesEnabled>` is new and
defaults to the previous behavior.

### Fixed

* **Standard in produced no output at all** unless `-m` or a `stdin` filter was configured. The read
  loop branched filter-or-match and never read the stream when neither was supplied, so a plain
  `cat app.log | ctr` printed nothing.
* **`-m` was ignored whenever a `stdin` filter was configured** — the two code paths were mutually
  exclusive. Both are now applied.
* **`-m` always matched case-sensitively**, ignoring `useCaseSensitiveSarch`.
* **`<includes>` had no effect when tailing files.** The include list was only ever consulted for
  standard in, so a filter written as a strict allow-list still printed every line of the file.
  `fileFilterDefaultsToInclude` was likewise inert for files.
* **Idle CPU spun at ~100% when a tailed file's trailing lines were filtered out.** Bytes consumed by
  a dropped line were never credited to the read position, so the reader believed data was always
  pending and never slept. Measured 98% CPU before, 0% after.
* **The process could hang on shutdown instead of exiting.** The writer parked in a blocking `take()`
  while shutdown only flipped a flag, leaving a non-daemon thread alive forever. The loop now polls
  and re-checks.
* **`_shouldContinue` was not `volatile`**, so the writer could miss the shutdown signal entirely.
* **The last queued line was dropped on shutdown.** The drain loop sized itself at `size() - 1`; with
  a single pending line nothing was flushed at all.
* **The shutdown backlog printed in reverse order** — the drain used `pollLast()`.
* **Output could be lost to buffering** on a redirected stream; the writer now flushes on exit.
* **`-v/--exclude-filters` set the same flag as `-f/--filters`**, making the two options
  indistinguishable. `-v` now toggles only the exclude terms, via the new
  `<filtering><excludesEnabled>` setting.
* **A config with exactly one `<colorpair>` loaded no colors.** commons-configuration returns a bare
  String rather than a collection for a single element; the raw cast threw and left the pair count at
  zero.
* **An unrecognized `defaultFgColor` emitted the literal text `null`** as a prefix on every line.
  It now falls back to white.
* **Duplicate `<filefilter>` entries were not detected.** The check used `Hashtable.contains()`,
  which tests values rather than keys, and compared the raw name against a regex-keyed map.
* **Regex metacharacters in a `<filename>` pattern were not escaped**, so a name such as
  `app(1).log` compiled into a different pattern and matched the wrong files. `*` remains the only
  wildcard.
* **A bounded output queue threw `IllegalStateException` once full**, killing the reader thread
  before it could signal shutdown and hanging the process. Readers now block for back-pressure.
* **`--version` threw on a jar with no `Implementation-Version`**; it reports `UNK`.
* Two malformed log statements: one with two placeholders and a single argument, and one that never
  printed the offending color name.
* Keyword case-folding now uses `Locale.ROOT` consistently, so matching does not change under a
  Turkish-locale JVM.
* `install.sh`: the guard protecting an existing config tested `/ec/ctrail.xml` instead of
  `/etc/ctrail.xml`, so every install silently overwrote the user's config. `chmod +x` also ran
  without `sudo` against a root-owned path.

### Changed

* `commons-lang3` is now declared explicitly rather than relied on transitively via
  `commons-configuration2`.
* `LineFormatter` no longer re-resolves the config file for every output line, which cost up to three
  filesystem `exists()` calls per line, and scans keywords over an array instead of a `LinkedList`.
* Help text for `-f` and `-v` now describes what those options actually do.

### Added

* `<filtering><excludesEnabled>` config setting, backing `-v/--exclude-filters`.
* Regression tests for the above: 42 tests, up from 18.
* Expanded README covering CLI options, every config element, and filter evaluation order.


## 1.1.0

* Exclude filters, multiple keywords per color pair, per-file filtering.

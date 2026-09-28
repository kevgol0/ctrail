# Changelog

## 1.2.1

### Fixed

* **`-e/--entirefile` was silently overridden by `-n/--lines`.** `-e` was applied before `-n`, so
  `-n` overwrote the `setTailLastLines(0)` it had just performed and `ctr -e -n 50` showed 50 lines
  where the README promises the whole file. Argument order made no difference. `-e` is now applied
  last and wins. (CTRAIL-6)

* **`ctr -n <value above Integer.MAX_VALUE>` crashed on startup.** The guard used
  `StringUtils.isNumeric`, which tests digit-ness, then parsed with `Integer.parseInt`, which tests
  range; the resulting `NumberFormatException` escaped `catch (ParseException)` and killed the run.
  The parse is now the guard. Negative values are rejected too, since a negative count would
  otherwise be read as "disabled". (CTRAIL-7)

* **Elapsed times rendered in locale-specific digits.** `DurationFormatter` called `String.format`
  with no `Locale`, so the startup banner and idle notices printed `४m १२s` on a Devanagari-default
  JVM — and `DurationFormatterTest`, which asserts the ASCII forms, failed outright there. All
  three calls now pass `Locale.ROOT`. (CTRAIL-12)

### Documentation

* **Filters are allow-list *or* deny-list, not both.** `fileFilterDefaultsToInclude` also decides
  for a `<filefilter>` that declares no `<includes>`, so an excludes-only filter emits nothing under
  the shipped `false` default. That is deliberate; the supported way to write a deny-list is to set
  the flag `true` and use `<excludes>` alone. The flag is global, so one config cannot mix the two
  styles. Reported as CTRAIL-2 and closed as working-as-designed, with tests pinning all four cases.

### Internal

* `OutputWriterThread`'s exit `flush()` was untested — the harness used an auto-flushing
  `PrintStream` and flushed again before reading, i.e. exactly the case the flush does not apply to.
  Deleting the production line left every test green; it now fails 5 of 6. (CTRAIL-18)

## 1.2.0

### Added

* **File liveness signals.** ctrail can now tell you whether what it is watching is actually moving,
  for files and stdin alike:
  * `execution.tailLast` history on open — line-accurate, replacing the byte offset that almost
    always opened mid-line
  * `execution.showStartupBanner` — one line per input giving size and last-modified age
  * `execution.idleNoticeSeconds` — `no movement in 30s`, repeating, paired with `resumed after ...`
  * `coloring.noticeColor` — ctrail's own messages, immune to keyword colouring
  * new `-n/--lines N` flag mirroring `tail -n`; `-e` zeroes tail-N as well as the byte skip

  ⚠️ **Behaviour change:** an install that never set `skipAheadInBytes` goes from "the last ~1000
  bytes" to "the last 10 lines" on open.

### Security

* Dependency sweep clearing all 8 Dependabot alerts — the CRITICAL and all three HIGHs:
  `commons-configuration2` 2.7 → 2.15.0, `commons-io` 2.7 → 2.20.0, `commons-lang3` 3.12.0 → 3.20.0,
  `logback-classic` 1.2.3 → 1.2.13, `commons-beanutils` 1.9.4 → 1.11.0.

  The last alert needed `commons-configuration2` 2.15.0, which initially failed with
  `NoSuchMethodError` — not its fault. `IOSupplier.getUnchecked()` arrived in commons-io **2.17.0**
  and our own explicit pin was holding commons-io at 2.14.0, below the floor. `dependency:tree` does
  not show optional dependencies, which is why the requirement was invisible.

### Fixed

* **A file matching no `<filefilter>` showed nothing.** `fileFilterDefaultsToInclude=false` — the
  shipped value — made `FileTailTracker` exclude every line of a source with no filter attached.
  Pointing ctrail at a log is its primary use, so a default install produced an empty screen.

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

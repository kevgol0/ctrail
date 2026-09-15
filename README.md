# ctrail

**Color Trail** — a `tail -f` replacement that produces customizable, colored terminal output. It reads from files or stdin and supports per-directory configuration.

---

## Configuration

ctrail looks for its XML config file in this order:

```
Java System Property  →  -DCTRAIL_CFG=/path/to/config.xml
Current directory     →  ./ctrail.xml
System config         →  /etc/ctrail.xml
```

At least one must be present or ctrail will not start. Place a `ctrail.xml` in any directory to customize coloring and filtering for that project.

### `<execution>`

| Element | Default | Description |
| --- | --- | --- |
| `maxProcessingLines` | 1000 | Lines a reader may enqueue before yielding to the other files. |
| `maxPendingLines` | 100000 | Bound on the pending output queue. Readers block once it fills. |
| `prependFilenameToLine` | true | Prefix each line with its source filename. |
| `skipAheadInBytes` | 1000 | On startup, begin this many bytes from the end of each file. `0` reads the whole file. |
| `noChangeSleepTimeMillis` | 100 | Idle sleep when no file has advanced. |
| `matchFirstWord` | true | `true` colors by the FIRST matching keyword; `false` by the LAST. With `false` the config order of keywords decides. |
| `useCaseSensitiveSarch` | false | Applies to coloring keywords, `-m`, and both filter lists. |

### `<inputFiles>`

| Element | Default | Description |
| --- | --- | --- |
| `maxInputFileCount` | 100 | Files opened concurrently; extra arguments are ignored with a warning. |

### `<coloring>`

Recognized color names:

```
BLACK   BLUE   CYAN   GREEN   PURPLE   RED   WHITE   YELLOW
```

each also available with a `_UNDERLINED` suffix (`BLUE_UNDERLINED`, …). An unrecognized name logs a
warning and falls back to the default foreground color.

```xml
<coloring>
    <filename>
        <blankLineOnFileChange>false</blankLineOnFileChange>
    </filename>

    <linecolors>
        <defaultFgColor>white</defaultFgColor>

        <colorpair>
            <keyword>(err)</keyword>   <!-- repeatable: several keywords may share a pair -->
            <fgcolor>red</fgcolor>     <!-- color of the log line -->
            <flcolor>red_underlined</flcolor>  <!-- optional: color of the filename prefix -->
        </colorpair>
    </linecolors>
</coloring>
```

### `<filtering>`

Filters decide, per line, whether it is printed. A filter applies either to files (matched by name)
or to piped input.

```xml
<filtering>
    <enabled>true</enabled>
    <excludesEnabled>true</excludesEnabled>
    <fileFilterDefaultsToInclude>false</fileFilterDefaultsToInclude>

    <!-- applies when reading from a pipe: `cat app.log | ctr` -->
    <stdinfilter>
        <includes>
            <keyword>geo-lookup</keyword>
        </includes>
        <excludes>
            <keyword>heartbeat</keyword>
        </excludes>
    </stdinfilter>

    <!-- applies to files whose name matches -->
    <filefilter>
        <filename>cityspark*.log.0</filename>
        <includes>
            <keyword>geo-lookup</keyword>
        </includes>
        <excludes>
            <keyword>heartbeat</keyword>
        </excludes>
    </filefilter>
</filtering>
```

| Element | Default | Description |
| --- | --- | --- |
| `enabled` | true | Master switch, overridden by `-f`. When false, no filter is attached to any source — files or stdin — and every line is shown. |
| `excludesEnabled` | true | Applies the `<excludes>` lists, overridden by `-v`. Turn off to keep includes while ignoring excludes. |
| `fileFilterDefaultsToInclude` | true | Verdict for a line that matched neither list, on a source that HAS a filter. Set `false` to make the filter a strict allow-list. |
| `stdinfilter` | — | Filter for piped input. Takes no `<filename>`; there is only ever one standard in. |
| `filefilter` | — | Repeatable. Filter for files matching `<filename>`. |

`<filename>` matching: `*` is a wildcard, every other character is a literal (so `.`, `(`, `+` and
friends match themselves). The pattern is anchored to the end of the name, and matches the file's
base name, not the full path.

Evaluation order for each line:

1. `-m/--match`, if given. A non-matching line is dropped.
2. The source's `<excludes>`. A match drops the line.
3. The source's `<includes>`. A match keeps the line.
4. Otherwise `fileFilterDefaultsToInclude` decides.

A source with no filter of its own is never filtered — every line is shown. That means files with no
matching `<filefilter>`, and piped input when no `<stdinfilter>` is declared.

> Before `<stdinfilter>` existed, piped input could only be filtered by a `<filefilter>` whose
> `<filename>` was literally `stdin`. That still works, but is deprecated — prefer `<stdinfilter>`,
> which wins if both are present.

### Config reference

```xml
<ctrail>
  <inputFiles>
    <maxInputFileCount>1000</maxInputFileCount>
  </inputFiles>

  <execution>
    <maxProcessingLines>1000</maxProcessingLines>
    <maxPendingLines>100000</maxPendingLines>
    <prependFilenameToLine>true</prependFilenameToLine>
    <skipAheadInBytes>1000</skipAheadInBytes>
    <noChangeSleepTimeMillis>100</noChangeSleepTimeMillis>
    <matchFirstWord>false</matchFirstWord>
    <useCaseSensitiveSarch>false</useCaseSensitiveSarch>
  </execution>

  <coloring>
    <filename>
      <blankLineOnFileChange>false</blankLineOnFileChange>
    </filename>
    <linecolors>
      <defaultFgColor>white</defaultFgColor>
      <colorpair>
        <keyword>(err)</keyword>
        <fgcolor>red</fgcolor>
      </colorpair>
      <!-- add more colorpairs as needed -->
    </linecolors>
  </coloring>

  <filtering>
    <enabled>true</enabled>
    <fileFilterDefaultsToInclude>false</fileFilterDefaultsToInclude>
    <filefilter>
      <filename>app*.log</filename>
      <includes>
        <keyword>ERROR</keyword>
      </includes>
      <excludes />
    </filefilter>
  </filtering>
</ctrail>
```

### Available colors

Color names are **case-insensitive** in the XML config.

| Category | Values |
|----------|--------|
| **Regular** | `BLACK`, `RED`, `GREEN`, `YELLOW`, `BLUE`, `PURPLE`, `CYAN`, `WHITE` |
| **Bold** | `BLACK_BOLD`, `RED_BOLD`, `GREEN_BOLD`, `ORANGE` / `YELLOW_BOLD`, `BLUE_BOLD`, `PURPLE_BOLD`, `CYAN_BOLD`, `WHITE_BOLD` |
| **Underlined** | `BLACK_UNDERLINED`, `RED_UNDERLINED`, `GREEN_UNDERLINED`, `YELLOW_UNDERLINED`, `BLUE_UNDERLINED`, `PURPLE_UNDERLINED`, `CYAN_UNDERLINED`, `WHITE_UNDERLINED` |
| **Bright** (high intensity) | `BLACK_BRIGHT`, `RED_BRIGHT`, `GREEN_BRIGHT`, `YELLOW_BRIGHT`, `BLUE_BRIGHT`, `PURPLE_BRIGHT`, `CYAN_BRIGHT`, `WHITE_BRIGHT` |
| **Bold Bright** | `BLACK_BOLD_BRIGHT`, `RED_BOLD_BRIGHT`, `GREEN_BOLD_BRIGHT`, `YELLOW_BOLD_BRIGHT`, `BLUE_BOLD_BRIGHT`, `PURPLE_BOLD_BRIGHT`, `CYAN_BOLD_BRIGHT`, `WHITE_BOLD_BRIGHT` |
| **Background** | `BLACK_BACKGROUND`, `RED_BACKGROUND`, `GREEN_BACKGROUND`, `YELLOW_BACKGROUND`, `BLUE_BACKGROUND`, `PURPLE_BACKGROUND`, `CYAN_BACKGROUND`, `WHITE_BACKGROUND` |
| **Background Bright** | `BLACK_BACKGROUND_BRIGHT`, `RED_BACKGROUND_BRIGHT`, `GREEN_BACKGROUND_BRIGHT`, `YELLOW_BACKGROUND_BRIGHT`, `BLUE_BACKGROUND_BRIGHT`, `PURPLE_BACKGROUND_BRIGHT`, `CYAN_BACKGROUND_BRIGHT`, `WHITE_BACKGROUND_BRIGHT` |

> `ORANGE` is an alias for `YELLOW_BOLD` (bold yellow).

#### Examples

Bold red for errors:
```xml
<colorpair>
  <keyword>ERROR</keyword>
  <fgcolor>red_bold</fgcolor>
</colorpair>
```

Bright cyan for debug lines:
```xml
<colorpair>
  <keyword>(dbg)</keyword>
  <fgcolor>cyan_bright</fgcolor>
</colorpair>
```

Red background for critical alerts:
```xml
<colorpair>
  <keyword>FATAL</keyword>
  <fgcolor>red_background</fgcolor>
</colorpair>
```

Bold-bright green for success messages:
```xml
<colorpair>
  <keyword>SUCCESS</keyword>
  <fgcolor>green_bold_bright</fgcolor>
</colorpair>
```

Combined foreground and filename colors:
```xml
<colorpair>
  <keyword>WARN</keyword>
  <fgcolor>orange</fgcolor>
  <flcolor>yellow_underlined</flcolor>
</colorpair>
```

---

## Usage

```bash
# tail files in the current directory
ctr /var/log/*.log

# pipe from stdin
some-command | ctr

# pipe from stdin with filtering (uses the "stdin" filefilter in config)
CTRAIL_CFG=etc/ctrail-stdin-example.xml some-command | ctr

# read entire file (not just tail)
ctr -e /var/log/app.log

# filter lines matching a string
ctr -m "ERROR" /var/log/app.log

# enable/disable include filters (overrides config)
ctr -f true /var/log/app.log

# enable/disable exclude filters (overrides config)
ctr -v true /var/log/app.log

# show help / version
ctr -h
ctr --version
```

### CLI options

| Flag | Long | Description |
|------|------|-------------|
| `-e` | `--entirefile` | Process the entire file, not just new lines |
| `-m` | `--match STR` | Only show lines matching STR |
| `-f` | `--filters` | Enable/disable include filters (`true`/`false`) |
| `-v` | `--exclude-filters` | Enable/disable exclude filters (`true`/`false`) |
| `-h` | `--help` | Print help |
|      | `--version` | Show version |

### Stdin filtering

When no file arguments are given, ctrail reads from stdin and applies the `<filefilter>` whose `<filename>` is `stdin`. This lets you include/exclude lines from piped output using the same keyword filtering available for files.

See [`etc/ctrail-stdin-example.xml`](etc/ctrail-stdin-example.xml) for a ready-to-use example config.

---

## Installation

### Prerequisites

- Java 8 or later

### Quick install

```bash
# install the latest release (version read from pom.xml)
./bin/install.sh

# or specify a version explicitly
./bin/install.sh 1.1.0
```

The install script downloads from [GitHub Releases](https://github.com/kevgol0/ctrail/releases) and places files at:

| File | Location |
|------|----------|
| Launcher script | `/usr/local/bin/ctr` |
| Library jar | `/usr/local/share/ctrail-X.X.X.jar` → symlinked to `ctrail.jar` |
| Default config | `/etc/ctrail.xml` (only if not already present) |

---

## Building

```bash
mvn package
```

The shaded (fat) jar is produced at `target/ctrail-<version>.jar`.

---

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

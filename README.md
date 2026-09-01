# ctrail

Color Trail: a tool that expands on the functionality of `tail -f` by producing customizable,
colored output. It reads from files or standard in, and can be customized per directory.


## Usage

```
ctr [options] [file ...]
```

With no file arguments `ctrail` reads standard in, so it drops into a pipeline:

```
cat app.log | ctr
kubectl logs -f mypod | ctr -m timeout
ctr /var/log/app.log /var/log/other.log
```

### Command line options

| Option | Long form | Argument | Description |
| --- | --- | --- | --- |
| `-e` | `--entirefile` | | Read from the start of the file instead of skipping ahead to the tail. Equivalent to `skipAheadInBytes` = 0. |
| `-m` | `--match` | `STR` | Only show lines containing `STR`. Honors `useCaseSensitiveSarch`. |
| `-f` | `--filters` | `true\|false` | Enable/disable per-file filtering entirely. Overrides `<filtering><enabled>`. |
| `-v` | `--exclude-filters` | `true\|false` | Enable/disable only the `<excludes>` terms, leaving `<includes>` active. Overrides `<filtering><excludesEnabled>`. |
| `-h` | `--help` | | Print command line directives. |
| | `--version` | | Show version. |


## Configuration / Customization

### Per execution configuration

The order of searching for a valid config file is:

```
	-> Java System Property: -DCTRAIL_CFG=XXX
		-> current working directory (./ctrail.xml)
			-> system config file (/etc/ctrail.xml)
```

In other words, it looks in the current working directory for any localization files; if not found,
it looks in `/etc/ctrail.xml`. At least one of the config entries must be present, or the system will
not run.

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


## Files

### Installation locations
* bash script: `ctr`, usually located in `/usr/local/bin/`
* library file:
    * usually located in: `/usr/local/lib/ctrail-XXX.jar`
    * linked (ln -s) to `ctrail.jar`

* config file: `/etc/ctrail.xml`
    * any local configs you want


## Building

```
mvn package          # produces target/ctrail-<version>.jar (shaded, executable)
mvn test             # runs the unit suite
```

Requires JDK 8 or later.


## Changelog

See [CHANGELOG.md](CHANGELOG.md).

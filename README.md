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

**Available colors:** `BLACK`, `BLUE`, `CYAN`, `DEFAULT`, `GREEN`, `RED`, `WHITE`, `YELLOW` — each also available as `*_UNDERLINED` (e.g. `RED_UNDERLINED`).

---

## Usage

```bash
# tail files in the current directory
ctr /var/log/*.log

# pipe from stdin
some-command | ctr

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
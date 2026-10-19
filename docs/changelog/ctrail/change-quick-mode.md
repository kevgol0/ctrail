# Change: quick mode, `ctr -q REGEX` (CTRAIL-22) — 1.5.0

## Summary

`-q/--quick REGEX` (repeatable) shows every line and colours whole lines matching a case-sensitive
regex, one colour per pattern, first match wins. `ctrail.xml` is never read and every other option
is ignored. Any argument parse error is now a usage error (stderr, exit 2).

## Code

```java
// CtrailEntryPoint.loadArgsAndOverrides(): arguments are parsed BEFORE any config is loaded
handleInfoOptions(options, line);          // -h / --version exit
if (line.hasOption("q")) applyQuickMode(line);       // installs quick config, never reads the file
else                     applyStandardOverrides(line);
```

```java
// CtrailProps.installQuickDefaults(): a defaults-only instance with the patterns, pinned as the singleton
final CtrailProps quick = new CtrailProps(patterns_, multipleFiles_);   // reads no file
_instance = quick;
_instanceCfgOverride = System.getProperty(CTRAIL_CFG_KEY);
```

```java
// LineFormatter.format(): quick patterns replace keyword colouring
if (_quickPatterns.length > 0) logClr = quickColorFor(line_.getLine());   // first find() wins
else { /* keyword loop, unchanged */ }
```

## Tests

| Class | Tests | Covers |
|---|---|---|
| `props/QuickModePropsTest` | 8 | no keys/filters/stdin filter from a config full of them; fixed settings; ANSI default colour; prefix by file count; singleton pinned; reset; empty pattern list rejected; fixture guard |
| `unit/QuickLineFormatterTest` | 11 | whole-line colour; default colour; case-sensitive; `(?i)`; regex; per-pattern colour; first wins; palette wraps; config colour pairs ignored; notices; filename prefix |
| `QuickModeCliTest` | 9 | filter not attached in quick mode (and attached in normal mode, as a fixture guard); repeat order and colours; other options ignored; prefix by file count; invalid regex, missing value and unknown option are usage errors; banner text |

- Full suite: **189 passed, 0 failed** (161 before).
- 13 mutations of the new code, each turned at least one test red.
- Smoke (packaged jar, `-DCTRAIL_CFG=ctrail-quick-isolation.xml`): every line shown, `ERROR` red,
  `timedout` yellow, the fixture's `WARN` → yellow ignored, lowercase `error` not coloured; stdin
  works; `-q '['`, `-q` and `--bogus` exit 2 with a message; a deprecated-key config prints its
  WARN in normal mode and not in quick mode.

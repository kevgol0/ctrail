# Plan: File Liveness — Tail-N on Open, Startup Banner, Idle "No Movement" Notice

**Date:** 2026-09-27
**Status:** `[FINISHED]` — 2026-09-27, all tests pass
**Branch:** `feat/liveness-tail-n-idle-notice` (cut from `master` @ `d43630d`)
**Jira:** _n/a — personal tool repo, no Jira project_
**Confluence:** _not published — personal tool repo, no team space_
**Supersedes:** [plan-tail-n-and-idle-notification-20260923-082716.md](plan-tail-n-and-idle-notification-20260923-082716.md) (`[DRAFT]`, never approved). Delete on approval?

---

## Concise View

### The real problem

You open `ctr app.log`, the screen is blank, and you cannot tell whether the file is quiet or
ctrail is broken. Three cheap signals fix that, at three different moments:

| When | Signal | Config | stdin |
|---|---|---|---|
| **t=0** | Startup banner: `watching app.log — 12.3 KB, modified 4m 12s ago` | `showStartupBanner` | `watching stdin` |
| **t=0** | Last N lines of the file (proper `tail -n`, not byte-skip) | `tailLastLines` | n/a — not seekable |
| **t=30s, 60s, …** | `app.log — no movement in 30s` | `idleNoticeSeconds` | ✅ same |
| **on resume** | `app.log — resumed after 4m 12s` | same switch | ✅ same |

The banner is the piece you did not ask for and the one I most recommend: it answers "is this
file alive?" **immediately**, instead of making you wait 30s for the first idle notice. A file
last modified 3 days ago is obvious the instant ctrail starts.

**Stdin is in scope** (your call, 2026-09-27). It is arguably where idle notices matter most — a
silent pipe gives you no mtime to check and no file to `ls`. Including it changed the design for
the better: the idle state machine now lives in **one** place instead of being inlined in the
file reader, so file mode and stdin mode share it. See [decision 8](#confirmed-decisions--scope).

[→ Detailed View](#detailed-view)

### Confirmed decisions / scope

1. **Tail-N seeks, it does not pre-read.** `FileTailTracker` positions the file pointer at the
   start of the Nth-from-last line; the existing reader loop emits those lines through the
   normal path. They get coloring, `-m` matching, file filters and filename prefix for free.
   The superseded draft enqueued lines directly, which bypassed all of that.
2. **`tailLastLines` replaces `skipAheadInBytes`** when `> 0`. Byte-skip stays as the fallback
   (`tailLastLines = 0`) so nothing existing breaks. Default `10`, matching `tail -n`.
3. **No max-files gate.** The draft gated tail-N at ≤2 files. N lines/file is already bounded
   and predictable at any file count — 5 lines × 50 files is *less* noise than today's 1000
   bytes × 50 files. Dropping the gate removes a config knob and a branch. **Say so if you want
   the gate back** and I will add `tailLastLinesMaxFiles`.
4. **Notices are a first-class line type**, not a log line that happens to be text. `LogLine`
   gets a `_notice` flag; `LineFormatter` renders notices in a dedicated color and skips keyword
   matching, so a notice containing "error" is not colored red.
5. **Idle notice repeats** every `idleNoticeSeconds` while the file stays quiet, and pairs with a
   `resumed after …` line. One-shot notices leave you guessing again 10 minutes later.
6. **`-e/--entirefile` zeroes both** `tailLastLines` and `skipAheadInBytes`.
7. **New CLI flag `-n/--lines N`** mirroring `tail -n`, overriding config.
8. **One idle state machine, shared by both modes.** A single `IdleMonitorThread` polls a list of
   activity sources; `FileTailTracker` and `StdinReaderThread` each expose one. The idle →
   notice → repeat → resume transitions are written and tested **once**. The earlier version of
   this plan inlined the checks in `FileReaderThread.run()`, which would have meant a second copy
   for stdin — two state machines that drift apart.
9. **Stdin keeps its blocking `readLine()`.** The watchdog is a separate daemon thread reading a
   `volatile long` timestamp. Polling `InputStream.available()` to make the read non-blocking was
   considered and rejected: it is unreliable across pipes and buffering, and it would mean
   rewriting all three stdin read loops.

### ⚠️ Constraints that override the house standards

The repo does **not** match the global Java standards. I follow the repo, per "never mix two
frameworks":

| House standard | This repo | Consequence |
|---|---|---|
| Corretto 21 | `maven-compiler-plugin` source/target **1.8** ([pom.xml:72](../../../pom.xml)) | No `var`, no `List.of`, no text blocks |
| JUnit 5 + Mockito | **JUnit 4.13.1**, Mockito absent ([pom.xml:16](../../../pom.xml)) | New tests are JUnit 4, `org.junit.Assert` |
| DI, never `new` on a collaborator | No DI framework; singleton `CtrailProps.getInstance()` | Match existing construction |

Raising the repo to 21 + JUnit 5 is a separate change. Flagging, not doing it here.

### Out of scope

- Raising Java/JUnit versions (above).
- Any change to color config, filtering, or the output writer's shutdown path.
- Tail-N history for stdin — a pipe is not seekable, there is no history to recover. Not a
  deferral; it is not possible.

---

## Detailed View

### Alternatives considered, and why not

| Idea | Verdict |
|---|---|
| In-place `\r` status line ("last line 42s ago") that rewrites itself, no scrollback spam | **Rejected.** `OutputWriterThread` writes asynchronously on its own thread; a `\r` line races with real output and gets shredded. It also corrupts `ctr app.log > out.txt` and piping. Not worth the fragility. |
| Periodic heartbeat every X seconds regardless of activity | **Rejected.** Noisier than idle-only, and tells you nothing the idle notice does not. |
| Idle notice fires once per idle period | **Rejected** in favour of repeat + resume — see decision 5. |
| Startup banner | **Adopted** — see "The real problem". |

### 1. Config (`CtrailProps.java`, `etc/ctrail.xml`, `etc/ctrail-stdin-example.xml`)

```xml
<execution>
  <!-- existing settings ... -->

  <!-- lines of history to show when a file is opened, like `tail -n N`.
       0 = disabled; falls back to skipAheadInBytes -->
  <tailLastLines>10</tailLastLines>

  <!-- print one banner line per file at startup: size and last-modified age -->
  <showStartupBanner>true</showStartupBanner>

  <!-- seconds of silence before a "no movement" notice; repeats at this
       interval while quiet. 0 = disabled -->
  <idleNoticeSeconds>30</idleNoticeSeconds>
</execution>

<coloring>
  <!-- color for ctrail's own notices (banner, idle, resumed) -->
  <noticeColor>cyan</noticeColor>
  <!-- ... -->
</coloring>
```

New fields on `CtrailProps`, read in the constructor beside the existing `config.getInt(...)`
calls at [CtrailProps.java:149-160](../../../src/main/java/com/kagr/tools/ctrail/props/CtrailProps.java#L149):

| Field | XML key | Type | Default |
|---|---|---|---|
| `_tailLastLines` | `execution.tailLastLines` | `int` | `10` |
| `_showStartupBanner` | `execution.showStartupBanner` | `boolean` | `true` |
| `_idleNoticeSeconds` | `execution.idleNoticeSeconds` | `int` | `30` |
| `_noticeColor` | `coloring.noticeColor` | `String` (via `getColorCode`) | `cyan` |

⚠️ **Behaviour change on upgrade:** with `tailLastLines` defaulting to `10`, an existing install
that never touched `skipAheadInBytes` switches from "last ~1000 bytes" to "last 10 lines". This
is the intended improvement (byte-skip almost always opens mid-line), but it is a change, and it
goes in the changelog.

### 2. Tail-N on open (`FileTailTracker.java`)

New package-private method, called from the constructor in place of the byte-skip block when
`getTailLastLines() > 0`:

```java
/**
 * Positions the file pointer at the first byte of the Nth-from-last line, so the
 * normal read loop emits that history through the usual filtering and coloring path.
 *
 * @param nLines_ number of trailing lines to keep; values <= 0 are ignored
 * @return the position seeked to
 */
protected final long seekToLastNLines(final int nLines_) throws IOException
```

Algorithm — backwards scan, no whole-file read:

1. Start at `_file.length()`; if the file is empty, position `0` and return.
2. Walk backwards in 8 KB chunks, counting `'\n'` bytes; stop once `nLines_ + 1` newlines are
   seen, or the start of file is reached.
3. Position just past that newline, set `_lastReadPosition`, `_file.seek(...)`.
4. A trailing newline at EOF does not count as a line boundary.

Edge cases the tests must pin: empty file, file with no trailing newline, file shorter than N
lines, file with exactly N lines, file larger than one chunk, CRLF line endings.

The existing byte-skip block ([FileTailTracker.java:75-94](../../../src/main/java/com/kagr/tools/ctrail/files/FileTailTracker.java#L75)) stays, reached only when `tailLastLines <= 0`.

`FileTailTracker` also gains `implements IActivityTracker` (§5) — the three idle fields and their
accessors, nothing more. `FileReaderThread.readToFilePosition()` calls `noteActivity()` on each
line emitted.

### 3. Startup banner (`CtrailEntryPoint.java`)

In `getFilesFromArgs()` ([CtrailEntryPoint.java:143](../../../src/main/java/com/kagr/tools/ctrail/CtrailEntryPoint.java#L143)), after each tracker is built and before the reader
thread starts, when `isShowStartupBanner()`:

```
ctrail: watching app.log — 12.3 KB, modified 4m 12s ago
```

Size from `File.length()`, age from `File.lastModified()`. A new private helper
`formatDuration(long millis_)` renders `45s`, `4m 12s`, `2h 09m`, `3d 04h` — shared with the idle
notice, so it lives in a small new final class `com.kagr.tools.ctrail.unit.DurationFormatter`
with one static method. Strings built with `StringUtils`/`StringBuilder`, no streams.

Banner lines are enqueued as notices **before** `_reader.start()`, so they land ahead of any
tailed history.

### 4. Notice line type (`LogLine.java`, `LineFormatter.java`)

`LogLine` gains `private boolean _notice;` and a second constructor:

```java
public LogLine(final String origFileName_, final String line_, final FileSearchFilter fst_, final boolean notice_)
```

The existing 3-arg constructor delegates with `false`, so all current call sites compile
untouched.

`LineFormatter.format()` gets an early branch before keyword matching
([LineFormatter.java:75](../../../src/main/java/com/kagr/tools/ctrail/unit/LineFormatter.java#L75)):

```java
// ctrail's own notices bypass keyword coloring and filename prefixing
if (line_.isNotice())
{
    return _noticeColor + line_.getLine() + _reset;
}
```

### 5. Idle state machine — shared by files and stdin (2 new classes)

> ⚠️ **Built as composition, not an interface.** `lombok.accessors.chain = true` in
> [lombok.config](../../../lombok.config) makes every generated setter return `this`, and a
> chained setter cannot implement a `void` interface method — `IActivityTracker` would have
> forced hand-written setters on both implementors. A concrete `ActivityState` that both
> classes *hold* avoids the clash entirely and keeps the state machine in one place, which was
> the point of decision 8. Same behaviour, same tests, one fewer type.

**`files/ActivityState.java`** (new) — what it takes to be watched. Deliberately tiny:

```java
public class ActivityState
{
    private final String _name;              // "app.log" or "stdin"
    private final long _idleIntervalMillis;  // <= 0 disables notices for this source
    private volatile long _lastActivityMillis;
    private volatile long _idleNoticeDueMillis;
    private volatile boolean _idle;
    private volatile boolean _finished;      // stdin at EOF; a file is never "done"
}
```

`FileTailTracker` and `StdinReaderThread` each expose one via `getActivityState()`. Every field
the monitor reads is **volatile** — written on a reader thread, read on the monitor thread.

**`files/IdleMonitorThread.java`** (new) — a daemon `Thread` holding
`List<ActivityState>` and the output deque. `run()` ticks every **250 ms** (a constant, not a
config knob — cheap, and precise enough that `idleNoticeSeconds = 1` still fires on time):

```java
for (int i = 0; i < _sources.size(); i++)   // iteration, not streams
{
    checkForIdle(_sources.get(i), System.currentTimeMillis());
}
```

`checkForIdle` is the whole state machine, in one place:

- `now >= getIdleNoticeDueMillis()` → emit `ctrail: app.log — no movement in 30s`,
  `setIdle(true)`, advance the due time by the interval. Elapsed is computed from
  `getLastActivityMillis()`, so the second notice reads `no movement in 1m 00s`, not `30s` again.
- Resume is detected by the **reader** side, not the monitor: `noteActivity()` on a source that
  `isIdle()` emits `ctrail: app.log — resumed after 4m 12s` and clears the flag. Putting it there
  means the resume line lands immediately with the new data, not up to 250 ms later.

`noteActivity(...)` is a static helper on `IdleMonitorThread` so both readers call the same code.
It has a package-visible overload taking the clock, added because the resume path could not
otherwise be asserted deterministically — a testability seam, not a behaviour change.

Lifecycle: constructed in `CtrailEntryPoint` **only when `idleNoticeSeconds > 0`**, started
alongside the reader/writer in `start()`, flagged down in `shutdown()`. Daemon, so a missed
shutdown cannot hang the JVM.

⚠️ **The monitor gets an immutable `ArrayList` snapshot of the trackers, not the live
`BlockingDeque`.** `FileReaderThread` `take()`s a tracker out of the deque while reading it; a
monitor iterating the deque would intermittently not see it. The snapshot is built in
`getFilesFromArgs()` where the trackers already exist, and never mutated after `start()`.

⚠️ **Use `_output.offer(...)`, not `_output.add(...)`, for every notice.** `_output` is a bounded
`LinkedBlockingDeque` sized by `maxPendingLines`; `add()` throws `IllegalStateException` when
full — on the monitor thread that would kill the watchdog silently, which is the worst possible
failure for a feature whose job is telling you nothing is happening. Existing line-emission code
uses `add()`; I am **not** changing that here (out of scope), only keeping the new paths safe.

### 6. Stdin mode (`StdinReaderThread.java`)

1. Holds an `ActivityState` named `"stdin"`, matching the filename
   already used on its `LogLine`s ([StdinReaderThread.java:130](../../../src/main/java/com/kagr/tools/ctrail/files/StdinReaderThread.java#L130)).
2. `private volatile long _lastActivityMillis` initialised at construction, and
   `private volatile boolean _finished`.
3. All three read loops — `runWithSearchFiler`, `runWithMatch`, `runPassthrough` — call
   `IdleMonitorThread.noteActivity(this, _output)` on each line they emit. ⚠️ **On the line
   emitted, not on every line read**: a filtered-out line is not movement you can see, so a
   filter that drops everything should still go idle. That is the behaviour you want, and it is
   an easy thing to get backwards.
4. `_finished = true` in a `finally` on each loop, so EOF stops the notices instead of leaving
   `no movement in 5m` scrolling after the pipe closed.
5. Startup banner for stdin is a plain `ctrail: watching stdin` — no size, no mtime available.

`CtrailEntryPoint.initInputReaderThread()` ([CtrailEntryPoint.java:99](../../../src/main/java/com/kagr/tools/ctrail/CtrailEntryPoint.java#L99)) already branches on file-count; the
stdin branch registers the `StdinReaderThread` as the monitor's single source.

⚠️ **Shutdown ordering.** `StdinReaderThread.run()` calls `_ender.initiateShutdown()` at EOF, and
`OutputWriterThread` then drains the queue. The monitor must be stopped **before** the writer, or
a notice enqueued during the drain can be printed after the last real line. Handled in
`CtrailEntryPoint.shutdown()`: monitor first, then reader, then writer.

### 7. CLI (`CtrailEntryPoint.loadArgsAndOverrides`)

```java
options.addOption(Option.builder("n")
        .longOpt("lines").hasArg()
        .argName("N")
        .desc("show the last N lines of each file on open (default 10; 0 disables)")
        .build());
```

- `-n N` → `setTailLastLines(N)`; non-numeric input logs a warning and leaves the config value.
- `-e/--entirefile` → `setSkipAheadInBytes(0)` **and** `setTailLastLines(0)`.
- Help footer gains a line for `-n`.

---

## Tests

JUnit 4 (repo standard — see constraints). Every new logic-bearing method gets a test; the only
exemptions are Lombok accessors.

| Class (new) | Covers |
|---|---|
| `files/FileTailTrackerSeekTest` | `seekToLastNLines` — empty file, no trailing newline, fewer lines than N, exactly N, >8 KB multi-chunk, CRLF, N ≤ 0 no-op |
| `files/IdleMonitorThreadTest` | the whole state machine against a hand-rolled `IActivityTracker` stub: notice fires at the interval; repeats with a **growing** elapsed figure; `resumed after …` on `noteActivity` while idle; no resume line when never idle; `isFinished()` sources are skipped; nothing emitted at `idleNoticeSeconds = 0`; a full output deque does not kill the thread |
| `files/StdinReaderThreadIdleTest` | drives a `PipedInputStream`: activity noted per **emitted** line; a line dropped by the match/filter does **not** count as activity; `_finished` set at EOF and no notices after it |
| `files/FileReaderThreadIdleTest` | end-to-end on a temp file: write, go quiet past the interval, assert the notice; append, assert `resumed after …` lands before the new line |
| `unit/DurationFormatterTest` | `45s`, `4m 12s`, `2h 09m`, `3d 04h`, zero, negative |
| `unit/NoticeFormattingTest` | `LineFormatter` renders a notice in `noticeColor`, with no filename prefix and no keyword coloring — including a notice whose text contains a color keyword |
| `props/LivenessPropsTest` | all four new keys read from a test XML; defaults applied when keys are absent |

Test config fixture: `src/test/resources/configs/ctrail-liveness.xml`, with `idleNoticeSeconds`
set to `1` so the suite stays fast — no test waits 30 s.

The shared state machine (decision 8) is what makes this testable: `IdleMonitorThread` takes a
`List<ActivityState>`, so hand-set timestamps exercise every transition with no
files, no threads and no sleeping. Inlined in `FileReaderThread`, the same coverage would have
needed real files and real waiting — twice.

⚠️ **Each new test is verified by deleting the line it covers and watching it go red** — a test
written against working code can assert nothing and still pass. I will report the red/green pair
per test in the modifications log.

Run:

```bash
mvn -q test
```

## Build / verify

1. `mvn -q clean package` — expect 0 errors, 0 new warnings.
2. `mvn -q test` — full suite green, existing tests unchanged.
3. Smoke test, tail-N + banner:
   ```bash
   printf 'line %s\n' $(seq 1 50) > /tmp/ctr-smoke.log
   java -jar target/ctrail-1.1.0.jar /tmp/ctr-smoke.log
   ```
   Expect a banner line, then `line 41` … `line 50`, then a `no movement in 30s` notice ~30s
   later, repeating.
4. Smoke test, resume: `echo "line 51" >> /tmp/ctr-smoke.log` → expect `resumed after …` then
   `line 51`.
5. Smoke test, **stdin** — a deliberately slow producer:
   ```bash
   (echo "first"; sleep 45; echo "second") | java -jar target/ctrail-1.1.0.jar
   ```
   Expect `watching stdin`, `first`, a `stdin — no movement in 30s` notice, another at 60s,
   then `stdin — resumed after …` immediately followed by `second`, then a clean exit at EOF
   **with no trailing idle notice**.
6. Smoke test, stdin back-compat: `cat /etc/hosts | java -jar target/ctrail-1.1.0.jar` → all
   lines, immediate exit, no notices at all.
7. Smoke test, back-compat: `<tailLastLines>0</tailLastLines>` → byte-skip behaviour unchanged;
   `-e` → whole file, no banner history duplication.
8. Final verdict recorded as ✅/❌ with the log excerpts.

## Open questions

None blocking. Two calls I made for you, reversible on request:

1. **Q1. Max-files gate** — dropped (decision 3). Say so and I add `tailLastLinesMaxFiles`.
2. **Q2. Default `tailLastLines = 10`** — changes behaviour for existing installs (see §1). Set
   it to `0` instead if you want upgrade-silent behaviour and opt-in tail-N.

## Follow-ups (not this change)

Two pre-existing bugs surfaced while testing. **Neither is caused by this change and neither is
fixed here** — both are separate behaviour changes that deserve their own plan.

1. **The last line is dropped at shutdown.** `OutputWriterThread.run()` drains with
   `int sz = _output.size() - 1;`, one short of what is queued. Reproduced on the **master** jar
   (`printf 'alpha\nbravo\ncharlie\n' | ctr` prints only alpha and bravo, 5 runs out of 5),
   so it predates this branch. Most visible on stdin, where EOF triggers shutdown immediately.
2. **A config with exactly one `<colorpair>` gets no colors at all.** `initColoring()` does
   `((Collection<?>) config.getProperty("coloring.linecolors.colorpair.fgcolor")).size()`, and
   commons-configuration returns a bare `String` for a single element, so the `ClassCastException`
   is swallowed and the count stays 0. `extractCount()` already handles this correctly for
   filters; `initColoring` does not use it. Both test fixtures here carry two colorpairs to work
   around it.

Also outstanding:

- Raise the repo to Corretto 21 + JUnit 5 + Mockito.
- `_output.add(...)` → `offer(...)` on the existing line-emission paths, so a full queue degrades
  instead of killing the reader thread.

## Definition of Done

> **Tests are not optional** — every logic-bearing method has a unit test (JUnit 4 per repo
> standard, in place of JUnit 5 + Mockito); verified by deleting the covered line and watching it
> go red; smoke-tested against a real file; committed on the feature branch; plan and changelog
> updated. Not published to Confluence — personal tool repo, no team space.

- [ ] Four config keys read from XML with the stated defaults; absent keys fall back.
- [ ] `tailLastLines > 0` shows exactly the last N lines through the normal filter/color path.
- [ ] `tailLastLines = 0` leaves byte-skip behaviour byte-for-byte unchanged.
- [ ] `-n N` overrides config; `-e` zeroes both tail-N and byte-skip.
- [ ] Startup banner prints size and last-modified age per file; `watching stdin` in stdin mode;
      suppressed when disabled.
- [ ] Idle notice fires at the interval, repeats with a growing elapsed figure, and is silent at `0`.
- [ ] `resumed after …` prints when a quiet source moves again, ahead of the new line.
- [ ] **Stdin:** notices fire on a quiet pipe, count only *emitted* lines as activity, and stop at
      EOF with no trailing notice.
- [ ] One idle state machine — `grep` confirms the transitions exist in `IdleMonitorThread` only.
- [ ] Notices render in `noticeColor`, unaffected by keyword coloring.
- [ ] Full suite green; every new test verified red-then-green.
- [ ] README config reference, CLI table and help footer updated.
- [ ] `etc/ctrail.xml` updated **idempotently**, with comments, original backed up.

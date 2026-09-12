# Plan: Add stdin example config

**Date:** 2026-09-02  
**Status:** [PROPOSED]  
**Branch:** `feature/stdin-example-config`

---

## Goal

Add a stdin-specific example config file (`etc/ctrail-stdin-example.xml`) that demonstrates how to use the `stdin$` filter feature when piping input through ctrail.

## Background

The code in `CtrailEntryPoint.java:112` looks up `stdin$` as the filter key in the `FileSearchFilter` map when no file arguments are provided (i.e., stdin mode). The `FileSearchFilter.toRegEx()` method appends `$` to the filename, so a `<filename>stdin</filename>` entry in the config becomes the regex key `stdin$`.

This feature is undocumented — there's no example config showing how to set up stdin filtering.

## Changes

### Phase 1: Add example config file

1. **Create `etc/ctrail-stdin-example.xml`**
   - Full working config with a `<filefilter>` entry using `<filename>stdin</filename>`
   - Include both `<includes>` and `<excludes>` keyword examples
   - Inline XML comments explaining the stdin filter mechanism
   - Standard coloring section matching the existing `etc/ctrail.xml` style

### Phase 2: Update README

2. **Update `README.md`** — add a brief note in the Usage section about stdin filtering with a pointer to the example config.

### Phase 3: Verify

3. **Build** — `mvn package` to confirm no regressions.

## Definition of Done

- [ ] `etc/ctrail-stdin-example.xml` exists with a working stdin filter config
- [ ] README references the stdin example
- [ ] Project builds cleanly

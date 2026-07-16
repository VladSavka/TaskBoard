# Acceptance pipeline (seeded template)

Acceptance pipeline built on the
[Acceptance Pipeline Specification](https://github.com/unclebob/Acceptance-Pipeline-Specification)
(APS). The portable `gherkin-parser` comes from APS (Babashka); the generator,
runtime, step handlers, and scripts here are the project-specific components.

**This directory is pre-seeded by `swarmup`.** The engine (runtime, generator,
`bb.edn`, `bin/` scripts) is ready — do NOT rebuild it. Only wire the two
project-specific pieces (see "Wiring", below).

## Layout

| Path | Role |
| --- | --- |
| `../features/` | Gherkin feature files (the specifier's deliverable; the APS subset) |
| `bin/product-harness` | Headless launcher that runs the Android-free product classes on a plain JVM so handlers can drive real behavior — **wire this** |
| `src/aps_project/handlers.clj` | Regex step handlers connecting step text to project behavior — **write these** |
| `src/aps_project/runtime.clj` | Expands JSON IR into scenario executions, dispatches steps (engine) |
| `src/aps_project/generator.clj` | JSON IR → runnable entry point + conforming metadata (engine) |
| `spec/` | Speclj unit specs for runtime and generator |
| `bin/` | Executable wrappers and the normal acceptance runner |
| `build/acceptance/` | Generated IR, entry points, metadata (gitignored) |

## Wiring (the only project-specific work)

1. Expose a headless Kotlin harness entrypoint (an `Android-free` `HarnessKt`)
   that answers the commands your handlers call.
2. In `bin/product-harness`, set `main_class` (and `classes` if your build path
   differs) in the "project wiring" block. The harness auto-compiles the debug
   Kotlin, trying offline first and falling back to an online build on a fresh
   checkout — no manual prime needed.
3. Write regex step handlers in `src/aps_project/handlers.clj` for your step
   shapes (keep the smoke handlers until yours pass, then remove them).

## One-time setup

External APS tools are installed fresh from GitHub (not vendored):

```sh
acceptance/bin/install-tools
```

This clones `.aps` and `.speclj-structure-check` at the worktree root.

## Running

```sh
cd acceptance
bb verify        # structure-check the specs, then run them (check + test)
bb test          # harness unit specs (Speclj)
bb check spec    # Speclj structure check only
./bin/acceptance # normal acceptance run: parse -> generate -> run all features
```

`./bin/acceptance` exits non-zero if any generated acceptance test fails.

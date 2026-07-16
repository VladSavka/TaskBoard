# Task Board

A small Android task-management app built with **Kotlin**, **Jetpack Compose
(Material 3)**, and **MVVM + Clean Architecture**. It manages a board of tasks
with priorities and optional due dates, backed by a mock "network" data source
that exercises realistic **loading / empty / error** states.

## Automated testing

### 1. Unit tests (JVM — no device needed)

Fast JVM tests for the domain use cases, ViewModels, and data sources
(`app/src/test/…`):

```sh
./gradlew :app:testDebugUnitTest
```

### 2. Acceptance tests (executable Gherkin specs)

Behavior is specified as human-readable **Gherkin** and executed against the real
product classes on a plain JVM (no emulator), using the
[Acceptance Pipeline Specification](https://github.com/unclebob/Acceptance-Pipeline-Specification)
(APS).

**Where a reviewer reads the tests:**

- **The specs** — the best starting point: [`features/`](features/), one
  `*.feature` file per behavior (add, edit, complete, delete, due dates, filter,
  sort, list states, theme, mock network, …).
- **Step definitions:** `acceptance/src/aps_project/handlers.clj`
- **Headless harness** that drives the real ViewModel:
  `app/src/debug/java/com/example/acceptance/Harness.kt`

**One-time setup:**

```sh
# 1. Install Babashka (bb):  https://github.com/babashka/babashka#installation
#    e.g. macOS / Linux:  brew install borkdude/brew/babashka
# 2. Fetch the acceptance test tools — run this script (needs git + network):
./acceptance/bin/install-tools
```

> **OS note:** `bb verify` / `acceptance/bin/acceptance` run via Babashka, but the
> `install-tools` and `product-harness` helpers are **bash** scripts. They run
> natively on **macOS and Linux**; on **Windows** run them under **WSL** or **Git
> Bash**. A JDK 17+ is required. The harness needs the **Android SDK** too — point
> it at yours with `export ANDROID_HOME=/path/to/Android/sdk` (it also reads
> `sdk.dir` from `local.properties` if present, e.g. after opening the project in
> Android Studio). On macOS with a default Android Studio install it falls back to
> `~/Library/Android/sdk` automatically.

**Run:**

```sh
cd acceptance
./bin/acceptance     # parse -> generate -> run all features; exits non-zero on any failure
# or:
bb verify            # structure-check the specs, then run them
```

The harness compiles the app's debug Kotlin, so the **Android SDK must be
available** (it locates `android.jar` via `local.properties` / `ANDROID_HOME`) —
the same toolchain used to build the app.

## Project layout

```
app/
  src/main/          production code (domain / data / presentation)
  src/test/          JVM unit tests
  src/debug/         headless acceptance harness (Harness.kt)
features/            Gherkin acceptance specs  ← read these
acceptance/          APS pipeline (runner, step handlers, generator)
gradle/              version catalog + wrapper
```

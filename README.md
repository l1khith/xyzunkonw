# SplitSignal

> **Two minds. One 3×3 grid. Zero internet.**

**SplitSignal** is a clean, minimalist cooperative mathematical deduction game built with Kotlin Multiplatform and Compose Multiplatform. Inspired by the editorial clarity of **NYT Games** and **Jane Street** puzzles, it strips away sci-fi clutter and jargon—leaving pure numbers, crisp typography, and asymmetric logic between two players.

One player holds the **Clue Sheet** (row sums, column sums, and relational constraints), while the other controls the **3×3 Number Grid** (`Tap any tile (+1)`). Play across two devices over local Wi-Fi with zero cloud infrastructure, or solve side-by-side in **Solo** split-screen mode across **Android**, **Desktop (Windows, macOS, Linux)**, and **WebAssembly (`wasmJs`)**.

---

## Aesthetic & Game Design

- **Zero-Jargon Editorial Aesthetic:** Warm paper surfaces, crisp hairlines, tabular numerals, and high-contrast ink—no reactors, oscilloscopes, or voltage gauges.
- **Asymmetric Two-Role Core:**
  - **Clue Sheet:** Displays read-only mathematical constraints, target row/column sums, parity or ordering clues, and live validation feedback without direct grid input.
  - **Number Grid:** Presents an interactive 3×3 tile matrix (`Tap any tile (+1)`) where every tap increments and cycles tile values without exposing the hidden constraints.
- **Warm-Up & Timed Progression:** Every run opens with an untimed warm-up puzzle so players can calibrate communication and verify local sync before moving into seeded timed puzzles (`Solved · Next Puzzle`).

---

## Quick Start

```bash
# 1. Run Desktop Solo Split-Screen
./gradlew :composeApp:run

# 2. Run WebAssembly (wasmJs) Browser App
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# 3. Run verification tests across modules
./gradlew :core:domain:jvmTest :composeApp:desktopTest

# 4. Build the Android Debug APK
./gradlew :composeApp:assembleDebug
```

---

## Multi-Module Architecture

SplitSignal is organized into focused Kotlin Multiplatform modules with clean `commonMain`, `jvmAndAndroidMain`, and `wasmJsMain` source sets:

```text
+---------------------------------------------------------------------------------------------------+
|                                   :composeApp (App Shell & DI)                                    |
|  - SplitSignalApp.kt, Koin DI (initKoin), WindowSizeClass adaptive layouts, composeResources      |
|  - androidMain: SplitSignalApplication, MainActivity | desktopMain: Main.kt | wasmJsMain: Main.kt |
+------------------------------------+---------------------------------+----------------------------+
                                     |                                 |
                   +-----------------+-----------------+   +-----------+-----------+
                   |         :feature:harmonic         |   |    :feature:conduit   |
                   |-----------------------------------|   |-----------------------|
                   | - Clue Sheet presentation &       |   | - 3×3 Number Grid UI  |
                   |   constraint readouts             |   | - Tap-to-increment    |
                   | - Target sums & deduction cues    |   |   tile controls       |
                   +-----------------+-----------------+   +-----------+-----------+
                                     |                                 |
                   +-----------------+---------------------------------+-----------+
                   |                         :core:network                         |
                   |---------------------------------------------------------------|
                   | commonMain: KtorSyncClient                                    |
                   | jvmAndAndroidMain: KtorSyncHost (CIO WS), LocalIpResolver     |
                   | wasmJsMain: Browser client-only host capability guards        |
                   +-------------------------------+-------------------------------+
                                                   |
                   +-------------------------------v-------------------------------+
                   |                          :core:domain                         |
                   |---------------------------------------------------------------|
                   | - Puzzle Engine, DeterministicRng (SplitMix32), Fnv1a         |
                   | - RoomCodeGenerator, SyncTransport, InMemorySyncPipe          |
                   | - SyncPacket, GameModels                                      |
                   +---------------------------------------------------------------+
```

### Key Architectural Pillars
- **Shared `jvmAndAndroidMain` + `wasmJsMain` Source Sets:** JVM socket capabilities (embedded Ktor `CIO` WebSocket server and `java.net.NetworkInterface` IPv4 discovery) live in `jvmAndAndroidMain`, while `wasmJsMain` compiles the full puzzle engine, UI, and WebSocket client in browser sandboxes (`ComposeViewport` via `index.html`).
- **Koin Dependency Injection:** `initKoin` wires domain, transport, and presentation dependencies consistently across `SplitSignalApplication` (Android), Desktop `Main.kt`, and WasmJS `Main.kt`.
- **Compose Multiplatform Resources (`composeResources`):** Shared UI strings live in `composeApp/src/commonMain/composeResources/values/strings.xml`, while Android launcher metadata uses `androidMain/res/values/strings.xml`.
- **Adaptive `WindowSizeClass` Layout:** `SplitSignalApp` adapts between single-pane layouts on phones and dual-pane **Clue Sheet** + **Number Grid** split views on tablets, desktops, and wide browser viewports (enforcing a `640×560` minimum window size on Desktop).

---

## Local Wi-Fi Co-op (`port = 0` Ephemeral Ktor CIO)

1. **Zero-Config Ephemeral Binding:** On `jvmAndAndroidMain` targets, `KtorSyncHost` starts an embedded Ktor `CIO` WebSocket server bound to `0.0.0.0:0` (`port = 0`), letting the OS assign a free ephemeral port with zero manual configuration.
2. **LAN Address Discovery:** Once started, `KtorSyncHost` reads `engine.resolvedConnectors().first().port` and pairs it with `LocalIpResolver.getLocalIpv4Address()` (preferring an active, non-loopback site-local IPv4 interface) to display `<ip>:<port>` alongside a 4-character unambiguous room code (`RoomCodeGenerator`).
3. **Room-Validated WebSocket Sync:** `KtorSyncClient` connects to `ws://<ip>:<port>/ws`, validates the room code via `SyncPacket.Handshake`, and synchronizes puzzle state and grid actions encoded with `kotlinx.serialization` JSON.

---

## KMP Boundary Decisions

- **`UInt` SplitMix32 + 32-bit FNV-1a over `Random` / `String.hashCode()`:** Platform RNGs and `String.hashCode()` can differ across JVM, Android, and WasmJS runtimes. `DeterministicRng` uses unsigned 32-bit SplitMix arithmetic (`UInt`), and `Fnv1a.hash32` hashes UTF-8 bytes directly so `RoomCodeGenerator` and puzzle generation produce identical boards for any room code on every target.
- **`SyncTransport` decoupling `InMemorySyncPipe` from Ktor WebSockets:** The domain engine communicates strictly through `SyncTransport` (`incomingPackets`, `connectionState`, `send`, `close`). `InMemorySyncPipe` powers zero-latency **Solo** split-screen play and deterministic `commonTest` parity tests, while `KtorSyncHost` and `KtorSyncClient` drop in seamlessly for local Wi-Fi play.
- **Target Capability Separation (`jvmAndAndroidMain` vs `wasmJsMain`):** Isolating embedded Ktor `CIO` server hosting and `java.net.NetworkInterface` IPv4 resolution in `jvmAndAndroidMain` enables `wasmJsMain` to share `:core:domain`, feature UI modules, and `:composeApp` without browser sandbox conflicts.

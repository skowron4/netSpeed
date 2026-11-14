# netSpeed

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF) ![Platform](https://img.shields.io/badge/Platform-Android_API_33--36-3DDC84) ![Domain](https://img.shields.io/badge/Domain-System_Telemetry_%26_Android_Internals-blue) ![Build](https://img.shields.io/badge/Build-Gradle_9.3.1_%7C_AGP_9.1.0-informational) ![Status](https://img.shields.io/badge/Status-Work_in_Progress-yellow)

## Abstract

`netSpeed` is a passive, zero-permission network receive (RX) throughput telemetry service for Android (API 33–36). Rather than generating synthetic probe traffic via an HTTP client or requiring `android.permission.INTERNET`, the system samples kernel-level byte counters directly via `android.net.TrafficStats` and computes a moving average over a sliding time window. Real-time metrics are rendered dynamically onto in-memory `ARGB_8888` bitmaps injected into the system status bar notification pipeline and pushed via IPC to a home-screen `AppWidgetProvider`.

## System Architecture & Pipeline

The application operates as a single-process reactive loop decoupled from the UI lifecycle via an Android Foreground Service (`specialUse` subtype).

1. **Lifecycle & Permission Gate (`MainActivity`)**: A Jetpack Compose (Material 3) entrypoint manages runtime permission negotiation for `android.permission.POST_NOTIFICATIONS` using `ActivityResultContracts.RequestPermission()`. Upon grant, it dispatches an explicit `Intent` to promote `SpeedMonitorService` via `Context.startForegroundService()`.
2. **Service Initialization (`SpeedMonitorService`)**: On creation, the service initializes a low-importance notification channel (`NotificationManager.IMPORTANCE_LOW`) to suppress acoustic/haptic interrupts, seeds the initial traffic counter baseline, and immediately calls `startForeground()` with `START_STICKY` restart semantics.
3. **Telemetry Ingestion & Sliding-Window Aggregation (`SpeedCalculator`)**: A coroutine loop executing on `Dispatchers.Main` ticks at a 500 ms cadence (2 Hz). Each tick queries monotonic time via `SystemClock.elapsedRealtime()` and total received bytes across all network interfaces via `TrafficStats.getTotalRxBytes()`.
4. **Dynamic Bitmap Rasterization (`IconGenerator`)**: Because Android's status bar notification API restricts `smallIcon` resources and does not natively support arbitrary live text in the status bar slot, `IconGenerator` allocates a 128x128 pixel `Bitmap.Config.ARGB_8888` buffer (65,536 bytes) per tick. It rasterizes the numeric value (70 px bold typeface, baseline `y = 65f`) and unit suffix (40 px regular typeface, baseline `y = 115f`) onto a software `Canvas` and wraps the output in an `IconCompat` payload.
5. **IPC Dispatch**: The service updates the ongoing notification (`notificationId = 1`, `setOnlyAlertOnce(true)`) via `NotificationManager.notify()` and pushes formatted throughput strings across process boundaries to the home-screen host using `RemoteViews` and `AppWidgetManager.updateAppWidget()`.

## Engineering & Performance Highlights

* **Zero-Network Overhead Telemetry**: Reads kernel interface statistics exposed by the Android framework (`TrafficStats.getTotalRxBytes()`). The application requires no network sockets, eliminates background wake-locks associated with active packet probing, and completely omits the `INTERNET` permission from its security footprint.
* **Sliding-Window Moving Average**: `SpeedCalculator` maintains a bounded in-memory history (`java.util.ArrayList<Measurement>`) of `(bytes: Long, time: Long)` tuples over a 3000 ms window (~7 samples at steady 2 Hz polling). Stale entries are evicted in O(n) time per tick via `removeAll { currentTime - it.time > 3000 }`, where n <= 8. Throughput is derived from the delta between the newest sample and the oldest surviving sample in the window (`rxDiff * 1000.0 / timeDiff / 1024.0`), smoothing out transient burst variance while maintaining sub-second responsiveness.
* **Binary-Prefix Formatting**: Throughput values scale dynamically across binary magnitudes: truncated integers for `< 1024.0` KiB/s (`K`), single-decimal precision (`%.1f`) above `1024.0` KiB/s (`M`), and `>= 1048576.0` KiB/s (`G`), with automatic fallback clamping (`0K`) on zero-delta intervals or counter resets.
* **Memory & Threading Trade-offs**:
  * *Current Implementation*: Rasterization and IPC serialization execute on `Dispatchers.Main` every 500 ms, allocating 65.5 KiB per bitmap (an allocation rate of 128 KiB/s managed by ART's generational garbage collector without explicit `Bitmap.recycle()` calls or object pooling).
  * *Known Limitations & Planned Refactoring*: Commit `0ff5fad` contains an incomplete work-in-progress integration of `SpeedWidgetProvider` (missing `@layout/widget_speed` and `@xml/widget_info` resources, a duplicate `startUpdatingSpeed()` declaration in `SpeedMonitorService.kt`, and a stripped `<manifest>` root tag). Furthermore, reusing a single mutable `Bitmap` buffer and pre-allocated `Paint` instances on `Dispatchers.Default` is planned to eliminate per-tick heap allocations.

## Build & Usage

### Prerequisites
* **JDK**: Java 21 (configured via Gradle Daemon JVM toolchain in `gradle/gradle-daemon-jvm.properties` with Foojay resolver 1.0.0; Java bytecode target compatibility is set to `VERSION_11`).
* **Android SDK**: `compileSdk = 36` (`minorApiLevel = 1`), `minSdk = 33` (Android 13), `targetSdk = 36`.
* **Build System**: Gradle 9.3.1 (Kotlin DSL), Android Gradle Plugin (AGP) 9.1.0 with built-in Kotlin 2.2.10 and Compose Compiler plugin.

### Note on Commit `0ff5fad` Build State
To compile commit `0ff5fad` cleanly, resolve the work-in-progress widget state before executing Gradle tasks:
1. Restore the root `<manifest>` tag and `<uses-permission>` declarations (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`) in `app/src/main/AndroidManifest.xml`.
2. Remove the duplicate `startUpdatingSpeed()` method in `SpeedMonitorService.kt` (lines 51 and 64) and invoke the constructor `AppWidgetProvider()` in `SpeedWidgetProvider.kt`.
3. Provide the widget layout (`res/layout/widget_speed.xml` containing `@+id/widgetSpeedText`) and metadata (`res/xml/widget_info.xml`).

### Commands

```bash
# Assemble debug and release binaries
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease

# Install the debug build on a connected device or emulator
./gradlew :app:installDebug

# Execute unit and instrumented test suites
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest

# Inspect resolved runtime dependency graph
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
```

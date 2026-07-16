# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Android companion app (Kotlin, Jetpack Compose) for a desk clock built around an
ESP32 + RGB LED matrix panel (firmware: https://github.com/golyakoff/Matrix_Clock_ESP32).
The app talks to the clock exclusively over Bluetooth Low Energy — no backend, no
Wi-Fi, no cloud sync. The only network calls are to the GitHub Releases API, used
to check for and download firmware updates.

The firmware repo is checked out locally at `c:\xMC\Matrix_Clock_ESP32` (PlatformIO
project) — read it directly when you need to check the device side of the BLE
protocol (e.g. `include/ble.h` for characteristic UUIDs) instead of guessing from
this repo alone.

## Commands

```bash
./gradlew assembleDebug          # debug build
./gradlew assembleRelease        # release build (unsigned unless signing props passed, see release.yml)
./gradlew installDebug           # build and install on a connected device/emulator
./gradlew testDebugUnitTest      # JVM unit tests (app/src/test)
./gradlew connectedAndroidTest   # instrumented tests on a device/emulator (app/src/androidTest)
./gradlew lint                   # Android lint
```

There is currently no meaningful test suite — `app/src/test` and `app/src/androidTest`
only contain the default template tests (`ExampleUnitTest`, `ExampleInstrumentedTest`).
Don't assume test coverage exists for a given class before changing it.

Single Kotlin/JVM module: everything lives under `app/`. `minSdk = 28`, `targetSdk = 36`,
`compileSdk = 36`. Version bumps live in `app/build.gradle.kts` (`versionCode`/`versionName`)
and must be paired with a new section at the top of `CHANGELOG.md`.

## Releasing

Pushing a tag `vX.Y.Z` (or running the workflow manually) triggers
`.github/workflows/release.yml`: it builds a signed release APK using four repo
secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) and
publishes a GitHub Release whose body is pulled directly from the `# Release X.Y.Z`
section of `CHANGELOG.md` matching the tag. If you bump the version, add the
matching changelog section first or the release notes will say "No changelog
section found."

## Architecture

```
app/src/main/java/net/agolyakov/tetrisclockble/
  data/model/ble/        BLE domain models (TetrisClockDevice, TetrisClockTime, TetrisClockAlarm, ...)
  data/model/github/      GitHub Releases API DTOs
  data/local/             SharedPreferences-backed storage (per-device friendly names)
  data/remote/            Retrofit GitHub API service + auth interceptor
  data/repository/        DeviceRepository (unused placeholder, see below), FirmwareRepository, GithubRepository
  domain/repository/      Repository interfaces (e.g. PreferencesRepository)
  domain/usecase/         Small use cases composed into ViewModels
  service/bluetooth/      BLE GATT layer: TetrisClockBleManager, BluetoothService, per-characteristic handlers
  ui/screen/{home,device,firmware}/  One package per screen: Screen (Composable) + ViewModel
  navigation/             NavGraph.kt (single NavHost, 3 routes: Home, Device, Firmware)
  di/MainModule.kt         Single Hilt module wiring the entire graph
```

### BLE communication layer

This is the core of the app and the layer most changes touch. It's split across
three cooperating pieces:

- **`TetrisClockBleManager`** (extends Nordic's `BleManager`) — owns the GATT
  connection, resolves the single `SERVICE_CONTROL_UUID` service, and exposes one
  `get*Characteristic()` / `set*Characteristic()` pair per BLE characteristic.
  All characteristic UUIDs and their read/write/notify semantics are documented
  as comments in this file's companion object — **when adding a characteristic,
  update both this file and the corresponding definition in the firmware's
  `include/ble.h`** (`c:\xMC\Matrix_Clock_ESP32\include\ble.h`) since the two
  must stay in sync.
- **Per-characteristic handlers** (`service/bluetooth/handlers/*ReadCharacteristicHandler`)
  — one small class per characteristic, each implementing
  `ReadCharacteristicHandler.onReadCharacteristicCallback(device, data)` to parse
  the raw `Data` payload and push it into a `MutableStateFlow`. `TetrisClockBleManager`
  is constructed with all of them injected and wires each to its characteristic's
  notification/read callback.
- **`BluetoothService`** (`@Singleton`, Hilt-injected everywhere) — the only class
  ViewModels talk to. Holds one `MutableStateFlow` per piece of device state
  (time, on/off, brightness, alarms, aging offset, temperature, firmware version,
  connection state) backed by a single `TetrisClockBleManager` instance, plus
  connect/disconnect/reconnect logic and OTA control. `resetDeviceState()` is
  called on every new `connect()` so stale values from a previously connected
  device don't linger on screen.

Device discovery (BLE scanning) is handled directly in `HomeViewModel` via
`BluetoothLeScanner`, filtered by `SERVICE_CONTROL_UUID` — it does not go through
`DeviceRepository`. **`DeviceRepository` currently returns a hardcoded list of
example devices and isn't wired into scanning or connection; don't assume it
reflects real device state.**

### Firmware OTA updates

`FirmwareRepository` orchestrates the full update flow: check current version
(`BluetoothService.getCurrentVersion()`) → compare against the latest GitHub
release (`GithubRepository`, unauthenticated GitHub API calls — see
`GitHubRequestInterceptor`) → download and SHA-256-verify the asset ending in
`_release_4mb_fw.bin` → push it to the device in 512-byte chunks over the
`MC_OTA_DATA_CHAR_UUID` characteristic, gated by start/end/abort commands on
`MC_OTA_CONTROL_CHAR_UUID` → reconnect and poll until the reported firmware
version matches. `BluetoothService.enterOtaUpdateMode()` / `exitOtaUpdateMode()`
suppress the normal "maintain connection" teardown logic while a transfer is in
flight.

### Dependency injection

Everything is wired in a single object, `di/MainModule.kt` (`@InstallIn(SingletonComponent::class)`,
all providers `@Singleton`). There's no per-feature module split — when adding a
new dependency, add its `@Provides` function here.

### Navigation

Single `NavHost` (`navigation/NavGraph.kt`) with three routes (`navigation/Screen.kt`):
Home (device scan/list) → Device (BLE control screen, receives the `TetrisClockDevice`
via `savedStateHandle`) → Firmware (OTA update flow). Each screen package holds its
`Screen` composable and matching `@HiltViewModel`.

## Known rough edges

- `utils/HashUtils.kt` is declared under package `com.yourcompany.yourapp.utils`
  (leftover boilerplate), not `net.agolyakov.tetrisclockble.utils` — imported via
  its actual package name, not its file path.
- ProGuard/R8 minification is currently disabled for release builds
  (`isMinifyEnabled = false` in `app/build.gradle.kts`).
- Some UI-facing strings (mainly OTA progress messages in `FirmwareRepository`)
  are hardcoded Russian rather than pulled from `res/values/strings.xml`.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Android companion app (Kotlin, Jetpack Compose) for a desk clock built around an
ESP32 + RGB LED matrix panel (firmware: https://github.com/golyakoff/Matrix_Clock_ESP32).
The app talks to the clock exclusively over Bluetooth Low Energy — no backend, no
cloud sync. The only network calls of the app are to the GitHub Releases API, used
to check for and download firmware updates. The clock itself (firmware v2.0.0+) may
use the home WiFi, configured from the app: time sync and the firmware update from
the internet.

The firmware repo is checked out next to this one, `../Matrix_Clock_ESP32` (ESP-IDF
5.5 project, see its CLAUDE.md) — read it directly when you need to check the device
side of the BLE protocol (`main/include/ble.h` documents every characteristic) instead
of guessing from this repo alone.

## Commands

On the Linux dev box (~4 GB RAM, no phone attached): `JAVA_HOME` is not set globally,
`gradlew` is not executable in the repo, and a Gradle build must run in a memory-capped
scope so that an OOM kills the build, not the session:

```bash
export JAVA_HOME=~/opt/jdk-21.0.12.1+1
systemd-run --user --scope -q -p MemoryMax=1900M -p MemorySwapMax=768M --setenv=JAVA_HOME=$JAVA_HOME \
    sh ./gradlew --no-daemon -q -Dorg.gradle.workers.max=1 assembleDebug
```

Never run it at the same time as a firmware build. The usual tasks:

```bash
./gradlew assembleDebug          # debug build
./gradlew assembleRelease        # release build (unsigned unless signing props passed, see release.yml)
./gradlew installDebug           # build and install on a connected device/emulator
./gradlew testDebugUnitTest      # JVM unit tests (app/src/test)
./gradlew connectedAndroidTest   # instrumented tests on a device/emulator (app/src/androidTest)
./gradlew lint                   # Android lint
```

JVM unit tests cover the `protocol/` package only (`NetworkCodecTest` — the byte layouts
of the network characteristics as the firmware's `ble.cpp` builds them — and
`TimeZoneMapperTest`); keep them in sync with the firmware. Nothing else has tests.

Single Kotlin/JVM module: everything lives under `app/`. `minSdk = 28`, `targetSdk = 36`,
`compileSdk = 36`. AGP 9 with its built-in Kotlin (no `kotlin-android` plugin, no legacy
`android.*` compatibility flags — they are removed in AGP 10), Hilt on KSP (not kapt: kapt
ran the dev box out of memory), no Parcelize (`TetrisClockDevice` is `Serializable`).
Version bumps live at the top of `app/build.gradle.kts` (`appVersionCode`/`appVersionName`;
the APK is `tetris-clock-vX.Y.Z-<build type>.apk`) and must be paired with a new section at
the top of `CHANGELOG.md`.

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
  protocol/              Pure Kotlin, no Android: models + NetworkCodec of the network characteristics
                         (firmware v2.0.0+), byte reader/writer, TimeZoneMapper (IANA zone -> POSIX TZ)
  data/model/ble/        BLE domain models (TetrisClockDevice, TetrisClockTime, TetrisClockAlarm, ...)
  data/model/github/      GitHub Releases API DTOs
  data/local/             SharedPreferences-backed storage (per-device friendly names)
  data/remote/            Retrofit GitHub API service + auth interceptor
  data/repository/        DeviceRepository (unused placeholder, see below), FirmwareRepository, GithubRepository
  domain/repository/      Repository interfaces (e.g. PreferencesRepository)
  domain/usecase/         Small use cases composed into ViewModels
  service/bluetooth/      BLE GATT layer: TetrisClockBleManager, BluetoothService, per-characteristic handlers
  ui/screen/{home,device,firmware}/  One package per screen: Screen (Composable) + ViewModel;
                         device/ is split by tab: DeviceScreen (scaffold, bottom bar), ClockTab,
                         SettingsTab, NetworkCards (WiFi, firmware), DeviceCards (shared card parts)
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
- **Network characteristics (firmware v2.0.0+)** follow FireLamp_Android's style
  instead of the per-characteristic handlers: they are optional (absent on older
  firmware: `networkSupported` stays false and the UI hides WiFi and the update over
  WiFi), `TetrisClockBleManager` moves their raw bytes (`readNetwork`/`writeNetwork`,
  suspend via Nordic ble-ktx, one `onNetworkNotification` callback), `NetworkCodec`
  decodes them and `BluetoothService` keeps the state flows (time zone, WiFi settings/
  status/scan, update info, OTA status). On every connection the phone's time zone is
  written when it differs. The WiFi settings write needs an encrypted link: the
  manager bonds first (`createBondInsecure`, Android's pairing prompt). The WiFi
  network list is read page by page after the scan-done notification. Never make one
  of them required in `isRequiredServiceSupported()`: older clocks would stop connecting.

Device discovery (BLE scanning) is handled directly in `HomeViewModel` via
`BluetoothLeScanner`, filtered by `SERVICE_CONTROL_UUID` — it does not go through
`DeviceRepository`. **`DeviceRepository` currently returns a hardcoded list of
example devices and isn't wired into scanning or connection; don't assume it
reflects real device state.**

### Firmware OTA updates

Two ways, offered by the Firmware card of the Settings tab once the phone found a newer
release (`DeviceViewModel.checkFirmware()`, run as soon as the clock's version is read):
the clock downloads it itself over WiFi (Command 0x03; `DeviceViewModel.installFirmwareByWifi()`
follows the OTA status notifications, the reboot and the new version, and reports a
bootloader rollback), or the phone sends it over BLE on the Firmware screen as below.

`FirmwareRepository` orchestrates the full update flow: check current version
(`BluetoothService.getCurrentVersion()`) → compare against the latest GitHub
release (`GithubRepository`, unauthenticated GitHub API calls — see
`GitHubRequestInterceptor`) → download and SHA-256-verify the asset ending in
`_release_4mb_fw.bin` or `_release_16mb_fw.bin` (by the clock's flash size) → push it to the device in 512-byte chunks over the
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
via `savedStateHandle`; two bottom tabs, Clock and Settings, each a scrolling column of
cards whose scroll position survives a tab switch) → Firmware (the BLE OTA update flow). Each screen package holds its
`Screen` composable and matching `@HiltViewModel`.

## Known rough edges

- `utils/HashUtils.kt` is declared under package `com.yourcompany.yourapp.utils`
  (leftover boilerplate), not `net.agolyakov.tetrisclockble.utils` — imported via
  its actual package name, not its file path.
- ProGuard/R8 minification is currently disabled for release builds
  (`isMinifyEnabled = false` in `app/build.gradle.kts`).
- Some UI-facing strings (mainly OTA progress messages in `FirmwareRepository`)
  are hardcoded Russian rather than pulled from `res/values/strings.xml`.

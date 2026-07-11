# Matrix Clock Android (Tetris Clock)

Android companion app for a desk clock built around an ESP32 and an RGB LED
matrix panel ([Matrix_Clock_ESP32](https://github.com/golyakoff/Matrix_Clock_ESP32)).
The app talks to the clock over Bluetooth Low Energy (BLE) — no cloud, no
Wi-Fi required for normal use.

## What it does

- Scans for and connects to a Matrix Clock over BLE, with a per-device
  friendly name you can set from the app
- Time sync between the phone and the clock's RTC
- Manual on/off and manual/automatic display brightness control
- Two on/off alarms (e.g. turn the display off at night, on in the morning)
- RTC temperature and aging-offset (accuracy trim) read/write
- Firmware updates (OTA) pushed to the clock over BLE, checked against the
  [firmware repo's](https://github.com/golyakoff/Matrix_Clock_ESP32) GitHub
  releases

## Requirements

- Android 9 (API 28) or newer
- A phone with Bluetooth Low Energy support

## Tech stack

- Kotlin, Jetpack Compose (Material 3)
- Hilt for dependency injection
- [Nordic Android BLE library](https://github.com/NordicSemiconductor/Android-BLE-Library)
  for the GATT communication layer
- Retrofit/OkHttp for checking GitHub Releases (firmware OTA updates)

## Project layout

```
app/src/main/java/net/agolyakov/tetrisclockble/
  data/         Models, local preferences, remote (GitHub) API, repositories
  di/           Hilt modules
  domain/       Repository interfaces and use cases
  service/bluetooth/   BLE GATT manager, characteristic UUIDs, per-characteristic handlers
  ui/           Compose screens (home/device/firmware), theme, shared components
  navigation/   Navigation graph
```

The BLE GATT service UUID and characteristic UUIDs are documented inline as
comments in
[`TetrisClockBleManager.kt`](app/src/main/java/net/agolyakov/tetrisclockble/service/bluetooth/TetrisClockBleManager.kt),
next to the corresponding definitions in the firmware's
[`include/ble.h`](https://github.com/golyakoff/Matrix_Clock_ESP32/blob/main/include/ble.h).

## Building and running

Open the project in Android Studio, or from the command line:

```bash
./gradlew assembleDebug     # debug build
./gradlew installDebug      # build and install on a connected device/emulator
```

## Releasing a signed build

Tagging a commit `vX.Y.Z` (or running the workflow manually) triggers
[`.github/workflows/release.yml`](.github/workflows/release.yml), which
builds a signed release APK and publishes it as a GitHub Release, using the
matching section of [`CHANGELOG.md`](CHANGELOG.md) as release notes. It
needs four repository secrets set under *Settings → Secrets and variables →
Actions*:

- `KEYSTORE_BASE64` — the release keystore, base64-encoded
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

## Related

- Firmware: https://github.com/golyakoff/Matrix_Clock_ESP32

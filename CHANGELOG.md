# Release 1.5.0

- [x] Added a pixel color order setting (RRGGBB vs RRBBGG) to the System card, backed by clock firmware v1.3.0's new BLE characteristic
- [x] The app now auto-reconnects if the clock disconnects (e.g. reboots) while the device screen is open

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v1.4.1...v1.5.0

# Release 1.4.1

- [x] Locked the app to portrait orientation to prevent screen-rotation from restarting the Activity mid-OTA update

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v1.4.0...v1.4.1

# Release 1.4.0

- [x] Added English as a second app language, switchable in-app via "Язык: English | Русский" on the Home screen (works alongside Android's own per-app language setting on Android 13+)
- [x] Slightly reduced card title and clock digit font sizes so longer English strings fit without wrapping

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v1.3.3...v1.4.0

# Release 1.3.3

- [x] Updated Android Gradle Plugin to 9.2.1 and Gradle to 9.6.1
- [x] Migrated to Gradle Daemon toolchain (auto-detected/provisioned JDK for the build daemon)
- [x] Moved `hiltViewModel()` usages to the new `androidx.hilt:hilt-lifecycle-viewmodel-compose` artifact
- [x] Minor Kotlin compiler warning cleanup (annotation targeting, `@OptIn(FlowPreview::class)`)

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v1.3.1...v1.3.3

# Release 1.3.1

- [x] Removed a dead/ineffective manual scroll-capture hint call; Compose already registers scroll capture for scrollable screens on its own
- [x] Note: Android's scrolling screenshot ("Capture more") may still be unavailable on some OEM builds (confirmed on Xiaomi HyperOS) due to a limitation in their long-screenshot detection, unrelated to this app

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v1.3.0...v1.3.1

# Release 1.3.0

- [x] Added a link on the Home screen to check for and open the latest app release on GitHub
- [x] Split the matrix on/off toggle into its own card above the brightness card
- [x] Reordered the brightness card layout and renamed the hourly schedule button

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v1.2.0...v1.3.0

# Release 1.2.0

- [x] Added hourly brightness schedule (24 per-hour levels, requires firmware v1.1.2+)
- [x] Reworked the brightness card layout
- [x] Manual brightness slider is disabled while auto brightness is on

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v1.1.0...v1.2.0

# Release 1.1.0

- [x] Fixed firmware update checks failing with 401 errors
- [x] Fixed device firmware version display and removed the duplicate label
- [x] Fixed stale device data showing after switching devices
- [x] Added app version number on the device list screen

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v1.0.0...v1.1.0

# Release 1.0.0

**Official public release!**

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v0.5.0...v1.0.0

# Release 0.5.0

- [x] Added "Rename device" feature

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v0.4.0...v0.5.0

# Release 0.4.0

- [x]  Added OTA Update functionality
- [x]  Updated look and feel
- [x]  Project structure update

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v0.3.0...v0.4.0

# Release 0.3.0

- [x] Added support for aging offset correction
- [x] Added the display of RTC IC temperature
- [x] Fixed an issue where the On/Off timer did not save when the toggle was clicked

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v0.2.0...v0.3.0

# Release 0.2.0

- [x] Added time synchronization with a mobile phone
- [x] Scheduling for On/Off timers
- [x] Design update for Light/Dark schemes

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v0.1.0...v0.2.0

# Release 0.1.0

- [x] Added ability to switch on/off the clock.
- [x] Introduced option to adjust display matrix brightness.

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/commits/v0.1.0
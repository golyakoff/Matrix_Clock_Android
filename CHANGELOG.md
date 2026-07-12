# Release 1.3.2

- [x] Updated Android Gradle Plugin to 8.13.2
- [x] Migrated to Gradle Daemon toolchain (auto-detected/provisioned JDK for the build daemon)

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v1.3.1...v1.3.2

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
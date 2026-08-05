# Release 1.8.1

- [x] Fixed firmware OTA updates being extremely slow (~0.1% every 10 seconds, ~30 minutes total) and often failing partway with "Failed to send OTA data packet". The negotiated ATT MTU was being pinned at the 23-byte fallback: the MTU exchange runs twice per connection (a GATT cache refresh re-runs it), the first attempt fails on the still-settling connection and completed a one-shot deferred with 23, so the later successful 515-byte exchange was ignored and every chunk was sent as 20 bytes. The app now keeps the best MTU seen instead of pinning the first (failed) result, so chunks are the full 512 bytes (~18-25x faster). Also requests a high-priority (short-interval) connection for the duration of the transfer and retries a failed chunk a few times before aborting, so a single dropped write on a weak link no longer kills the whole update

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v1.8.0...v1.8.1

# Release 1.8.0

- [x] Reworked the "Day splash" card into a "Splash" card matching clock firmware v1.5.0: turning it on now reveals a frequency picker (once a day / every 3 hours / every hour), a duration picker (10 / 20 / 40 / 60 seconds) and the animation picker, with a description under the title that updates to match the selected frequency. "Preview splash" plays the selected animation on the device for the chosen duration
- [x] The animation picker now offers a second animation, "Russian flag" (Флаг России), alongside Nyan Cat

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v1.7.0...v1.8.0

# Release 1.7.0

- [x] Added a "Day splash" card (between Scenarios and System) for clock firmware v1.4.0's new easter egg: an animated screensaver the clock plays over the 00:00 minute at the day boundary instead of the usual Tetris digits. Toggle it on/off, pick the animation from a dropdown (Nyan Cat for now), and preview the selected one on the device right away with the "Preview splash" button

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v1.6.0...v1.7.0

# Release 1.6.0

- [x] The Home screen now shows a hint when no clocks are nearby ("No Tetris Clocks found nearby. Make sure the device is powered on and within 10 meters.") in the device-list area instead of a blank screen

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v1.5.1...v1.6.0

# Release 1.5.1

- [x] Much faster firmware OTA upload: removed the fixed 30 ms pause after every 512-byte chunk (~1.5 min of pure sleep per update); flow control now relies on the clock's own write queue, and the chunk size follows the negotiated MTU

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v1.5.0...v1.5.1

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
# Release 1.2.0

- [x] Added the hourly brightness schedule: 24 per-hour brightness levels, edited in a dialog on the Device screen and stored on the clock (requires firmware v1.1.2 or newer)
- [x] Reworked the brightness card: the matrix on/off switch is no longer attached to the "Яркость" title but has its own "Табло Вкл/Выкл" row, and "Автояркость" now shows the level the schedule holds for the current hour
- [x] The manual brightness slider is disabled while auto brightness is on, since the clock follows the schedule then

**Full Changelog**: https://github.com/golyakoff/Matrix_Clock_Android/compare/v1.1.0...v1.2.0

# Release 1.1.0

- [x] Fixed a hardcoded, now GitHub-revoked personal access token that broke firmware update checks (401 errors, apparent hang) after the ESP32 firmware repo went public - GitHub API calls no longer send any auth token
- [x] Fixed the device firmware version shown on the Device screen (was hardcoded, now read from the device)
- [x] Fixed stale device data (e.g. firmware version) persisting on screen after switching to a different paired device in the same app session
- [x] Removed the duplicate firmware version label on the Device screen
- [x] Added the app's own version number, shown at the bottom of the device list screen

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
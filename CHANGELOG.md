# Changelog

All notable changes to TiltFold are documented here.

## [2.0.0] — In progress\n\n### Release engineering (configured, not yet published)\n- Added a tag- and manually-triggered GitHub Release workflow for installable debug APK previews.\n- Added SHA-256 checksum generation and version/tag validation.\n- Documented the APK distribution channel and the separate requirements for production signing.\n- Added a machine-readable llms.txt project overview and categorized release notes.

### Engineering
- Replaced the single-file sensor demo with a documented Android/Compose project structure.
- Added a normalized tilt model, smoothing, dead-zone handling, and pure JVM unit tests.
- Sensor sampling follows the Activity lifecycle and listeners are released on pause or disposal.
- Added a manual preview path for devices where motion sensor registration is unavailable.
- Rebuilt the preview as two hinged perspective panels, with a soft mode and a flat comparison mode.
- Added responsive scrolling, accessible control semantics, app theme, and adaptive/legacy launcher icons.
- Added Android CI for debug builds and unit tests.

### Product direction
- Clarified that TiltFold is an in-app perspective/fold visual experiment, not a system-wide Android feature or a physically accurate flexible-screen simulator.
- Prioritized honest capability descriptions, predictable interaction, and device compatibility.

## [1.0.0]
- Initial gyroscope/tilt visual prototype.

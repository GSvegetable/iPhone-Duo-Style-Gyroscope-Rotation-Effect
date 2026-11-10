# Contributing to TiltFold

Thanks for taking an interest in TiltFold.

## Before changing code

1. Read README.md for the actual product scope and current limitations.
2. Keep the project focused on an in-app Android motion/perspective experiment.
3. Do not describe the effect as system-wide or as a physically accurate flexible display unless the implementation genuinely supports that claim.

## Development checks

Use JDK 17 and Android SDK Platform 35.

Run the JVM unit tests and assemble the debug APK:

    gradle :app:testDebugUnitTest :app:assembleDebug

## Code guidelines

- Keep sensor math independent of Android UI where practical so it can be unit tested.
- Register sensor listeners only while the Activity is resumed.
- Always unregister listeners and release observers when leaving the active lifecycle.
- Clamp and validate raw sensor values before they reach UI state.
- Preserve a usable manual preview when a device cannot register a motion sensor.
- Keep UI controls accessible to screen readers and operable on small displays.
- Update README.md and CHANGELOG.md when behavior or user-facing controls change.

## Pull requests

A useful pull request includes:
- A concise problem statement.
- A description of the behavior change.
- Unit tests for new motion math or state logic.
- CI passing for unit tests and debug APK assembly.
- Physical-device results when the change affects sensors, rendering, or lifecycle behavior.

Do not claim physical-device validation unless the exact device and Android version were tested.

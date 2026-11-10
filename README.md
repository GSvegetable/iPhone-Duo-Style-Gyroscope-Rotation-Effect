# TiltFold

<p align="center"><strong>A motion-driven perspective fold study for Android.</strong></p>

<p align="center">
  <a href="https://github.com/GSvegetable/iPhone-Duo-Style-Gyroscope-Rotation-Effect/actions/workflows/android-ci.yml"><img alt="Android CI" src="https://github.com/GSvegetable/iPhone-Duo-Style-Gyroscope-Rotation-Effect/actions/workflows/android-ci.yml/badge.svg"></a>
  <a href="https://github.com/GSvegetable/iPhone-Duo-Style-Gyroscope-Rotation-Effect/releases"><img alt="Latest release" src="https://img.shields.io/github/v/release/GSvegetable/iPhone-Duo-Style-Gyroscope-Rotation-Effect?include_prereleases&label=release"></a>
  <img alt="License" src="https://img.shields.io/badge/license-not%20yet%20declared-lightgrey">
</p>

<p align="center">
  <a href="https://github.com/GSvegetable/iPhone-Duo-Style-Gyroscope-Rotation-Effect/releases">Download APK</a> ·
  <a href="https://github.com/GSvegetable/iPhone-Duo-Style-Gyroscope-Rotation-Effect/actions">Build history</a> ·
  <a href="./docs/RELEASING.md">Release process</a> ·
  <a href="./docs/DEVICE_VALIDATION.md">Device test plan</a>
</p>

TiltFold maps device roll to a perspective-driven surface animation. It explores the visual language of a foldable display inside one Android app.

**Important:** TiltFold does not bend the physical screen, change Android system UI, or apply effects to other apps. The renderer is a Compose perspective simulation, not a physically accurate 3D mesh or flexible display model.

## What works

- Reads the gravity sensor, with accelerometer fallback.
- Normalizes and smooths the sensor signal to reduce jitter.
- Stops sensor sampling when the Activity is paused and releases listeners on disposal.
- Offers a manual tilt control when sensor registration is unavailable.
- Includes a two-panel hinge simulation and three preview modes: 3D fold, soft surface, and flat.
- Provides screen-reader labels for the preview, mode selector, and adjustment controls.
- Uses a dedicated TiltFold launcher icon and dark system-bar theme.
- Exposes an intensity control and a live tilt indicator.
- Runs on Android 7.0 (API 24) and newer.

## Download and install

TiltFold APK downloads are distributed through [GitHub Releases](https://github.com/GSvegetable/iPhone-Duo-Style-Gyroscope-Rotation-Effect/releases). The release workflow is now configured to build an installable debug-signed APK and publish a SHA-256 checksum alongside it.

**Release status:** no public APK release has been published yet. Until the first release appears, use the local Android Studio build below.

When a release is available:
1. Open the Releases page and download the APK asset.
2. Verify its SHA-256 hash against SHA256SUMS.txt when provided.
3. Open the APK on your Android device and approve installation from that source if Android asks.
4. Launch TiltFold and try sensor mode or manual preview.

The automated APK is a debug-signed evaluation build, not a production Play Store package. See [the release policy](./docs/RELEASING.md).

## Requirements

- Android Studio with JDK 17.
- Android SDK Platform 35.
- Gradle 8.9 or compatible Gradle 8.x.
- A physical Android device is recommended for motion input. Manual preview is available without a usable sensor.

## Run locally

1. Clone the repository.
2. Open the project root in Android Studio.
3. Allow Gradle sync and install the Android 35 SDK if prompted.
4. Run the app configuration on a device or emulator.

Command-line checks, after configuring the Android SDK:

    gradle :app:testDebugUnitTest :app:assembleDebug

## Project structure

    .
    ├── .github/workflows/\n    │   ├── android-ci.yml\n    │   └── android-release.yml
    ├── app/
    │   ├── build.gradle.kts
    │   └── src/
    │       ├── main/
    │       │   ├── AndroidManifest.xml
    │       │   ├── java/com/tiltfold/demo/
    │       │   │   ├── MainActivity.kt
    │       │   │   ├── TiltFold.kt
    │       │   │   └── TiltMath.kt
    │       │   └── res/values/strings.xml
    │       └── test/java/com/tiltfold/demo/TiltMathTest.kt
    ├── build.gradle.kts
    ├── gradle.properties
    ├── settings.gradle.kts
    ├── CHANGELOG.md
    └── README.md

## Motion model

The app uses the gravity sensor when available and falls back to the accelerometer. The X component is normalized by the measured gravity-vector magnitude, clamped to [-1, 1], passed through a first-order low-pass filter, and given a small dead zone around neutral.

The sensor listener is active only while the Activity is resumed. On pause or disposal, the listener is unregistered and the filtered value is reset. This avoids background sampling and stale tilt state when returning to the app.

## Preview modes

| Mode | Behavior |
| --- | --- |
| 3D fold | Applies a perspective rotation to the preview surface. |
| Soft curve | Uses a restrained scale response to suggest surface flex. |
| Flat | Shows the untransformed comparison surface. |

Intensity scales the visual response without changing the sensor input itself. If no compatible sensor can be registered, the manual slider provides a deterministic preview.

## Compatibility and limitations

- Minimum Android version: API 24.
- Target SDK: 35.
- Sensor availability and calibration vary by device.
- The current visual is rendered inside the app using Jetpack Compose graphics layers.
- This is not an OS-level feature and cannot reshape other applications.
- The current implementation is an illustrative perspective effect, not a full 3D mesh simulation.
- Physical-device validation across multiple screen sizes and sensor implementations is still required.

## Quality checks

See [CONTRIBUTING.md](./CONTRIBUTING.md) for development rules and [docs/DEVICE_VALIDATION.md](./docs/DEVICE_VALIDATION.md) for the physical-device test plan. The device checklist is a plan, not a statement that those devices have already been tested.

GitHub Actions runs:
- Android debug APK assembly.
- JVM unit tests for tilt normalization, invalid sensor values, smoothing, and reset behavior.

A passing CI build is not a substitute for testing on real hardware.

## Version history

See [CHANGELOG.md](./CHANGELOG.md) for version history. The current source version is 2.0.0; the first downloadable preview release still needs to be published through Actions → Android Release.

## Distribution and package registry

TiltFold is an installable Android application, not a reusable Android library. APK files belong in GitHub Releases. The GitHub Packages tab is intentionally unused; publishing an empty or misleading Maven package would not help users install the app. If the project later extracts reusable functionality into an Android library module, Maven publishing can be added then.

## License

No license is currently declared. Until a license file is added, normal copyright restrictions apply; do not assume the code is public domain or freely reusable.

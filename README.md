# TiltFold

<p align="center"><strong>A motion-driven perspective fold study for Android.</strong></p>

<p align="center">
  <a href="https://github.com/GSvegetable/iPhone-Duo-Style-Gyroscope-Rotation-Effect/actions">Build status</a> ·
  <a href="https://github.com/GSvegetable/iPhone-Duo-Style-Gyroscope-Rotation-Effect/pulls">Pull requests</a> ·
  <a href="https://github.com/GSvegetable/iPhone-Duo-Style-Gyroscope-Rotation-Effect/releases">Releases</a>
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
    ├── .github/workflows/android-ci.yml
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
| Off | Removes the motion-driven transform. |

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

See CHANGELOG.md. Version 2.0.0 represents the ongoing project restructuring and stabilization work.

## License

No license is currently declared. Until a license file is added, normal copyright restrictions apply; do not assume the code is public domain or freely reusable.

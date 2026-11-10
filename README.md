# TiltFold

A small Android motion experiment that turns device roll into a perspective-driven display fold. TiltFold is an independent visual study inspired by foldable-screen transitions; it is not an Apple product and does not reproduce system-wide display behavior.

## Features

- Live left/right tilt input from the Android gravity sensor.
- Accelerometer fallback when a gravity sensor is unavailable.
- A smoothed motion signal to reduce hand jitter.
- Three preview modes: perspective fold, soft surface, and flat.
- Adjustable effect intensity and a manual preview control on devices without a supported motion sensor.
- A standard Android Studio / Gradle project layout.

## Requirements

- Android Studio with JDK 17.
- Android SDK 35 installed.
- Android 7.0 (API 24) or newer.
- A physical device is recommended for motion input. The manual slider remains available when no compatible motion sensor is present.

## Build

1. Clone this repository.
2. Open the repository root in Android Studio.
3. Allow Gradle sync to finish.
4. Select the `app` run configuration and launch it on a device.

Command-line build (after installing the Android SDK and configuring `ANDROID_HOME`):

```bash
gradle :app:assembleDebug
```

## Project layout

```text
.
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/tiltfold/demo/
│       │   ├── MainActivity.kt
│       │   └── TiltFold.kt
│       └── res/values/strings.xml
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

## Motion model

The gravity vector's X component is normalized into a range of -1 to 1. A first-order low-pass filter smooths incoming samples before the UI uses them. Sensor listeners are registered and released with the composable lifecycle to avoid leaking callbacks when the screen leaves composition.

The effect is intentionally presented as a perspective transform rather than claiming to be a physically accurate flexible display simulation. Device orientation, sensor calibration, display size, and manufacturer-specific sensor behavior can all affect the result.

## Limitations

- This is an in-app visual effect. Android does not allow an ordinary app to reshape every other app's UI or the system launcher this way.
- The current renderer uses Compose layer transforms and gradients; it is not a full 3D mesh or a physically based glass simulation.
- The project has not yet been validated across a representative device matrix. Contributions and device-specific testing are welcome.

## License and attribution

No Apple-owned assets are included. The name and visual direction are descriptive references only; this project is not affiliated with or endorsed by Apple.

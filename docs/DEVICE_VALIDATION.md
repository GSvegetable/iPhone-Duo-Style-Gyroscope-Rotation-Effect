# Physical Device Validation

This document is a test plan, not a claim that the listed devices have already been tested.

## Device coverage

| Class | Target coverage | Why it matters |
| --- | --- | --- |
| Pixel / near-stock Android | Android 14 or newer | Baseline sensor and lifecycle behavior |
| Samsung Galaxy / One UI | Android 13 or newer | Vendor sensor tuning and display behavior |
| Xiaomi / HyperOS | Android 13 or newer | Vendor background and sensor behavior |
| Android emulator | API 24 and API 35 images | Installability, layout, and no-sensor fallback |

## Manual test checklist

### Sensor behavior
- [ ] Tilt left and right; the preview moves in the expected direction.
- [ ] Return the device to level; the preview settles without visible jitter.
- [ ] Hold a steady angle for 10 seconds; the preview remains stable.
- [ ] Rotate the device through supported orientations and confirm the signal remains intuitive.
- [ ] Confirm the UI reports manual mode if sensor registration fails.

### Lifecycle and resource use
- [ ] Open the app, background it, wait 30 seconds, and return.
- [ ] Lock and unlock the device while the app is open.
- [ ] Confirm the preview returns to neutral after pause and resumes sampling.
- [ ] Repeatedly navigate away and return; confirm no duplicated sensor response.

### Layout and accessibility
- [ ] Check narrow phone, large phone, tablet, and landscape layouts.
- [ ] Confirm controls remain reachable by scrolling on short screens.
- [ ] Verify TalkBack can identify the preview, mode choices, and sliders.
- [ ] Verify the screen remains usable with large font/display scaling.
- [ ] Confirm contrast remains legible in light from the screen and system bars.

### Manual fallback
- [ ] Use the app on an emulator without a motion sensor.
- [ ] Move the manual tilt slider across its full range.
- [ ] Change intensity and each render mode.
- [ ] Confirm the UI does not crash when sensor registration returns false.

## Reporting results

For each device, record:
- Manufacturer and model
- Android version and security patch
- Screen size and display scaling
- Sensor type selected (gravity or accelerometer)
- Pass/fail per checklist section
- Reproduction steps and logcat output for failures

Do not mark a device as supported based only on a successful CI build. CI validates compilation and unit tests; this checklist validates real sensor and display behavior.

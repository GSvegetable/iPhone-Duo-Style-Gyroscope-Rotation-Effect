# Release process

TiltFold is an Android application, so its user-facing distribution format is an APK attached to a GitHub Release. It is not published as a Maven package: this repository does not currently expose a reusable Android library.

## Current release channel

The automated workflow produces a debug-signed APK for evaluation and sideload testing. It is not a production Play Store artifact. Do not describe it as production-signed.

## Create a preview release

1. Update versionName in app/build.gradle.kts to the next semantic version, for example 2.0.0.
2. Update versionCode to a unique, higher integer.
3. Update CHANGELOG.md with user-visible changes and known limitations.
4. Merge the tested changes into main.
5. Open Actions → Android Release → Run workflow.
6. Enter a version such as 2.0.0-preview.1.

The workflow:
- Checks the requested version against the app's versionName.
- Runs JVM unit tests.
- Builds an installable debug APK.
- Generates a SHA-256 checksum.
- Creates a GitHub Release and uploads both files.
- Marks versions containing a hyphen as prereleases.

A tag push such as v2.0.0-preview.1 can also trigger the same workflow.

## Before a stable production release

- Complete the physical-device validation plan in DEVICE_VALIDATION.md.
- Configure a protected Android release keystore in GitHub Actions secrets.
- Add a release signing configuration without committing keystore files or passwords.
- Build and verify a minified release variant.
- Test upgrade/install behavior and record the tested device matrix.
- Only then publish a production-signed APK and remove the preview-only wording.

Never commit signing keys, passwords, access tokens, or local keystore files.

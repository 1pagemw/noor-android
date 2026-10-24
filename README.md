# Noor Android

Noor is a native Android learning app for secondary school students in Malawi. This first Compose scaffold follows the modern visual concepts in [`docs/design/`](docs/design/) and the palette in [`docs/design-system.md`](docs/design-system.md).

## Development environment

Open the repository in GitHub Codespaces. The dev container provides Java 17, Gradle tooling, Kotlin and GitHub Copilot editor support. The Android SDK installer is available in `.devcontainer/install-android-sdk.sh`; run it in the Codespace terminal when ready. It asks you to review and accept Google Android SDK licenses interactively. Do not run it unless you agree to those license terms.

The Codespace is for editing and building. For device checks, download the debug APK from the `noor-debug-apk` artifact on a successful GitHub Actions run and install it on an Android phone.

## Build and test

```bash
./gradlew testDebugUnitTest assembleDebug
```

The Android CI workflow runs on pushes to `main` and `feature/**`, on pull requests into `main`, and through manual workflow dispatch. It runs unit tests, builds a debug APK, and stores the APK artifact for seven days.

## GitHub workflow

Track bugs and feature requests with Issues. Work in a feature branch and open a Pull Request to `main`. Dependabot checks Gradle and GitHub Actions dependencies weekly. Avoid storing signing keys or passwords in the repository; signed release builds will use GitHub Secrets when release publishing is added.

## Current scaffold

The app includes a four-tab shell (Today, Learn, Review, Progress), sample subject rows, and a sample biology quiz/feedback flow. Names, topics, and progress figures are illustrative only. They are not real student data or confirmation of curriculum alignment. The app does not yet have persistent storage, authentication, downloaded lessons, or official curriculum content.

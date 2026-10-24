# Noor Android

Noor is a native Android learning app for secondary school students in Malawi. Version `0.1.0` is the Form 3 pilot for Biology and Physics. It follows the palette in [`docs/design-system.md`](docs/design-system.md) and keeps the five screen references in [`docs/design/`](docs/design/).

## What works in v0.1.0

- First-run setup for the Form 3, English-language pilot; no account is required.
- Biology and Physics lesson-pack catalogue with pack downloads over HTTPS.
- Downloaded packs are checked against catalogue SHA-256 hashes, saved inside the app, and readable offline.
- Each subject pack has a short lesson, key terms, and two explained multiple-choice questions per topic.
- On-device lesson completion, quiz results, due-topic review schedule, progress summary, and text-size adjustment.
- Settings to remove saved packs or clear progress while retaining downloads.

The Biology pack has six starter topics and the Physics pack has seven. Curriculum headings are provisional and the learning material is marked **Draft — local teacher review needed**. See [`docs/content/form3-scope.md`](docs/content/form3-scope.md) before using the content with students. A local Biology and Physics teacher should verify the syllabus mapping, explanations, terminology, calculations, and questions before classroom use.

## Development environment

Open the repository in GitHub Codespaces. The dev container provides Java 17, Gradle tooling, Kotlin, and GitHub Copilot editor support. The Android SDK installer is `.devcontainer/install-android-sdk.sh`; it asks you to review and accept Google Android SDK licenses interactively. Do not run it unless you agree to those license terms.

The core learning flow needs no account or server. A student needs an internet connection once to download a subject pack. After that, lessons, quizzes, and local progress work offline. Removing a pack frees its local copy; progress is stored separately on the device.

## Build, test, and content validation

```bash
python3 scripts/validate_content_packs.py
./gradlew testDebugUnitTest assembleDebug
```

GitHub Actions runs the content validator and Android unit tests, builds a debug APK, and uploads it as the `noor-debug-apk` artifact for seven days. The APK is a test build and is not release-signed.

## Versioning and GitHub workflow

Android `versionName` is `0.1.0` and `versionCode` is `1`. Keep `versionName` semantic and increase `versionCode` monotonically for each distributable build. Signed release builds, Git tags, and Play-ready bundles will be added before wider distribution; signing keys belong in GitHub Secrets, never in the repository.

The repository is public. Changes should use feature branches and Pull Requests. `main` requires a passing `android-verify-build` check and resolved Pull Request conversations; force-pushes and branch deletion are disabled. Issues, Dependabot, and the Pull Request template support project planning and maintenance.

## Current limitations

Noor does not yet provide official curriculum approval, full syllabus coverage, accounts, cloud sync, teacher dashboards, notifications, or a signed public release. Student progress stays on the phone and is removed if app data is cleared or the app is uninstalled.

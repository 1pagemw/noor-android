# Noor Android

Noor is a native Android learning app for secondary school students in Malawi. Version `0.4.3` is the Form 3 Biology and Physics development pilot. It follows the palette in [`docs/design-system.md`](docs/design-system.md) and keeps the screen references in [`docs/design/`](docs/design/).

## What works in v0.4.3

- First-run setup for the Form 3, English-language pilot; no account is required.
- Metadata-only Biology and Physics catalogues with independently downloaded lesson units over HTTPS.
- Lessons save automatically when opened; WorkManager can prefetch the next lesson over Wi-Fi or mobile data.
- Downloaded lesson units are checked against catalogue SHA-256 hashes, saved inside the app, and readable offline.
- Each lesson includes key terms and its explained multiple-choice questions.
- On-device lesson completion, quiz results, due-topic review schedule, progress summary, and text-size adjustment.
- Profile controls to remove individual offline lessons or clear progress while retaining saved content.
- Three-tab navigation, private profile statistics, due-review cards, and progress-shaded topic cards.
- Local study streak tracking after lesson completion or finished practice.
- Consistent offline-availability labels, expandable search results, non-duplicated Profile metrics, and a single Review action per due topic.
- Accessible quiz selection and answer feedback; offline lesson completion notices persist when the lesson is reopened.
- Profile text-size controls and retry actions use the app palette and shared button treatment; saved-lesson removal failures are surfaced.
- Student-facing screens omit internal reviewer workflow badges; search can be cleared, Today’s path names individual lessons, empty Review state is consolidated, and saved-lesson deletion asks for confirmation.
- Legacy subject-pack migration validates both the installed file and its backup, uses a valid backup when the primary is corrupt, and verifies current-version SHA-256 before extraction; source files remain until migration completes.
- Per-lesson storage checks and staged file replacement that preserves the current copy if an update fails.

The Biology pack has six starter topics and the Physics pack has seven. Reviewer workflow labels are hidden in the student app, but the lesson material remains provisional and requires syllabus and teacher verification before classroom use. See [`docs/content/form3-scope.md`](docs/content/form3-scope.md) for the content scope and review requirements.

## Development environment

Open the repository in GitHub Codespaces. The dev container provides Java 17, Gradle tooling, Kotlin, and GitHub Copilot editor support. The Android SDK installer is `.devcontainer/install-android-sdk.sh`; it asks you to review and accept Google Android SDK licenses interactively. Do not run it unless you agree to those license terms.

The core learning flow needs no account or live service. A student needs an internet connection to open a lesson for its first download; the next lesson may be prefetched in the background. Saved lessons and their quizzes work offline. Removing a lesson frees its local copy; progress is stored separately on the device.

## Build, test, and content validation

```bash
python3 scripts/validate_content_packs.py
./gradlew testDebugUnitTest assembleDebug
```

GitHub Actions runs the content validator and Android unit tests, builds a debug APK, and uploads it as the `noor-debug-apk` artifact for seven days. The APK is a test build and is not release-signed.

## Versioning and GitHub workflow

Android `versionName` is `0.4.3` and `versionCode` is `7`. Keep `versionName` semantic and increase `versionCode` monotonically for each distributable build. CI debug APKs use the stable development signing key; this is not a production signing configuration. Signed release builds, Git tags, and Play-ready bundles will be added before wider distribution; signing keys belong in GitHub Secrets, never in the repository.

The repository is public. Changes should use feature branches and Pull Requests. `main` requires a passing `android-verify-build` check and resolved Pull Request conversations; force-pushes and branch deletion are disabled. Issues, Dependabot, and the Pull Request template support project planning and maintenance.

## Current limitations

Noor does not yet provide official curriculum approval, full syllabus coverage, accounts, cloud sync, teacher dashboards, notifications, or a signed public release. Low-storage, airplane-mode, and intermittent-network handling still need manual device verification. Student progress stays on the phone and is removed if app data is cleared or the app is uninstalled.

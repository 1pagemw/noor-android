# Development log

## 2026-11-08 — CI: run Android CI on feature/android-scaffold

- **Scope:** Add `feature/android-scaffold` to the existing `push` and `pull_request` branch filters in `.github/workflows/android-ci.yml`, retaining `main` and `workflow_dispatch`.
- **Decision:** Enable automatic CI for the repository’s app-development branch and PR base without changing jobs, steps, permissions, build logic, or app code.
- **Deviations:** None in the workflow. This is the sole documentation file; its entry is required by the standing per-task devlog protocol and did not exist on the base branch.
- **Checks:** `git diff --check` passed. A standard-library comparison confirmed the workflow equals base plus exactly two branch entries. PyYAML was unavailable, so local YAML parsing could not be performed. Manual workflow run [42467724387](https://github.com/1pagemw/noor-android/actions/runs/42467724387) reached the CI step `Run unit tests and assemble debug APK`, but all four attempts failed before Gradle could run the tests/build, due to Gradle distribution download timeouts or HTTP 504 responses. The Android UI-test and APK-upload steps did not run.
- **Unresolved:** A successful manual CI run is still required. PR #2 remains without an automatic run until this PR merges; after merge, merge `feature/android-scaffold` into `polish/quiz-shuffle-and-destructive-styling` to trigger CI for PR #2. Do not merge PR #2 before Vibe re-reviews its CI results.

# Development log

## 2026-11-08 — CI: run Android CI on feature/android-scaffold

- **Scope:** Add `feature/android-scaffold` to the existing `push` and `pull_request` branch filters in `.github/workflows/android-ci.yml`, retaining `main` and `workflow_dispatch`.
- **Decision:** Enable automatic CI for the repository’s app-development branch and PR base without changing jobs, steps, permissions, build logic, or app code.
- **Deviations:** None in the workflow. This is the sole documentation file; its entry is required by the standing per-task devlog protocol and did not exist on the base branch.
- **Checks:** `git diff --check` passed. A standard-library comparison confirmed the workflow equals base plus exactly two branch entries. PyYAML was unavailable, so local YAML parsing could not be performed. Manual workflow run [42467724387](https://github.com/1pagemw/noor-android/actions/runs/42467724387) reached the CI step `Run unit tests and assemble debug APK`, but all four attempts failed before Gradle could run the tests/build, due to Gradle distribution download timeouts or HTTP 504 responses. The Android UI-test and APK-upload steps did not run.
- **Unresolved:** A successful manual CI run is still required. PR #2 remains without an automatic run until this PR merges; after merge, merge `feature/android-scaffold` into `polish/quiz-shuffle-and-destructive-styling` to trigger CI for PR #2. Do not merge PR #2 before Vibe re-reviews its CI results.
- **Automatic PR check:** GitHub also triggered `pull_request` run [42467922807](https://github.com/1pagemw/noor-android/actions/runs/42467922807) on `9e2cd87`, confirming the workflow was recognized and executed for PR #3. It reached the unit-test/build step but timed out fetching Gradle; no tests or build completed. The exact error is in PR #3’s Checks section.

## 2026-11-08 — Task #1: quiz shuffle and profile polish

- **Scope:** Shuffle Quick Check question and answer-option order per practice session; make the correct-option mapping stable; style the clear-progress action as destructive; snap text size to five values; label profile accuracy as all-time.
- **Decisions:** Use a pure seeded shuffle helper and save the session seed with `rememberSaveable(topic.id)`, so the same quiz layout is reconstructed after process recreation. Wrap shuffled data while retaining the original question for prompt/explanation and remap the correct answer index. Reuse the existing `NoorColors.Error`; set Slider `steps = 3`. Keep quiz scoring, review scheduling, repository/storage behavior, dialog copy, and clear-progress behavior unchanged.
- **Deviations:** None from the approved Task #1 reference implementation. No dependency or version change.
- **Checks:** `python3 scripts/validate_content_packs.py` passed for both Form 3 packs and 13 lesson units. `git diff --check` passed. Local `./gradlew test` and `./gradlew assembleDebug` were attempted but could not run in this sandbox; exact failure output is recorded in the PR Checks section.
- **Unresolved at implementation time:** The Android CI workflow currently filters pull requests to base branch `main`, while this task requires a PR targeting `feature/android-scaffold`; automatic PR CI may therefore not run. Manual branch dispatch will be attempted and reported. No Android device or emulator is attached (`adb` is not installed), so the requested mid-quiz process-death and UI smoke checks remain pending device verification.
- **Known limitation left in scope:** randomized positions reduce rote answer-letter memorization but do not increase the small topic question banks or otherwise make a short quiz a strong mastery measure; content depth remains deferred for verified content work.

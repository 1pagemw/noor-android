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
## 2026-11-08 — Merge PR #3 and sync PR #2

- **Merges:** PR #3 merged with merge commit `c4776373e19d0625ee95d03a08f32070cd321340`. PR #2 branch synced with merge commit `2c008d466f70319490726ebdefb4d5058a4c0d99` (parents: Task #1 head `3d9bb46` and the PR #3 merge commit).
- **Conflict resolution:** The sole conflict was add/add in `docs/devlog.md`. Resolved by retaining the CI-trigger entry first and the Task #1 entry second under one `# Development log` heading; no other path conflicted. No application source was changed during the sync.
- **CI:** Base-branch push run [42488277932](https://github.com/1pagemw/noor-android/actions/runs/42488277932) and pull-request run [42488281519](https://github.com/1pagemw/noor-android/actions/runs/42488281519) both passed unit-test/assemble, Android UI-test, and APK-upload steps on base commit `c477637`. PR #2 run [42488371460](https://github.com/1pagemw/noor-android/actions/runs/42488371460), started 2026-11-08T14:09:05Z, failed at Kotlin compilation before tests/build completed. Exact compiler output: `NoorApp.kt:69:35 Unresolved reference 'TextButtonDefaults'.` and `NoorApp.kt:1401:30 Unresolved reference 'TextButtonDefaults'.` UI tests and APK upload were skipped. This is a code compilation failure, not a Gradle download timeout; no retry or code edit was made under the handoff's scope limits.
- **Unresolved:** PR #2 remains open and must not merge. Its code has not passed CI; the unresolved references require owner/Vibe review and an authorized code correction before a successful PR #2 verification run and phone smoke test.


## 2026-11-09 — Task #1 compile fix (PR #2)

- **Scope and fix:** Commit `1fa8931b39fcbfa0e7778265764ec96dfd8f1986` removes the invalid `TextButtonDefaults` import and uses the already-imported `ButtonDefaults.textButtonColors(...)` in the clear-progress confirmation dialog. This is an API-only correction: the visual result remains the error-colored “Clear progress” text; no other behavior was changed.
- **Verification:** `git diff --check` passed. Automatic PR check [42574583065](https://github.com/1pagemw/noor-android/actions/runs/42574583065) on this commit completed successfully. `Run unit tests and assemble debug APK` passed, including `:app:testDebugUnitTest` and `:app:assembleDebug` (`BUILD SUCCESSFUL in 3m 3s`; 43 actionable tasks). `Run Android UI tests` passed: `:app:connectedDebugAndroidTest`, 10 tests completed, 0 skipped, 0 failed (`BUILD SUCCESSFUL in 8m 43s`; 67 actionable tasks). `Upload debug APK` succeeded and created artifact [noor-debug-apk, ID 13655970033](https://github.com/1pagemw/noor-android/actions/runs/42574583065/artifacts/13655970033).
- **Unresolved / gate:** PR #2 remains open and unmerged. Vibe's re-review and drmwrld's on-phone smoke test of the uploaded APK are still pending; do not merge until those gates are satisfied.


## 2026-11-09 — Previous lesson navigation and future class selection

- **Scope:** Add a Previous lesson action to quiz results, next to the existing lesson-sequence actions, without changing quiz completion or progress semantics. Keep navigation within the active subject pack's ordered lesson index; do not wrap at the first lesson or cross between forms.
- **Decision:** The Previous lesson action is available only when an earlier lesson exists. It returns to that lesson through the existing lesson route callback. Add unit and Android UI regression coverage for previous/next actions and no-wrap boundaries.
- **Future form picker recommendation (not implemented):** Keep the pilot Form 3-only experience free of a redundant picker. When Form 4 content is available, provide one persistent, locally stored active-form selector shared across learning screens; offer only forms with usable content and retain progress separately per form. A first-run chooser is optional only when more than one form is available. The specific picker location and whether Today/Profile statistics are filtered or combined remain product-owner decisions.
- **Checks:** Pending automated CI for the implementation commit. Local sandbox does not have the Android SDK, so Gradle tests/build cannot be run here.
- **Unresolved:** PR #2 remains open and unmerged pending Vibe review and owner phone smoke test; the picker recommendation needs owner approval before implementation.

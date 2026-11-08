# Development log

## 2026-11-08 — Task #1: quiz shuffle and profile polish

- **Scope:** Shuffle Quick Check question and answer-option order per practice session; make the correct-option mapping stable; style the clear-progress action as destructive; snap text size to five values; label profile accuracy as all-time.
- **Decisions:** Use a pure seeded shuffle helper and save the session seed with `rememberSaveable(topic.id)`, so the same quiz layout is reconstructed after process recreation. Wrap shuffled data while retaining the original question for prompt/explanation and remap the correct answer index. Reuse the existing `NoorColors.Error`; set Slider `steps = 3`. Keep quiz scoring, review scheduling, repository/storage behavior, dialog copy, and clear-progress behavior unchanged.
- **Deviations:** None from the approved Task #1 reference implementation. No dependency or version change.
- **Checks:** `python3 scripts/validate_content_packs.py` passed for both Form 3 packs and 13 lesson units. `git diff --check` passed. Local `./gradlew test` and `./gradlew assembleDebug` were attempted but could not run in this sandbox; exact failure output is recorded in the PR Checks section.
- **Unresolved at implementation time:** The Android CI workflow currently filters pull requests to base branch `main`, while this task requires a PR targeting `feature/android-scaffold`; automatic PR CI may therefore not run. Manual branch dispatch will be attempted and reported. No Android device or emulator is attached (`adb` is not installed), so the requested mid-quiz process-death and UI smoke checks remain pending device verification.
- **Known limitation left in scope:** randomized positions reduce rote answer-letter memorization but do not increase the small topic question banks or otherwise make a short quiz a strong mastery measure; content depth remains deferred for verified content work.

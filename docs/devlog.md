# Development log

## 2026-11-08 — CI: run Android CI on feature/android-scaffold

- **Scope:** Add `feature/android-scaffold` to the existing `push` and `pull_request` branch filters in `.github/workflows/android-ci.yml`. Keep the existing `main` entries and `workflow_dispatch` trigger.
- **Decision:** Enable automatic CI for the repository’s actual app-development branch and PR base, without changing jobs, steps, permissions, build logic, or app code.
- **Deviations:** None in the workflow. This file is the sole documentation addition, required by the standing per-task devlog protocol; it was absent from the feature-branch base.
- **Checks:** Whitespace/YAML validation and a manual workflow dispatch are pending.
- **Unresolved:** The manual run must reach the unit-test step or beyond. PR #2 remains without an automatic run until this PR merges; after merge, merge `feature/android-scaffold` into `polish/quiz-shuffle-and-destructive-styling` to trigger CI for PR #2. Do not merge PR #2 before Vibe re-reviews its CI results.

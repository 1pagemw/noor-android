# Noor product roadmap

## Product direction

Noor should help secondary-school students in Malawi learn and revise curriculum content on Android phones, including when mobile data is limited or unavailable. Build a dependable learning loop before adding broad catalogs, accounts, social features, or automation:

1. Choose a form, subject, and topic.
2. Learn from a short, clear lesson.
3. Check understanding with practice questions and explanations.
4. Review topics that need another try.
5. See progress and choose a useful next step.

The `feature/android-scaffold` branch advances the Form 3 Biology and Physics pilot to v0.4.2. In addition to the offline learning loop and local progress, it uses three primary tabs, shows four summary metrics in Profile, distinguishes due-review cards from other saved topics, shades topic cards by lesson completion, adds an on-device study streak, and replaces whole-subject downloads with lesson-level units. The starter subjects contain six Biology topics and seven Physics topics, each with one short lesson and two questions.

**Current status:** the code and draft packs are not yet an approved classroom pilot. Topic scope is provisional, the current official syllabus documents have not been verified, and no Malawi teacher has reviewed the lessons. CI and manual Android/offline device checks must pass before calling the alpha exit gate complete. See [`content scope and sources`](content/form3-scope.md) and [`versioning policy`](versioning.md).

## Product principles

- **Offline-first and data-conscious:** reading, practice, and review should work without a live connection after content is on the phone. Keep downloads small and let students see and remove stored content.
- **Curriculum-led:** map lessons to the current Malawi secondary curriculum and any relevant examination objectives. Have a qualified local teacher review content and explanations before students rely on them.
- **Private by default:** let students use the core app without an account. Collect as little personal information as possible; no advertising, public profiles, direct messaging, or location collection in the MVP.
- **Accessible on everyday phones:** test on lower-cost Android devices, slow connections, small screens, and limited storage. Make text readable and controls easy to tap.
- **Encouraging, not pressuring:** use useful feedback and private progress, not public rankings or punitive streaks.

## Phase 0 — agree the pilot and foundations (scope selected; review still open)

The initial scope is Form 3 Biology and Physics, English-first, account-free, and offline-capable. Before writing a large library, confirm the current curriculum and exam references, content licensing, who will review lessons, preferred language(s), and what devices/connectivity the pilot should support.

Prepare the content model and local storage so a lesson pack can be updated without requiring students to be online. Write a simple content-review checklist for correctness, clarity, language, and rights to any diagrams or past-paper questions. Decide what feedback can be collected without identifying a child.

### App versioning foundation

The current development build advances the scaffold to `versionName = 0.4.2` and `versionCode = 6`. Keep `versionName` semantic and human-readable (`0.x.y` during pilot development; `1.0.0` for the first stable public release). Increase Android `versionCode` monotonically for every distributable build so a newer test build can update an older one. Tag release source with matching Git tags such as `v0.1.0` and `v1.0.0`.

The Actions workflow is for CI and debug APK artifacts. Repository builds use a stable development-only signing key from GitHub Secrets when available so newer development APKs can update earlier ones; this is not a production release key. Before distributing builds beyond the development team, add a separate release workflow that builds a signed APK/AAB from a version tag, creates release notes, and keeps debug and release builds clearly separate.

**Exit gate:** Form 3, Biology, Physics, English-first, and the account-free privacy approach are selected. The current official curriculum source, named teacher reviewer, content-rights policy, and device expectations remain to be confirmed.

## Phase 1 — MVP / closed alpha

Deliver one complete, useful learning loop for the agreed pilot scope rather than many empty subject pages.

- First-run setup: select form and preferred content language; skip account creation and collect no unnecessary personal details.
- Subject and topic library with clear curriculum labels and an obvious “continue learning” path.
- Short lesson pages using lightweight text, diagrams, and examples; show key terms and a clear completion action.
- Lesson-level downloads triggered on open, one next-lesson prefetch, per-lesson storage controls, and clear offline availability indicators; downloads may use Wi-Fi or mobile data.
- Multiple-choice practice with immediate correct/incorrect feedback and a short explanation. Keep question content local with the lesson pack.
- A local review queue for missed or due topics, with a simple repeat-practice flow.
- Private on-device progress: completed lessons, recent practice, and a simple topic-level summary. Explain that progress is stored on the phone.
- Basic settings: form, language/content preference, text-size choice, storage management, and help/feedback route.
- Automated checks for quiz logic, content parsing, and key offline flows; manual checks on at least one lower-cost Android device.

**Not in this phase:** sign-in, cloud sync, teacher dashboards, live classes, chat, public rankings, payments, or an AI tutor.

**Alpha exit gate:** a student can open lessons to save them, use saved lessons and quizzes in airplane mode, close and reopen the app without losing local progress, and understand why an answer is correct. A teacher has reviewed the pilot content.

## Phase 2 — school or community pilot

Run a time-bounded pilot with a small, consenting group of students and educators. Use feedback to correct content and simplify confusing flows before expanding the library.

- Complete and teacher-review the agreed pilot form/subject coverage; label content by form, topic, and source/version.
- Add search and a lightweight glossary if students need them in observed use.
- Add more question formats only where useful and practical on phones (for example, numeric answers for mathematics after multiple-choice is reliable).
- Add opt-in, local study reminders only if students and educators want them; reminders must be easy to turn off.
- The MVP now includes a lesson-level report composer. It opens Android's share chooser with form, subject, topic, lesson, content version, and optional student-entered details; Noor does not send automatically or attach account/device identity. Define an official teacher destination and report-handling process before pilot use.
- **Implemented in v0.3.0 (initial reliability slice):** detect a newer catalogue pack while keeping the older downloaded version usable, stage and validate updates before replacing the installed file, recover backups after an interrupted swap, and check available storage before a download/update. The broader retry/offline/network and low-storage matrix still needs device testing.
- **Implemented in v0.4.0 (lesson-level download slice):** bundle a metadata-only lesson catalogue, fetch individual lesson payloads on demand, queue a sequential next-lesson prefetch, allow connected Wi-Fi or mobile networks, validate SHA-256 and payload metadata before atomic install, migrate existing subject packs without deleting them until all lessons are safely extracted, and let students remove individual cached lessons in Profile. Offline completion messages state that progress remains on-device and that account sync is not available yet.
- **Implemented in v0.4.1 (migration hardening):** validate the installed legacy subject pack and its `.bak` candidate before choosing a source; prefer the best valid version, using the backup if the primary is corrupt. Verify identity/version and, for the current catalog version, the whole-pack SHA-256 before extracting any unit. Truncated, modified, and incomplete current-version packs are left intact. Source files are removed only after every catalog lesson is installed. Older versions without a historical catalog hash remain eligible for structural validation and migration.
- **Implemented in v0.4.2 (reliability and UI polish):** prevent automatic download loops when WorkManager reports success but no verified lesson file is available; let the learner explicitly retry. Persist offline lesson-completion provenance locally and clear it with progress. Standardize cached/uncached labels, expose all search matches beyond the initial six, make each due-review card have one action, expose quiz option/feedback states to accessibility services, remove repeated numbers from Profile metric labels, surface failed lesson deletion, theme the text-size slider and retry action, and clarify the reset dialog’s effect on Continue Learning.
- **Verification still required:** run Android CI and instrumentation tests; manually check airplane-mode behavior, worker retry and recovery, legacy-pack migration, prefetch, storage warnings, and deletion on target devices and intermittent connections.
- Test large text, contrast, screen readers, small screens, low storage, slow or intermittent data, and airplane-mode behavior.

Reminders, extra quiz formats, expanded glossary/search, official report routing, and curriculum approval remain gated on student/educator preference, pilot observations, or decisions from the local content reviewer; do not infer those requirements from the software implementation alone.

Use pilot observations to choose what to build next. Avoid collecting identifiable student analytics by default; if evaluation data is needed, agree consent, purpose, retention, and access first.

**Pilot exit gate:** educators approve the content, students can complete the core loop reliably on target devices, and the main reported usability or correctness problems are resolved.

## Phase 3 — stable v1 release

Release only after the content scope and support plan are credible. The exact number of forms and subjects should follow pilot capacity; do not promise all Forms 1–4 until the material is reviewed and ready.

- Expand verified lesson and practice coverage for the forms and subjects Noor commits to support.
- Add exam practice mode, including timed sets and useful answer explanations, only for content Noor has rights to use. Past-paper materials require permission or a clearly documented legal source.
- Add topic-level recommendations such as “review this topic next,” based on a transparent rule the app can explain.
- Offer additional content languages, audio, or richer media where user research supports them and the content can be maintained offline; keep downloads optional and size-visible.
- Publish a privacy notice and support/contact route; document how on-device data is stored, cleared, and backed up.
- Add optional backup or account-based sync only if students need recovery or multi-device use. Keep core learning available without signing in and design for sync conflicts/offline edits.
- Use signed release artifacts, versioned GitHub releases, release notes, staged testing, and a way to replace a faulty release.

**v1 exit gate:** the promised scope is curriculum-reviewed, release builds are signed and reproducible, app updates work, and support/feedback responsibilities are clear.

## Phase 4 — grow with evidence

Add these after the student experience and content operations are stable:

- More forms, subjects, languages, and curriculum packs.
- A teacher/content-editor workflow for drafting, reviewing, correcting, and publishing versioned material.
- Optional teacher-created study plans or assignments, with student consent and clear school controls.
- Optional teacher views of class-level progress, designed to avoid unnecessary exposure of individual student data.
- Multi-device restore/sync and managed content updates if the pilot shows a real need.
- Carefully evaluated adaptive practice or AI-supported help only after content quality, answer checking, privacy, moderation, and low-connectivity behavior can be assured.

Social features, open chat, public leaderboards, live video, and ad-based monetization should remain out of scope unless there is a strong, reviewed case and the safeguarding and operational capacity to support them.

## Implementation order and dependencies

1. Confirm pilot scope and curriculum review process.
2. Replace sample screens with a real local content model and one end-to-end lesson/quiz/review flow.
3. Make the learning pack and progress persistence work offline.
4. Validate with students/educators and low-cost devices; fix observed issues.
5. Expand reviewed content and pilot operations.
6. Add release signing and tagged app versioning before wider distribution.
7. Add accounts, teacher tools, analytics, or AI only when a demonstrated need and privacy/safety plan justify them.

## GitHub and release workflow

Continue using feature branches, Pull Requests, Issues, Dependabot, and the Android CI workflow. The repository is public by the owner’s choice. `main` is protected: Pull Requests and a passing `android-verify-build` check are required, conversations must be resolved, administrators are included, and force pushes/deletions are blocked. CI validates lesson-pack structure and checksums, runs unit tests, builds a debug APK, and keeps the artifact for seven days. Repository workflows use the development-only signing key when both Actions secrets are available; forked builds fall back to the runner's default debug key. No production release-signing workflow is configured.

## Remaining decisions before a classroom pilot

1. Who will review the Biology and Physics packs, and what corrections are needed?
2. Which current official syllabus and exam documents define the Form 3 scope?
3. Is English sufficient for the first pilot, or should later packs include Chichewa?
4. Which lower-cost Android phones, screen sizes, and connectivity conditions should be used for manual testing?

Recommended next step: obtain the official curriculum documents and ask a Malawi Biology and Physics teacher to review the draft packs before expanding coverage or calling the app classroom-ready.

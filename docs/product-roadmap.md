# Noor product roadmap

## Product direction

Noor should help secondary-school students in Malawi learn and revise curriculum content on Android phones, including when mobile data is limited or unavailable. Build a dependable learning loop before adding broad catalogs, accounts, social features, or automation:

1. Choose a form, subject, and topic.
2. Learn from a short, clear lesson.
3. Check understanding with practice questions and explanations.
4. Review topics that need another try.
5. See progress and choose a useful next step.

The current app screens are a visual scaffold. The sample learner, subjects, progress figures, and biology question are illustrative, not verified curriculum content.

## Product principles

- **Offline-first and data-conscious:** reading, practice, and review should work without a live connection after content is on the phone. Keep downloads small and let students see and remove stored content.
- **Curriculum-led:** map lessons to the current Malawi secondary curriculum and any relevant examination objectives. Have a qualified local teacher review content and explanations before students rely on them.
- **Private by default:** let students use the core app without an account. Collect as little personal information as possible; no advertising, public profiles, direct messaging, or location collection in the MVP.
- **Accessible on everyday phones:** test on lower-cost Android devices, slow connections, small screens, and limited storage. Make text readable and controls easy to tap.
- **Encouraging, not pressuring:** use useful feedback and private progress, not public rankings or punitive streaks.

## Phase 0 — agree the pilot and foundations (now)

Decide the initial student group and content scope before writing a large library. Confirm the curriculum and exam references to use, content licensing, who will review lessons, preferred language(s), and what devices/connectivity the pilot should support. Start narrow: one form and one or two subjects, selected with students or local educators.

Prepare the content model and local storage so a lesson pack can be updated without requiring students to be online. Write a simple content-review checklist for correctness, clarity, language, and rights to any diagrams or past-paper questions. Decide what feedback can be collected without identifying a child.

### App versioning foundation

The scaffold currently declares `versionName = 0.1.0` and `versionCode = 1`. Keep `versionName` semantic and human-readable (`0.1.x` for early pilot builds; `1.0.0` for the first stable public release). Increase Android `versionCode` monotonically for every distributable build so a newer test build can update an older one. Tag release source with matching Git tags such as `v0.1.0` and `v1.0.0`.

The current Actions workflow is for CI and short-lived debug APK artifacts. Before distributing builds beyond the development team, add a release workflow that builds a signed APK/AAB from a version tag, stores signing credentials only in GitHub Secrets, creates release notes, and keeps debug and release builds clearly separate.

**Exit gate:** pilot form, subject(s), language, curriculum source, teacher reviewer, device expectations, and privacy approach are agreed.

## Phase 1 — MVP / closed alpha

Deliver one complete, useful learning loop for the agreed pilot scope rather than many empty subject pages.

- First-run setup: select form and preferred content language; skip account creation and collect no unnecessary personal details.
- Subject and topic library with clear curriculum labels and an obvious “continue learning” path.
- Short lesson pages using lightweight text, diagrams, and examples; show key terms and a clear completion action.
- Downloadable subject/topic packs with download progress, storage size, delete controls, and offline availability indicators.
- Multiple-choice practice with immediate correct/incorrect feedback and a short explanation. Keep question content local with the lesson pack.
- A local review queue for missed or due topics, with a simple repeat-practice flow.
- Private on-device progress: completed lessons, recent practice, and a simple topic-level summary. Explain that progress is stored on the phone.
- Basic settings: form, language/content preference, text-size choice, storage management, and help/feedback route.
- Automated checks for quiz logic, content parsing, and key offline flows; manual checks on at least one lower-cost Android device.

**Not in this phase:** sign-in, cloud sync, teacher dashboards, live classes, chat, public rankings, payments, or an AI tutor.

**Alpha exit gate:** a student can download the pilot pack once, use lessons and quizzes in airplane mode, close and reopen the app without losing local progress, and understand why an answer is correct. A teacher has reviewed the pilot content.

## Phase 2 — school or community pilot

Run a time-bounded pilot with a small, consenting group of students and educators. Use feedback to correct content and simplify confusing flows before expanding the library.

- Complete and teacher-review the agreed pilot form/subject coverage; label content by form, topic, and source/version.
- Add search and a lightweight glossary if students need them in observed use.
- Add more question formats only where useful and practical on phones (for example, numeric answers for mathematics after multiple-choice is reliable).
- Add opt-in, local study reminders only if students and educators want them; reminders must be easy to turn off.
- Provide a clear “report a content problem” route that does not expose student details publicly.
- Improve pack updates, download recovery, storage warnings, and safe handling of an interrupted connection.
- Test large text, contrast, screen readers, small screens, low storage, slow or intermittent data, and airplane-mode behavior.

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

Continue using feature branches, Pull Requests, Issues, Dependabot, and the Android CI workflow. CI currently builds a debug APK and keeps its artifact for seven days. GitHub rejected branch protection for this private repository with a message that GitHub Pro or a public repository is required; keep the repository private and review the passing CI result manually unless the plan changes. Do not make the repository public just to enable a setting.

## Decisions needed before the first real content pack

1. Which form should the pilot support first: Form 1, 2, 3, or 4?
2. Which subject should lead, and what is the first topic students struggle with?
3. Should the MVP be English-only, or should Chichewa be included from the first content pack?
4. Is Noor primarily for day-to-day lesson support, exam preparation, or both?

Recommended default until these are decided: account-free, English-first, offline-capable MVP; select one form and one or two subjects with a local teacher and students before creating substantial content.

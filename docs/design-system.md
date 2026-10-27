# Noor design system

The visual direction follows the supplied ten-screen reference while preserving Noor's offline-first behavior, readable lesson typography, and restrained green-and-cream palette. Botanical illustration is used sparingly as a brand accent, not as background decoration.

## Color palette

- **Forest green** `#2E6B4E`: primary actions, selected states, progress, and key emphasis.
- **Deep moss** `#1F4D37`: high-contrast green surfaces and pressed/emphasized states.
- **Mint** `#DCEBDD`: selected rows, supportive information panels, and soft secondary surfaces.
- **Warm cream** `#F6F3EA`: primary app canvas.
- **White** `#FFFFFF`: cards and reading surfaces.
- **Ink** `#15181E`: main text and high-contrast icons.

Use ink on light surfaces and white or warm cream on dark green surfaces. Keep status meaning clear without relying on color alone. Do not add colors only for decoration; error feedback may retain a distinct error color when needed for clarity.

## Typography and spacing

Use the platform serif face for the Noor wordmark and display/page/section headings to match the reference's editorial character. Use the bundled **Atkinson Hyperlegible Next** typeface for body text, controls, labels, metadata, and educational reading. The app bundles static Regular, Medium, SemiBold, and Bold TTF instances so body text works on the minimum supported Android version without variable-font support or a network connection. Keep lesson paragraphs at a comfortable 24sp line height, body weight Regular, section headings SemiBold, and page titles Bold. Preserve Android text scaling and readable wrapping.

The Atkinson font is sourced from [Google Fonts’ Atkinson Hyperlegible Next project](https://github.com/googlefonts/atkinson-hyperlegible-next) and distributed under the SIL Open Font License 1.1. The full licence is included at `licenses/OFL-Atkinson-Hyperlegible-Next.txt`.

Build spacing from a consistent 4/8-point scale. Keep cards compact, use rounded corners and subtle borders, and preserve touch targets of at least 48dp. Avoid repeated explanatory paragraphs; retain only lesson explanations, draft/review status, privacy and data-loss notices, and answer feedback.

## Shared navigation

Keep the four-tab navigation: **Today**, **Learn**, **Review**, and **Progress**. It remains fixed above Android's bottom gesture inset, with 24dp icons, 12sp Medium labels, Deep Moss for unselected content, Forest for selected content, and a Mint selected indicator. Use Android safe-drawing insets and scrollable content on long screens. Back navigation must return to the previous route rather than exit the app.

## Screen direction

### Onboarding

Use a lower-case serif “noor” wordmark, the headline “Small steps. Big knowledge.”, one student-at-study illustration, concise Form 3 / Malawi copy, a brief draft-content disclosure, and a prominent pill-shaped “Get started” action. Do not put essential text inside the illustration.

### Today

Keep Continue Learning as the primary action in a Deep Moss card. A local search field searches saved lessons and topics only. “Today's path” is assembled from available saved lesson packs and displays actual completion state; do not invent streaks, points, elapsed-time claims, or progress. Provide a compact route to Learn rather than repeating large subject cards on this screen.

### Learn

Show Form 3 Biology and Physics subject cards, with topic counts and offline status derived from the actual local pack state. The offline banner must distinguish saved packs from packs that still need downloading. Keep draft/teacher-review status visible while the lesson packs remain drafts.

### Lesson reading and completion

Show subject/topic, lesson position, reading time, form, and the pack's actual curriculum status. Use readable paragraph spacing and relevant key-term cards. Add educational diagrams only when their labels and facts are supported by the lesson content; label schematic diagrams as not to scale. Keep “Lesson completed” visually quiet and “Practise this topic” as the prominent action.

### Review and progress

Review presents the private practice queue, with due topics separated from other saved topics. Progress stays local and clearly separates lesson completion from quiz accuracy. Accuracy is unavailable until questions have been answered; zero-value progress bars must have no partial fill.

### Quick Check

Use a focused question layout with visible position/progress, large answer targets, clear selected/correct states, explanatory feedback, and a prominent next action.

### Profile and settings

Show the learner's selected form and actual local learning stats without asking for an account or inventing a student identity. Preserve controls for saved packs, text scaling, privacy, app version, and deliberate progress deletion.

## Data and content safeguards

Never hardcode progress, download state, topic/session counts, or quiz accuracy. Use singular/plural labels dynamically. All current lesson packs are draft content awaiting teacher review; disclose that status wherever students open the content. Lesson and topic search works only on locally saved content so it remains useful offline.

# Noor design system

This is the starting visual system based on the selected modern screen concepts. It intentionally avoids the earlier botanical direction and keeps the palette restrained.

## Color palette

- **Forest green** `#2E6B4E`: primary actions, selected states, progress, and key emphasis.
- **Deep moss** `#1F4D37`: high-contrast green surfaces and pressed/emphasized states.
- **Mint** `#DCEBDD`: selected rows, supportive information panels, and soft secondary surfaces.
- **Warm cream** `#F6F3EA`: primary app canvas.
- **Ink** `#15181E`: main text and high-contrast icons.

Use ink on light surfaces and white or warm cream on dark green surfaces. Keep status meaning clear without relying on color alone. Avoid introducing additional accent colors.

## Typography and spacing

Use the bundled **Atkinson Hyperlegible Next** typeface through the shared Compose `NoorFontFamily` token. The app bundles static Regular, Medium, SemiBold, and Bold TTF instances so the same faces work on the minimum supported Android version without variable-font support or a network connection. `NoorTypography` uses this family across every text token, mapping body text to Regular, supporting labels to Medium, section headings to SemiBold, and page titles to Bold. Keep lesson paragraphs at a comfortable 24sp line height and preserve Android text scaling. Keep titles short, use sentence case, and preserve readable wrapping. Use concise interface labels instead of repeated helper paragraphs, but retain lesson explanations, answer feedback, and essential data-loss or curriculum-status notices. Build spacing from a consistent 4/8-point scale. Controls retain generous touch areas and clear selected, disabled, and pressed states.

The font is sourced from [Google Fonts’ Atkinson Hyperlegible Next project](https://github.com/googlefonts/atkinson-hyperlegible-next) and distributed under the SIL Open Font License 1.1. The full licence is included at `licenses/OFL-Atkinson-Hyperlegible-Next.txt`.

## Component direction

Prefer modular cards and compact subject rows with simple geometric line icons. Use rounded corners consistently, subtle borders, and minimal shadow. Keep navigation labels explicit: Today, Learn, Review, and Progress. Focused quiz screens should minimize navigation and make answer choices easy to tap.

## Today home screen

- Use 18dp horizontal page margins and 14dp top/bottom padding on the warm-cream canvas. Keep the content scrollable above a fixed white bottom bar.
- Header: “Noor” at 28sp Bold in Ink, with “Form 3 · Biology & Physics” beneath at 14sp Medium in Deep Moss. Place a 24dp Forest settings icon in a 48dp touch target on the right.
- Keep Continue Learning as the primary action. Place it 16dp below the header in a full-width Deep Moss card with 24dp corners and 18dp padding. Use a 12sp Mint overline, 14sp Regular subject/topic line, 20sp Semibold lesson title, and a 52dp-high Forest button with 16dp corners.
- Place the 20sp Semibold “Form 3 subjects” section heading 20dp below the continue card. Subject cards use white surfaces, 1dp Mint borders, 20dp corners, 16dp padding, 48dp Forest icon tiles with 14dp corners, 18sp Semibold subject names, and 14sp Regular/Medium metadata and download status. Maintain 12dp vertical gaps.
- Keep the four-tab navigation 80dp high above Android’s bottom gesture inset. Use 24dp icons, 12sp Medium labels, Deep Moss for unselected content, Forest for selected content, and a Mint selected indicator.

## Screen references

- [Today dashboard](design/today.png)
- [Subject library](design/subjects.png)
- [Quiz question](design/quiz.png)
- [Answer feedback](design/feedback.png)
- [Progress overview](design/progress.png)

These are visual concepts. Names, sample topic counts, and progress figures are illustrative, not real student data or confirmation of curriculum alignment.

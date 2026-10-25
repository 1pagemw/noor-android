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

## Screen references

- [Today dashboard](design/today.png)
- [Subject library](design/subjects.png)
- [Quiz question](design/quiz.png)
- [Answer feedback](design/feedback.png)
- [Progress overview](design/progress.png)

These are visual concepts. Names, sample topic counts, and progress figures are illustrative, not real student data or confirmation of curriculum alignment.

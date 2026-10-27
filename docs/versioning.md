# Noor Android versioning

## Current app identity

- `versionName`: `0.3.0`
- `versionCode`: `3`
- First implemented pilot scope: Form 3 Biology and Physics
- Debug APKs from GitHub Actions are test builds, not signed public releases.

## Rules for future changes

1. Use semantic `versionName` values: `0.MINOR.PATCH` during the pilot. Use `1.0.0` only after the stable-release gates in the product roadmap have been met.
2. Increase Android `versionCode` for every distributable APK/AAB. Never reuse or decrease a code, even when the human-readable version changes.
3. Keep the version fields in `app/build.gradle.kts` together in the same reviewed Pull Request as a release.
4. Tag a source release with the matching version, for example `v0.1.0`; do not tag unreleased or failing code.
5. Before a public release, add a dedicated release workflow that verifies the tag matches `versionName`, builds a signed artifact, and publishes notes. Store signing credentials only as protected GitHub Secrets and restrict who can run the workflow.
6. Keep CI debug artifacts separate from release artifacts. Avoid calling an unsigned debug APK a production release.

## Changeset guidance

- **Patch** (for example, `0.1.1`): bug fixes or content corrections without changing the intended pilot scope.
- **Minor** (for example, `0.2.0`): backward-compatible pilot functionality or new reviewed learning content.
- **Major** (`1.0.0`): the first stable public release; after that, increment major only for breaking changes to app behavior, content format, or supported workflows.

Content packs have their own integer `contentVersion` and SHA-256 checksum. Increment a pack’s `contentVersion` whenever its downloaded content changes; update the app catalogue checksum and pack metadata in the same Pull Request. An app release is not required for text-only pack updates when the catalogue remains compatible.

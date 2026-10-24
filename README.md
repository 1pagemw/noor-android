# Noor Android

A native Android learning app for secondary school students in Malawi. The first screens will follow the visual concepts in [`docs/design/`](docs/design/) and the palette and component guidance in [`docs/design-system.md`](docs/design-system.md).

## Development environment

Open this repository in GitHub Codespaces. The dev container provides Java 17, Gradle tooling, Kotlin and GitHub Copilot editor support. Android SDK command-line tools can be installed from the Codespace terminal with:

```bash
bash .devcontainer/install-android-sdk.sh
```

The script asks you to review and accept Google Android SDK licenses interactively before it installs Android platform packages. Do not run it unless you agree to those license terms. The installed SDK is for building; this Codespace is not configured to run an Android emulator. We’ll use a physical Android phone to install and review debug APKs as the app takes shape.

## GitHub workflow

Use Issues to track features and bugs. Work on a feature branch, then open a Pull Request into `main` so changes can be reviewed. GitHub Actions build and release workflows will be added when the initial Android Gradle project is in place.

## Project status

This repository currently contains environment setup and visual design references. Product behavior, curriculum scope, supported Android versions, and app data requirements are still to be agreed before feature implementation.

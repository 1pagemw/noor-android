#!/usr/bin/env bash
set -euo pipefail

# Android SDK command-line tools package and checksum published by Android Developers.
TOOLS_ZIP="commandlinetools-linux-15859902_latest.zip"
TOOLS_URL="https://dl.google.com/android/repository/${TOOLS_ZIP}"
TOOLS_SHA256="4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583"
SDK_ROOT="${ANDROID_HOME:-${HOME}/Android/Sdk}"
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT

if ! command -v curl >/dev/null 2>&1 || ! command -v unzip >/dev/null 2>&1; then
  sudo apt-get update
  sudo apt-get install -y curl unzip
fi

mkdir -p "${SDK_ROOT}/cmdline-tools"
curl --fail --location --silent --show-error "${TOOLS_URL}" --output "${TEMP_DIR}/${TOOLS_ZIP}"
printf '%s  %s\n' "${TOOLS_SHA256}" "${TEMP_DIR}/${TOOLS_ZIP}" | sha256sum --check
unzip -q "${TEMP_DIR}/${TOOLS_ZIP}" -d "${TEMP_DIR}"
rm -rf "${SDK_ROOT}/cmdline-tools/latest"
mv "${TEMP_DIR}/cmdline-tools" "${SDK_ROOT}/cmdline-tools/latest"

SDKMANAGER="${SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager"
export ANDROID_HOME="${SDK_ROOT}"
export ANDROID_SDK_ROOT="${SDK_ROOT}"
export PATH="${SDK_ROOT}/cmdline-tools/latest/bin:${SDK_ROOT}/platform-tools:${PATH}"

cat <<'NOTICE'
The next command displays Android SDK license terms. Review them and accept only if you agree.
No licenses are accepted automatically by this script.
NOTICE
"${SDKMANAGER}" --sdk_root="${SDK_ROOT}" --licenses
"${SDKMANAGER}" --sdk_root="${SDK_ROOT}" \
  "platform-tools" \
  "platforms;android-36" \
  "build-tools;36.0.0"

PROFILE="${HOME}/.bashrc"
for line in \
  "export ANDROID_HOME=\"${SDK_ROOT}\"" \
  "export ANDROID_SDK_ROOT=\"${SDK_ROOT}\"" \
  "export PATH=\"\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH\""; do
  grep -Fqx "${line}" "${PROFILE}" 2>/dev/null || printf '%s\n' "${line}" >> "${PROFILE}"
done

echo "Android SDK packages installed under ${SDK_ROOT}. Open a new terminal to load the PATH updates."

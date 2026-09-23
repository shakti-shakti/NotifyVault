#!/usr/bin/env bash
#
# Reproducible NotifyVault release build.
# Run from any directory with:
#   bash scripts/build-release.sh
#
# The script reuses compatible tools already installed. If Java 17 or the
# Android SDK command-line tools are missing, it downloads them into a local
# user cache and never modifies the system package manager.

set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CACHE_ROOT="${NOTIFYVAULT_TOOLCHAIN_DIR:-${XDG_CACHE_HOME:-${HOME}/.cache}/notifyvault}"
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-${CACHE_ROOT}/android-sdk}}"
JDK_ROOT="${CACHE_ROOT}/jdk-17"
ANDROID_CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
JDK_URL="https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
ANDROID_API="android-36"
BUILD_TOOLS="36.0.0"

say() {
  printf '\n[NotifyVault] %s\n' "$*"
}

fail() {
  printf '\n[NotifyVault] ERROR: %s\n' "$*" >&2
  exit 1
}

download() {
  local url="$1"
  local destination="$2"
  mkdir -p "$(dirname "$destination")"

  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --retry 3 --retry-delay 2 --output "$destination" "$url"
  elif command -v wget >/dev/null 2>&1; then
    wget --tries=3 --output-document="$destination" "$url"
  elif command -v python3 >/dev/null 2>&1; then
    python3 - "$url" "$destination" <<'PY'
import shutil
import sys
import urllib.request

with urllib.request.urlopen(sys.argv[1]) as response, open(sys.argv[2], "wb") as output:
    shutil.copyfileobj(response, output)
PY
  else
    fail "curl, wget, or python3 is required to download missing toolchains."
  fi
}

extract_zip() {
  local archive="$1"
  local destination="$2"
  mkdir -p "$destination"

  if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "$archive" -d "$destination"
  elif command -v python3 >/dev/null 2>&1; then
    python3 - "$archive" "$destination" <<'PY'
import sys
import zipfile

with zipfile.ZipFile(sys.argv[1]) as archive:
    archive.extractall(sys.argv[2])
PY
  else
    fail "unzip or python3 is required to extract the Android SDK tools."
  fi
}

java_major_version() {
  local java_binary="$1"
  "$java_binary" -version 2>&1 |
    sed -nE 's/.*version "([0-9]+).*/\1/p' |
    head -n 1
}

find_compatible_java() {
  local candidate=""
  local binary=""
  local major=""

  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    candidate="${JAVA_HOME}"
  elif command -v java >/dev/null 2>&1; then
    binary="$(readlink -f "$(command -v java)")"
    candidate="$(cd "$(dirname "$binary")/.." && pwd)"
  fi

  if [[ -n "$candidate" && -x "${candidate}/bin/java" ]]; then
    major="$(java_major_version "${candidate}/bin/java")"
    if [[ "$major" =~ ^[0-9]+$ ]] && (( major >= 17 )); then
      printf '%s\n' "$candidate"
      return 0
    fi
  fi

  return 1
}

ensure_java() {
  local java_home=""
  local archive=""
  local temporary=""
  local extracted=""

  if java_home="$(find_compatible_java)"; then
    export JAVA_HOME="$java_home"
    export PATH="${JAVA_HOME}/bin:${PATH}"
    say "Using Java ${JAVA_HOME}"
    return
  fi

  if [[ ! -x "${JDK_ROOT}/bin/java" ]]; then
    say "Java 17 is missing; downloading a local Temurin JDK."
    mkdir -p "$CACHE_ROOT"
    archive="${CACHE_ROOT}/jdk-17.tar.gz"
    download "$JDK_URL" "$archive"
    temporary="$(mktemp -d)"
    trap 'rm -rf "$temporary"' RETURN
    tar -xzf "$archive" -C "$temporary"
    extracted="$(find "$temporary" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
    [[ -n "$extracted" ]] || fail "The downloaded JDK archive had no top-level directory."
    rm -rf "$JDK_ROOT"
    mkdir -p "$(dirname "$JDK_ROOT")"
    mv "$extracted" "$JDK_ROOT"
    rm -rf "$temporary"
    trap - RETURN
  fi

  [[ -x "${JDK_ROOT}/bin/java" ]] || fail "Unable to prepare Java 17 at ${JDK_ROOT}."
  export JAVA_HOME="$JDK_ROOT"
  export PATH="${JAVA_HOME}/bin:${PATH}"
  say "Using downloaded Java ${JAVA_HOME}"
}

ensure_android_sdk() {
  local sdkmanager="${SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager"
  local archive=""
  local temporary=""
  local extracted=""

  if [[ ! -x "$sdkmanager" ]]; then
    say "Android SDK command-line tools are missing; downloading them."
    archive="${CACHE_ROOT}/android-commandline-tools.zip"
    temporary="$(mktemp -d)"
    trap 'rm -rf "$temporary"' RETURN
    download "$ANDROID_CMDLINE_TOOLS_URL" "$archive"
    extract_zip "$archive" "$temporary"
    extracted="${temporary}/cmdline-tools"
    [[ -d "$extracted" ]] || fail "The Android command-line tools archive had an unexpected layout."
    rm -rf "${SDK_ROOT}/cmdline-tools/latest"
    mkdir -p "${SDK_ROOT}/cmdline-tools"
    mv "$extracted" "${SDK_ROOT}/cmdline-tools/latest"
    rm -rf "$temporary"
    trap - RETURN
  fi

  [[ -x "$sdkmanager" ]] || fail "Unable to prepare sdkmanager at ${sdkmanager}."
  export ANDROID_SDK_ROOT="$SDK_ROOT"
  export ANDROID_HOME="$SDK_ROOT"
  export PATH="${SDK_ROOT}/cmdline-tools/latest/bin:${SDK_ROOT}/platform-tools:${SDK_ROOT}/emulator:${PATH}"

  say "Installing/checking Android SDK ${ANDROID_API} and build-tools ${BUILD_TOOLS}."
  yes | "$sdkmanager" --sdk_root="$SDK_ROOT" --licenses >/dev/null || true
  "$sdkmanager" --sdk_root="$SDK_ROOT" \
    "platform-tools" \
    "platforms;${ANDROID_API}" \
    "build-tools;${BUILD_TOOLS}"
}

main() {
  local keystore="${KEYSTORE_PATH:-${ROOT}/my-release-key.jks}"
  local apk="${ROOT}/app/build/outputs/apk/release/app-release.apk"
  local apksigner="${SDK_ROOT}/build-tools/${BUILD_TOOLS}/apksigner"

  [[ -x "${ROOT}/gradlew" ]] || fail "Gradle wrapper not found at ${ROOT}/gradlew."
  if [[ "$keystore" != /* ]]; then
    keystore="${ROOT}/${keystore}"
  fi
  [[ -f "$keystore" ]] || fail "Release keystore not found. Set KEYSTORE_PATH or provide my-release-key.jks."
  export KEYSTORE_PATH="$keystore"

  ensure_java
  ensure_android_sdk
  printf 'sdk.dir=%s\n' "$ANDROID_SDK_ROOT" > "${ROOT}/local.properties"

  say "Running tests and assembling the signed release APK."
  cd "$ROOT"
  ./gradlew --no-daemon test assembleRelease --stacktrace

  [[ -f "$apk" ]] || fail "Gradle completed without producing ${apk}."
  [[ -x "$apksigner" ]] || fail "apksigner not found at ${apksigner}."

  say "Verifying APK signature."
  "$apksigner" verify --verbose "$apk" | sed -n '1,12p'
  git diff --check

  say "Success."
  printf 'APK: %s\n' "$apk"
  printf 'Copied APK: %s\n' "${ROOT}/output/NotifyVault-release.apk"
}

main "$@"
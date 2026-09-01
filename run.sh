#!/bin/bash
# Netra Sahayak - build, install and launch on an Android device or emulator.
# macOS / Linux. On Windows use Android Studio's Run button, or gradlew.bat.
#
#   ./run.sh            build + install + launch (starts an emulator if none is connected)
#   ./run.sh emulator   only start an emulator
set -e

PKG=com.sih.netrasahayak

# --- locate the Android SDK -------------------------------------------------
if [ -z "$ANDROID_HOME" ]; then
    for candidate in "$HOME/Library/Android/sdk" "$HOME/Android/Sdk" "$HOME/android-sdk"; do
        [ -d "$candidate" ] && ANDROID_HOME="$candidate" && break
    done
fi
if [ -z "$ANDROID_HOME" ] || [ ! -d "$ANDROID_HOME" ]; then
    echo "ERROR: Android SDK not found."
    echo "Install Android Studio, then set ANDROID_HOME, e.g.:"
    echo "  export ANDROID_HOME=\$HOME/Library/Android/sdk   # macOS"
    echo "  export ANDROID_HOME=\$HOME/Android/Sdk           # Linux"
    exit 1
fi
export ANDROID_HOME
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

# --- locate a JDK 17+ -------------------------------------------------------
if [ -z "$JAVA_HOME" ]; then
    if [ -x /usr/libexec/java_home ] && /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
        JAVA_HOME="$(/usr/libexec/java_home -v 17)"
    else
        for candidate in \
            /opt/homebrew/opt/openjdk@17 \
            /usr/lib/jvm/java-17-openjdk-amd64 \
            "/Applications/Android Studio.app/Contents/jbr/Contents/Home"; do
            [ -d "$candidate" ] && JAVA_HOME="$candidate" && break
        done
    fi
fi
[ -n "$JAVA_HOME" ] && export JAVA_HOME
if ! "${JAVA_HOME:+$JAVA_HOME/bin/}java" -version >/dev/null 2>&1; then
    echo "ERROR: no JDK 17 found. Install one (Android Studio bundles it) or set JAVA_HOME."
    exit 1
fi

# --- emulator ---------------------------------------------------------------
start_emulator() {
    if adb devices | grep -qE "(device|emulator-[0-9]+)\s+device"; then
        echo "==> A device is already connected."
        return
    fi
    AVD="$(emulator -list-avds 2>/dev/null | head -1)"
    if [ -z "$AVD" ]; then
        echo "ERROR: no device connected and no emulator (AVD) configured."
        echo "Create one in Android Studio: Tools > Device Manager > Create Device (API 24+)."
        exit 1
    fi
    echo "==> Starting emulator: $AVD"
    nohup emulator -avd "$AVD" -no-snapshot -no-boot-anim > /tmp/netra-emulator.log 2>&1 &
    adb wait-for-device
    echo "==> Waiting for Android to finish booting..."
    until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
        sleep 3
    done
    sleep 8
    echo "==> Emulator ready."
}

start_emulator
[ "$1" = "emulator" ] && exit 0

echo "==> Building and installing..."
./gradlew installDebug

echo "==> Launching Netra Sahayak..."
adb shell am start -n "$PKG/.MainActivity" > /dev/null
echo "==> Done."

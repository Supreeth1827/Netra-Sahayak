#!/bin/bash
# Copies a sample retinal image into the device's gallery so that
# "CHOOSE FROM GALLERY" has something to pick. macOS / Linux.
set -e

if [ -z "$ANDROID_HOME" ]; then
    for candidate in "$HOME/Library/Android/sdk" "$HOME/Android/Sdk" "$HOME/android-sdk"; do
        [ -d "$candidate" ] && ANDROID_HOME="$candidate" && break
    done
fi
export PATH="${ANDROID_HOME:+$ANDROID_HOME/platform-tools:}$PATH"

HERE="$(cd "$(dirname "$0")" && pwd)"
adb push "$HERE/sample_fundus.jpg" /sdcard/Pictures/sample_fundus.jpg
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
    -d file:///sdcard/Pictures/sample_fundus.jpg > /dev/null
echo "Sample image added to the gallery."

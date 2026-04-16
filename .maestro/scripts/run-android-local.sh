#!/usr/bin/env bash
set -e

# Default values
SUITE="smoke"
USE_EMULATOR=true
APP_ID="tech.arnav.twofac.app"
AVD_NAME=""
export APP_ID

# Parse arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --usb) USE_EMULATOR=false ;;
        --suite) SUITE="$2"; shift ;;
        --avd) AVD_NAME="$2"; shift ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
    shift
done

cd "$(dirname "$0")/../.." # go to project root

if [ "$USE_EMULATOR" = true ]; then
    echo "Starting/Selecting Emulator..."
    if [ -n "$AVD_NAME" ]; then
        eval "$(node .agents/skills/simulators-emulators/scripts/android-emulator-picker.mjs --boot --shell --avd "$AVD_NAME")"
    else
        eval "$(node .agents/skills/simulators-emulators/scripts/android-emulator-picker.mjs --boot --shell)"
    fi
    
    if [ -z "$ANDROID_SERIAL" ]; then
        echo "Failed to start or select an emulator."
        exit 1
    fi
    echo "Using Emulator: $ANDROID_SERIAL"
else
    echo "Looking for USB devices..."
    # Select first connected physical device (not emulator)
    DEVICE_SERIAL=$(adb devices -l | grep -v "emulator" | grep " device " | head -n 1 | awk '{print $1}')
    if [ -z "$DEVICE_SERIAL" ]; then
        echo "No USB device found."
        exit 1
    fi
    export ANDROID_SERIAL="$DEVICE_SERIAL"
    echo "Using USB Device: $ANDROID_SERIAL"
fi

echo "Building and installing Android app..."
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew :androidApp:installDebug --quiet

echo "Running Maestro tests ($SUITE suite)..."
maestro --device "$ANDROID_SERIAL" test -e APP_ID="$APP_ID" --include-tags="$SUITE" --exclude-tags="platform-ios" .maestro/


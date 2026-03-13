#!/usr/bin/env bash
set -e

# Default values
SUITE="smoke"
USE_SIMULATOR=true
APP_ID="tech.arnav.twofac.app"
export APP_ID

# Parse arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --usb) USE_SIMULATOR=false ;;
        --suite) SUITE="$2"; shift ;;
        --udid) EXPLICIT_UDID="$2"; shift ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
    shift
done

cd "$(dirname "$0")/../.." # go to project root

if [ "$USE_SIMULATOR" = true ]; then
    echo "Starting/Selecting Simulator..."
    if [ -n "$EXPLICIT_UDID" ]; then
        eval "$(node .agents/skills/simulators-emulators/scripts/ios-simulator-picker.mjs --boot --shell --udid "$EXPLICIT_UDID")"
    else
        eval "$(node .agents/skills/simulators-emulators/scripts/ios-simulator-picker.mjs --boot --shell)"
    fi
    
    if [ -z "$IOS_SIMULATOR_UDID" ]; then
        echo "Failed to start or select a simulator."
        exit 1
    fi
    TARGET_UDID="$IOS_SIMULATOR_UDID"
    TARGET_DESTINATION="$IOS_SIMULATOR_DESTINATION"
    echo "Using Simulator: $TARGET_UDID"
else
    echo "Looking for USB devices..."
    # If using USB, we rely on xcodebuild and explicit UDID or discover
    if [ -n "$EXPLICIT_UDID" ]; then
        TARGET_UDID="$EXPLICIT_UDID"
    else
        TARGET_UDID=$(xcrun xctrace list devices 2>&1 | grep -v Simulator | grep -E -o '[0-9A-F]{8}-[0-9A-F]{16}|[0-9A-F]{24}' | head -n 1)
    fi

    if [ -z "$TARGET_UDID" ]; then
        echo "No USB device found."
        exit 1
    fi
    TARGET_DESTINATION="id=$TARGET_UDID"
    echo "Using USB Device: $TARGET_UDID"
fi

echo "Building iOS app..."
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination "$TARGET_DESTINATION" -derivedDataPath build/ios_build build -quiet

if [ "$USE_SIMULATOR" = true ]; then
    echo "Installing app on Simulator..."
    xcrun simctl install "$TARGET_UDID" build/ios_build/Build/Products/Debug-iphonesimulator/TwoFac.app
else
    echo "Installing app on USB device via devicectl/ios-deploy (requires manual setup or let Maestro handle if already installed)..."
    # Actually, Maestro requires the app to be installed, but for USB we might rely on the user having run the app via Xcode once,
    # or we can use ideviceinstaller if available. Since it's beyond standard tools, we just assume it's built or use xcodebuild test.
    # We will try to rely on maestro to run it if installed, or just prompt.
fi

echo "Running Maestro tests ($SUITE suite)..."
maestro --device "$TARGET_UDID" test -e APP_ID="$APP_ID" --include-tags="$SUITE" --exclude-tags="platform-android" .maestro/

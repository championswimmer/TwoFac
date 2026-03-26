#!/bin/bash
set -e
cd "$(dirname "$0")"

echo "Building TwoFacKeychain dylib..."

# Build for arm64
echo "Building for arm64..."
swiftc -emit-library -target arm64-apple-macos11.0 \
    -Osize -o libtwofac_keychain_arm64.dylib TwoFacKeychain.swift

# Build for x86_64
echo "Building for x86_64..."
swiftc -emit-library -target x86_64-apple-macos11.0 \
    -Osize -o libtwofac_keychain_x86_64.dylib TwoFacKeychain.swift

# Create universal binary
echo "Creating universal binary..."
lipo -create \
    libtwofac_keychain_arm64.dylib \
    libtwofac_keychain_x86_64.dylib \
    -output libtwofac_keychain.dylib

# Clean up intermediate files
rm -f libtwofac_keychain_arm64.dylib libtwofac_keychain_x86_64.dylib

echo "Built libtwofac_keychain.dylib (universal binary)"
file libtwofac_keychain.dylib

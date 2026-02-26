#!/usr/bin/env bash
set -euo pipefail

# This script sets up the environment for GitHub Copilot
# It pre-installs Java, Gradle, and Node.js before Copilot starts working
# This ensures Copilot can run Gradle tasks for testing without waiting for dependencies

echo "==> Setting up Java Development Kit (JDK) 21..."
# Install JDK 21 (Temurin distribution) matching the project's GitHub Actions
if ! command -v java &> /dev/null || ! java -version 2>&1 | grep -q "version \"21"; then
    # Download and install Temurin JDK 21
    wget -q -O /tmp/temurin-jdk-21.tar.gz \
        "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.6%2B7/OpenJDK21U-jdk_x64_linux_hotspot_21.0.6_7.tar.gz"

    mkdir -p /opt/java
    tar -xzf /tmp/temurin-jdk-21.tar.gz -C /opt/java
    rm /tmp/temurin-jdk-21.tar.gz

    # Set JAVA_HOME and update PATH
    export JAVA_HOME=/opt/java/jdk-21.0.6+7
    export PATH=$JAVA_HOME/bin:$PATH

    echo "export JAVA_HOME=/opt/java/jdk-21.0.6+7" >> ~/.bashrc
    echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> ~/.bashrc
fi

echo "==> Java version:"
java -version

echo "==> Setting up Node.js 22..."
# Install Node.js 22 matching the project's GitHub Actions
if ! command -v node &> /dev/null || ! node --version | grep -q "v22"; then
    curl -fsSL https://deb.nodesource.com/setup_22.x | bash -
    apt-get install -y nodejs
fi

echo "==> Node.js version:"
node --version
npm --version

echo "==> Setting up Gradle..."
# Gradle will be installed via the gradlew wrapper, but we need to ensure it's cached
# The project uses Gradle 8.14.4 as specified in gradle-wrapper.properties

# Pre-download Gradle distribution to speed up first run
GRADLE_VERSION="8.14.4"
GRADLE_DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
GRADLE_DIST_DIR="$GRADLE_USER_HOME/wrapper/dists/gradle-${GRADLE_VERSION}-bin"

mkdir -p "$GRADLE_DIST_DIR"

if [ ! -f "$GRADLE_DIST_DIR/gradle-${GRADLE_VERSION}-bin.zip" ]; then
    echo "==> Downloading Gradle ${GRADLE_VERSION}..."
    wget -q -O "$GRADLE_DIST_DIR/gradle-${GRADLE_VERSION}-bin.zip" "$GRADLE_DIST_URL"
fi

# Also cache Gradle dependencies by running a simple Gradle command
echo "==> Warming up Gradle cache..."
if [ -f "./gradlew" ]; then
    ./gradlew --version || true
    # Pre-download common dependencies
    ./gradlew dependencies --configuration runtimeClasspath --quiet || true
fi

echo "==> Setup complete!"
echo "Java, Node.js, and Gradle are now available for GitHub Copilot to use."
echo "Gradle dependencies have been cached for faster builds."

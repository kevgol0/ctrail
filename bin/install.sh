#!/bin/bash
set -e

# resolve version from argument or pom.xml
VERSION="${1:-}"
if [ -z "$VERSION" ]; then
    SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
    POM="$SCRIPT_DIR/../pom.xml"
    if [ -f "$POM" ]; then
        VERSION=$(grep '<version>' "$POM" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
    fi
fi

if [ -z "$VERSION" ]; then
    echo "Usage: install.sh [VERSION]"
    echo "  e.g. install.sh 1.1.0"
    exit 1
fi

REPO="https://github.com/kevgol0/ctrail/releases/download"
LIB_DIR="/usr/local/share"
JAR="ctrail-${VERSION}.jar"

# download and install the jar
echo "Installing ctrail v${VERSION}..."
curl -fSL "${REPO}/v${VERSION}/${JAR}" -o "/tmp/${JAR}"
sudo mv "/tmp/${JAR}" "${LIB_DIR}/"

if [ -e "${LIB_DIR}/ctrail.jar" ]; then
    sudo rm "${LIB_DIR}/ctrail.jar"
fi
sudo ln -s "${LIB_DIR}/${JAR}" "${LIB_DIR}/ctrail.jar"

# download and install the default config (skip if one already exists)
CONFIG="ctrail.xml"
curl -fSL "${REPO}/v${VERSION}/${CONFIG}" -o "/tmp/${CONFIG}"
if [ ! -e "/etc/${CONFIG}" ]; then
    sudo mv "/tmp/${CONFIG}" "/etc/${CONFIG}"
else
    echo "NOT replacing pre-existing file: /etc/${CONFIG}"
    rm "/tmp/${CONFIG}"
fi

# download and install the launcher script
curl -fSL "${REPO}/v${VERSION}/ctr" -o "/tmp/ctr"
sudo mv "/tmp/ctr" /usr/local/bin/ctr
sudo chmod +x /usr/local/bin/ctr

echo "ctrail v${VERSION} installed successfully."

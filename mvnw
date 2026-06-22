#!/bin/sh
set -e
MAVEN_HOME="${HOME}/.m2/wrapper/dists"
MAVEN_VERSION="3.9.6"
MAVEN_DIST_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.zip"
MAVEN_INSTALL_DIR="${MAVEN_HOME}/apache-maven-${MAVEN_VERSION}"

if [ ! -d "${MAVEN_INSTALL_DIR}" ]; then
  echo "Downloading Apache Maven ${MAVEN_VERSION}..."
  mkdir -p "${MAVEN_HOME}"
  TMP_ZIP="${MAVEN_HOME}/apache-maven-${MAVEN_VERSION}-bin.zip"
  curl -fsSL "${MAVEN_DIST_URL}" -o "${TMP_ZIP}"
  unzip -q "${TMP_ZIP}" -d "${MAVEN_HOME}"
  rm "${TMP_ZIP}"
  echo "Maven ${MAVEN_VERSION} installed."
fi

exec "${MAVEN_INSTALL_DIR}/bin/mvn" "$@"

#!/usr/bin/env bash
set -euo pipefail

REFERENCE_VERSION="${REFERENCE_VERSION:-}"

if [[ -z "${REFERENCE_VERSION}" ]]; then
  echo "REFERENCE_VERSION is required (example: 1.2.3)"
  exit 2
fi

CURRENT_VERSION="$(./mvnw -q -DforceStdout help:evaluate -Dexpression=project.version)"
CURRENT_MAJOR="${CURRENT_VERSION%%.*}"
REFERENCE_MAJOR="${REFERENCE_VERSION%%.*}"

if [[ "${CURRENT_MAJOR}" != "${REFERENCE_MAJOR}" ]]; then
  echo "Major version changed (${REFERENCE_MAJOR} -> ${CURRENT_MAJOR}), binary compatibility gate is skipped."
  exit 0
fi

echo "Running binary compatibility gate against ${REFERENCE_VERSION} (major=${CURRENT_MAJOR})"
./mvnw -pl platform-test -am \
  -Dcompatibility.skip=false \
  -Dcompatibility.referenceVersion="${REFERENCE_VERSION}" \
  verify

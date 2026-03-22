#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
POM_PATH="${ROOT_DIR}/platform-bom/pom.xml"

fail() {
  echo "[verify-platform-bom-structure] ERROR: $1" >&2
  exit 1
}

require_xpath() {
  local xpath="$1"
  local message="$2"
  if ! xmllint --xpath "$xpath" "${POM_PATH}" >/dev/null 2>&1; then
    fail "$message"
  fi
}

forbid_xpath() {
  local xpath="$1"
  local message="$2"
  if xmllint --xpath "$xpath" "${POM_PATH}" >/dev/null 2>&1; then
    fail "$message"
  fi
}

if [[ ! -f "${POM_PATH}" ]]; then
  fail "Missing required file: ${POM_PATH}"
fi

if ! command -v xmllint >/dev/null 2>&1; then
  fail "Missing required tool: xmllint"
fi

require_xpath "/*[local-name()='project']/*[local-name()='dependencyManagement']/*[local-name()='dependencies']" "Missing required <dependencyManagement>/<dependencies> section"
require_xpath "/*[local-name()='project']/*[local-name()='dependencyManagement']/*[local-name()='dependencies']/*[local-name()='dependency'][*[local-name()='artifactId' and text()='spring-boot-dependencies'] and *[local-name()='type' and text()='pom'] and *[local-name()='scope' and text()='import']]" "Missing required spring-boot-dependencies BOM import"

forbid_xpath "/*[local-name()='project']/*[local-name()='parent']" "Forbidden section found: <parent>"
forbid_xpath "/*[local-name()='project']/*[local-name()='properties']" "Forbidden section found: <properties>"
forbid_xpath "/*[local-name()='project']/*[local-name()='dependencies']" "Forbidden section found: <dependencies>"
forbid_xpath "/*[local-name()='project']/*[local-name()='build']" "Forbidden section found: <build>"
forbid_xpath "/*[local-name()='project']/*[local-name()='modules']" "Forbidden section found: <modules>"
forbid_xpath "/*[local-name()='project']/*[local-name()='profiles']" "Forbidden section found: <profiles>"
forbid_xpath "/*[local-name()='project']/*[local-name()='distributionManagement']" "Forbidden section found: <distributionManagement>"
forbid_xpath "/*[local-name()='project']/*[local-name()='repositories']" "Forbidden section found: <repositories>"
forbid_xpath "/*[local-name()='project']/*[local-name()='pluginRepositories']" "Forbidden section found: <pluginRepositories>"

managed_dependency_count="$(xmllint --xpath "count(/*[local-name()='project']/*[local-name()='dependencyManagement']/*[local-name()='dependencies']/*[local-name()='dependency'])" "${POM_PATH}" 2>/dev/null || true)"
if [[ "${managed_dependency_count}" == "0" ]]; then
  fail "<dependencyManagement> must contain at least one managed dependency"
fi

echo "[verify-platform-bom-structure] OK: platform-bom/pom.xml follows dependencyManagement-only constraints"

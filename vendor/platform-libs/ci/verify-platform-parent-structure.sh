#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
POM_PATH="${ROOT_DIR}/platform-parent/pom.xml"

fail() {
  echo "[verify-platform-parent-structure] ERROR: $1" >&2
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

require_xpath "/*[local-name()='project']/*[local-name()='properties']" "Missing required <properties> section"
require_xpath "/*[local-name()='project']/*[local-name()='build']/*[local-name()='pluginManagement']" "Missing required <pluginManagement> section"
require_xpath "/*[local-name()='project']/*[local-name()='build']/*[local-name()='plugins']/*[local-name()='plugin'][*[local-name()='artifactId' and text()='maven-enforcer-plugin']]" "Missing required maven-enforcer-plugin in build/plugins"
require_xpath "/*[local-name()='project']/*[local-name()='build']/*[local-name()='plugins']/*[local-name()='plugin'][*[local-name()='artifactId' and text()='maven-enforcer-plugin']]/*[local-name()='executions']/*[local-name()='execution'][*[local-name()='phase' and text()='validate']]" "maven-enforcer-plugin must be bound to validate phase"
require_xpath "/*[local-name()='project']/*[local-name()='build']/*[local-name()='plugins']/*[local-name()='plugin'][*[local-name()='artifactId' and text()='maven-enforcer-plugin']]/*[local-name()='executions']/*[local-name()='execution']/*[local-name()='goals']/*[local-name()='goal' and text()='enforce']" "maven-enforcer-plugin must execute enforce goal"

forbid_xpath "/*[local-name()='project']/*[local-name()='dependencies']" "Forbidden section found: <dependencies>"
forbid_xpath "/*[local-name()='project']/*[local-name()='dependencyManagement']" "Forbidden section found: <dependencyManagement>"
forbid_xpath "/*[local-name()='project']/*[local-name()='modules']" "Forbidden section found: <modules>"
forbid_xpath "/*[local-name()='project']/*[local-name()='profiles']" "Forbidden section found: <profiles>"
forbid_xpath "/*[local-name()='project']/*[local-name()='distributionManagement']" "Forbidden section found: <distributionManagement>"

build_plugins_xpath="/*[local-name()='project']/*[local-name()='build']/*[local-name()='plugins']/*[local-name()='plugin']"
plugin_count="$(xmllint --xpath "count(${build_plugins_xpath})" "${POM_PATH}" 2>/dev/null || true)"
if [[ "${plugin_count}" != "1" ]]; then
  fail "build/plugins must contain exactly one plugin (maven-enforcer-plugin), got ${plugin_count}"
fi

enforcer_count="$(xmllint --xpath "count(${build_plugins_xpath}[*[local-name()='artifactId' and text()='maven-enforcer-plugin']])" "${POM_PATH}" 2>/dev/null || true)"
if [[ "${enforcer_count}" != "1" ]]; then
  fail "build/plugins must contain only maven-enforcer-plugin"
fi

echo "[verify-platform-parent-structure] OK: platform-parent/pom.xml follows policy-only constraints"

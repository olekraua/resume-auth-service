#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT_POM="${ROOT_DIR}/pom.xml"
MODULE_POM="${ROOT_DIR}/microservices/backend/services/auth-service/pom.xml"

fail() {
	echo "[verify-parent-model] ERROR: $1" >&2
	exit 1
}

require_xpath() {
	local pom_path="$1"
	local xpath="$2"
	local message="$3"
	if ! xmllint --xpath "${xpath}" "${pom_path}" >/dev/null 2>&1; then
		fail "${message}"
	fi
}

forbid_xpath() {
	local pom_path="$1"
	local xpath="$2"
	local message="$3"
	if xmllint --xpath "${xpath}" "${pom_path}" >/dev/null 2>&1; then
		fail "${message}"
	fi
}

if [[ ! -f "${ROOT_POM}" ]]; then
	fail "Missing required file: ${ROOT_POM}"
fi

if [[ ! -f "${MODULE_POM}" ]]; then
	fail "Missing required file: ${MODULE_POM}"
fi

if ! command -v xmllint >/dev/null 2>&1; then
	fail "Missing required tool: xmllint"
fi

require_xpath \
	"${ROOT_POM}" \
	"/*[local-name()='project']/*[local-name()='parent'][*[local-name()='groupId' and text()='net.devstudy'] and *[local-name()='artifactId' and text()='platform-parent']]" \
	"Root parent must be net.devstudy:platform-parent"

require_xpath \
	"${ROOT_POM}" \
	"/*[local-name()='project']/*[local-name()='dependencyManagement']/*[local-name()='dependencies']/*[local-name()='dependency'][*[local-name()='groupId' and text()='net.devstudy'] and *[local-name()='artifactId' and text()='platform-bom'] and *[local-name()='type' and text()='pom'] and *[local-name()='scope' and text()='import']]" \
	"Root pom must import net.devstudy:platform-bom in dependencyManagement"

forbid_xpath \
	"${ROOT_POM}" \
	"/*[local-name()='project']/*[local-name()='dependencies']/*[local-name()='dependency']" \
	"Root pom must not declare runtime/compile dependencies. Keep root policy-only."

forbid_xpath \
	"${ROOT_POM}" \
	"/*[local-name()='project']/*[local-name()='repositories']" \
	"Root pom must not declare repositories. Use CI/user settings.xml instead."

forbid_xpath \
	"${ROOT_POM}" \
	"/*[local-name()='project']/*[local-name()='pluginRepositories']" \
	"Root pom must not declare pluginRepositories. Use CI/user settings.xml instead."

require_xpath \
	"${ROOT_POM}" \
	"/*[local-name()='project']/*[local-name()='build']/*[local-name()='plugins']/*[local-name()='plugin'][*[local-name()='artifactId' and text()='maven-enforcer-plugin']]/*[local-name()='executions']/*[local-name()='execution'][*[local-name()='phase' and text()='validate'] and *[local-name()='goals']/*[local-name()='goal' and text()='enforce']]" \
	"Root pom must execute maven-enforcer-plugin in validate phase"

require_xpath \
	"${MODULE_POM}" \
	"/*[local-name()='project']/*[local-name()='parent'][*[local-name()='groupId' and text()='net.devstudy'] and *[local-name()='artifactId' and text()='resume']]" \
	"Service module must inherit from root parent pom"

forbid_xpath \
	"${MODULE_POM}" \
	"/*[local-name()='project']/*[local-name()='dependencies']/*[local-name()='dependency'][not(starts-with(*[local-name()='groupId']/text(),'net.devstudy')) and *[local-name()='version']]" \
	"External dependency versions in module are forbidden. Manage them via platform-bom."

echo "[verify-parent-model] OK: root is policy-only and service uses platform parent/BOM model"

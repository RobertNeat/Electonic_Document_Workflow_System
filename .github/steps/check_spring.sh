#!/usr/bin/env bash
set -euo pipefail

: "${PROJECT_PATH:?PROJECT_PATH is required}"
: "${RUNTIME_VERSION:?RUNTIME_VERSION is required}"
: "${BUILD_TOOL:=maven}"
exceptions="${TRIVY_EXCEPTIONS:-[]}"

command -v mise >/dev/null
command -v trivy >/dev/null
command -v semgrep >/dev/null

# Testcontainers domyślnie przerywa pull obrazu po 30s bez postępu (pull.pause.timeout) - na tym
# runnerze rejestr bywa wolny, ale nie zablokowany (docker image pull has not made progress in 30s,
# po kilku ponowieniach i tak się kończy) - wydłużone, żeby test się nie wywalał na samym pull'u.
export TESTCONTAINERS_PULL_PAUSE_TIMEOUT=120

case "$BUILD_TOOL" in
  maven)
    test -f "$PROJECT_PATH/pom.xml"
    settings_file="$PWD/.mvn/settings.xml"
    test -f "$settings_file"
    mise exec "java@temurin-${RUNTIME_VERSION}" -- java -version
    (cd "$PROJECT_PATH" && mise exec "java@temurin-${RUNTIME_VERSION}" "maven@3.9" -- mvn --batch-mode -s "$settings_file" verify)
    ;;
  gradle)
    test -x "$PROJECT_PATH/gradlew"
    (cd "$PROJECT_PATH" && mise exec "java@temurin-${RUNTIME_VERSION}" -- ./gradlew --no-daemon check assemble)
    ;;
  *) echo "Unsupported Spring build tool: $BUILD_TOOL" >&2; exit 2 ;;
esac

if jq -e 'length == 0' <<<"$exceptions" >/dev/null; then
  trivy fs --exit-code 1 --severity HIGH,CRITICAL --scanners vuln "$PROJECT_PATH"
else
  report="$(mktemp)"
  filtered_report="$(mktemp)"
  trap 'rm -f "$report" "$filtered_report"' EXIT

  trivy fs --exit-code 0 --format json --severity HIGH,CRITICAL --scanners vuln "$PROJECT_PATH" > "$report"

  jq --argjson exceptions "$exceptions" '
    def base_path: (.PkgPath // "" | split("/") | last);
    def display_name: if base_path == "" then .PkgName else "\(.PkgName) (\(base_path))" end;
    def is_exception:
      . as $v
      | any($exceptions[]; .name == $v.PkgName or .name == ($v | display_name));

    .Results = [
      .Results[]
      | if .Vulnerabilities then
          .Vulnerabilities = [.Vulnerabilities[] | select(is_exception | not)]
        else
          .
        end
      | select((.Vulnerabilities // []) | length > 0)
    ]
  ' "$report" > "$filtered_report"

  remaining="$(jq '[.Results[]?.Vulnerabilities[]?] | length' "$filtered_report")"

  if [ "$remaining" -gt 0 ]; then
    trivy convert --format table "$filtered_report"
    exit 1
  fi

  echo "Trivy dependency scan passed after applying configured exceptions:"
  jq -r '.[] | "- \(.name): \(.cause)"' <<<"$exceptions"
fi

semgrep scan --config auto --error "$PROJECT_PATH"

#!/usr/bin/env bash
set -euo pipefail

# Updates Maven versions across all module pom.xml files in this repository,
# while intentionally leaving the workspace root pom.xml unchanged.
#
# What is updated:
# - MassiveSuper/pom.xml project <version>
# - <parent><version> in module poms where parent artifactId is MassiveSuper
#
# What is not updated:
# - Root ./pom.xml
# - Any pom.xml under */target/*
#
# Usage:
#   ./update-pom-versions.sh              # interactive prompt
#   ./update-pom-versions.sh 3.4.2        # non-interactive

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

ROOT_POM="./pom.xml"
MASSIVE_SUPER_POM="./MassiveSuper/pom.xml"

if [[ -t 1 ]]; then
  COLOR_CYAN=$'\033[0;36m'
  COLOR_GREEN=$'\033[0;32m'
  COLOR_YELLOW=$'\033[0;33m'
  COLOR_RED=$'\033[0;31m'
  COLOR_DIM=$'\033[0;90m'
  COLOR_RESET=$'\033[0m'
else
  COLOR_CYAN=''
  COLOR_GREEN=''
  COLOR_YELLOW=''
  COLOR_RED=''
  COLOR_DIM=''
  COLOR_RESET=''
fi

if [[ ! -f "$ROOT_POM" ]]; then
  echo "Error: run this script from the repository root (where ./pom.xml exists)."
  exit 1
fi

trim_version() {
  local v="$1"
  v="${v#"${v%%[![:space:]]*}"}"
  v="${v%"${v##*[![:space:]]}"}"
  printf '%s' "$v"
}

is_valid_version() {
  local v
  v="$(trim_version "$1")"
  [[ -n "$v" ]] || return 1
  [[ "$v" != *" "* ]] || return 1
  [[ "$v" != *"<"* && "$v" != *">"* ]] || return 1
  return 0
}

get_massivesuper_version() {
  local pom_file="$1"

  if [[ ! -f "$pom_file" ]]; then
    return 0
  fi

  awk '
    BEGIN { depth = 0 }
    {
      line = $0

      if (depth == 1 && line ~ /<version>[[:space:]]*[^<]+[[:space:]]*<\/version>/) {
        tmp = line
        sub(/.*<version>[[:space:]]*/, "", tmp)
        sub(/[[:space:]]*<\/version>.*/, "", tmp)
        print tmp
        exit
      }

      open_count = gsub(/<[^\/!?][^>]*>/, "&", line)
      close_count = gsub(/<\/[[:alnum:]_.:-]+>/, "&", line)
      single_tag_count = gsub(/<[^>]+\/>/, "&", line)
      depth += (open_count - single_tag_count) - close_count
      if (depth < 0) depth = 0
    }
  ' "$pom_file"
}

VERSION_FROM_CLI=0
if [[ $# -ge 1 ]]; then
  NEW_VERSION="$1"
  VERSION_FROM_CLI=1
else
  NEW_VERSION=""
fi

CURRENT_VERSION=""
if [[ -f "$MASSIVE_SUPER_POM" ]]; then
  CURRENT_VERSION="$(get_massivesuper_version "$MASSIVE_SUPER_POM" || true)"
fi

echo ""
echo "${COLOR_CYAN}=== MassiveCraft POM Version Update ===${COLOR_RESET}"

if [[ -n "$CURRENT_VERSION" ]]; then
  echo "${COLOR_YELLOW}Current MassiveSuper version: ${CURRENT_VERSION}${COLOR_RESET}"
else
  echo "${COLOR_YELLOW}Current MassiveSuper version: (could not read from MassiveSuper/pom.xml)${COLOR_RESET}"
fi

if [[ "$VERSION_FROM_CLI" -eq 0 ]]; then
  echo ""
  while true; do
    read -rp "Enter new version: " NEW_VERSION
    if is_valid_version "$NEW_VERSION"; then
      break
    fi
    echo "${COLOR_RED}Invalid version. Enter a non-empty version string (e.g. 3.4.2 or 3.4.2-SNAPSHOT).${COLOR_RESET}"
  done
elif ! is_valid_version "$NEW_VERSION"; then
  echo "${COLOR_RED}Invalid version '${NEW_VERSION}'. Enter a non-empty version string without whitespace or XML characters.${COLOR_RESET}"
  exit 1
fi

NEW_VERSION="$(trim_version "$NEW_VERSION")"

echo ""
echo "${COLOR_CYAN}Will update module pom.xml files to version: ${NEW_VERSION}${COLOR_RESET}"

if [[ -n "$CURRENT_VERSION" && "$CURRENT_VERSION" == "$NEW_VERSION" ]]; then
  echo "${COLOR_YELLOW}NOTE: New version matches the current version.${COLOR_RESET}"
fi

if [[ "$VERSION_FROM_CLI" -eq 0 ]]; then
  read -rp "Continue? (y/n): " confirm
  if [[ "$confirm" != "y" ]]; then
    echo "${COLOR_YELLOW}Aborted.${COLOR_RESET}"
    exit 0
  fi
fi

echo ""

update_pom() {
  local pom_file="$1"
  local tmp_file
  tmp_file="$(mktemp)"

  if [[ "$pom_file" == "./MassiveSuper/pom.xml" ]]; then
    # Update the top-level project version (direct child of <project>).
    awk -v new_version="$NEW_VERSION" '
      BEGIN {
        depth = 0
        updated_project_version = 0
      }

      {
        line = $0

        if (!updated_project_version && depth == 1 && line ~ /<version>[[:space:]]*[^<]+[[:space:]]*<\/version>/) {
          sub(/<version>[[:space:]]*[^<]+[[:space:]]*<\/version>/, "<version>" new_version "</version>", line)
          updated_project_version = 1
        }

        print line

        open_count = gsub(/<[^\/!?][^>]*>/, "&", line)
        close_count = gsub(/<\/[[:alnum:]_.:-]+>/, "&", line)
        single_tag_count = gsub(/<[^>]+\/>/, "&", line)
        depth += (open_count - single_tag_count) - close_count
        if (depth < 0) depth = 0
      }
    ' "$pom_file" > "$tmp_file"
  else
    # Update parent version only for parent artifactId MassiveSuper.
    awk -v new_version="$NEW_VERSION" '
      BEGIN {
        in_parent = 0
        parent_is_massivesuper = 0
        updated_parent_version = 0
      }

      {
        line = $0

        if (line ~ /<parent([[:space:]>])/) {
          in_parent = 1
          parent_is_massivesuper = 0
        }

        if (in_parent && line ~ /<artifactId>[[:space:]]*MassiveSuper[[:space:]]*<\/artifactId>/) {
          parent_is_massivesuper = 1
        }

        if (in_parent && parent_is_massivesuper && !updated_parent_version && line ~ /<version>[[:space:]]*[^<]+[[:space:]]*<\/version>/) {
          sub(/<version>[[:space:]]*[^<]+[[:space:]]*<\/version>/, "<version>" new_version "</version>", line)
          updated_parent_version = 1
        }

        print line

        if (line ~ /<\/parent>/) {
          in_parent = 0
          parent_is_massivesuper = 0
        }
      }
    ' "$pom_file" > "$tmp_file"
  fi

  if cmp -s "$pom_file" "$tmp_file"; then
    rm -f "$tmp_file"
    echo "${COLOR_DIM}No changes: ${pom_file}${COLOR_RESET}"
    return 1
  fi

  mv "$tmp_file" "$pom_file"
  echo "${COLOR_GREEN}Updated: ${pom_file}${COLOR_RESET}"
  return 0
}

updated_count=0
unchanged_count=0

while IFS= read -r pom; do
  # Skip workspace root pom.xml only.
  if [[ "$pom" == "$ROOT_POM" ]]; then
    continue
  fi

  if update_pom "$pom"; then
    ((updated_count++)) || true
  else
    ((unchanged_count++)) || true
  fi
done < <(find . -type f -name "pom.xml" -not -path "*/target/*" | sort)

echo ""
if [[ "$updated_count" -gt 0 ]]; then
  echo "${COLOR_GREEN}=== Done. Set version to: ${NEW_VERSION} (${updated_count} file(s) updated, ${unchanged_count} unchanged) ===${COLOR_RESET}"
else
  echo "${COLOR_YELLOW}=== Done. No files were updated (version may already be ${NEW_VERSION}). ===${COLOR_RESET}"
fi

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

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <new-version>"
  echo "Example: $0 3.4.0-alpha-14"
  exit 1
fi

NEW_VERSION="$1"
ROOT_POM="./pom.xml"

if [[ ! -f "$ROOT_POM" ]]; then
  echo "Error: run this script from the repository root (where ./pom.xml exists)."
  exit 1
fi

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
    echo "No changes: $pom_file"
  else
    mv "$tmp_file" "$pom_file"
    echo "Updated: $pom_file"
  fi
}

while IFS= read -r pom; do
  # Skip workspace root pom.xml only.
  if [[ "$pom" == "$ROOT_POM" ]]; then
    continue
  fi
  update_pom "$pom"
done < <(find . -type f -name "pom.xml" -not -path "*/target/*" | sort)

echo "Done. Set version to: $NEW_VERSION"

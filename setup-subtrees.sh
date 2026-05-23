#!/usr/bin/env bash
set -uo pipefail

# setup-subtrees.sh
#
# Run ONCE to:
#   1. Add each plugin fork as a named remote in MassiveCraft
#   2. Split each subdirectory's history into a standalone branch
#   3. Force push that branch to the fork's main branch
#
# This will OVERWRITE the current content of the forks (intentional - you are
# replacing the upstream fork history with your own commit history).
#
# Run from any branch. The current HEAD is what gets pushed.
# After this, use sync-subtrees.sh after each PR merge to keep forks in sync.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

PLUGIN_NAMES=(Factions CreativeGates MassiveHat MassiveBooks FactionsChat)
PLUGIN_PREFIXES=(Factions CreativeGates MassiveHat MassiveBooks FactionsChat)
PLUGIN_REMOTES=(factions-remote creativegates-remote massivehat-remote massivebooks-remote factionschat-remote)
PLUGIN_URLS=(
  "https://github.com/JeremyFail/Factions.git"
  "https://github.com/JeremyFail/CreativeGates.git"
  "https://github.com/JeremyFail/MassiveHat.git"
  "https://github.com/JeremyFail/MassiveBooks.git"
  "https://github.com/JeremyFail/FactionsChat.git"
)

if [[ -t 1 ]]; then
  COLOR_CYAN=$'\033[0;36m'
  COLOR_GREEN=$'\033[0;32m'
  COLOR_YELLOW=$'\033[0;33m'
  COLOR_RED=$'\033[0;31m'
  COLOR_RESET=$'\033[0m'
else
  COLOR_CYAN=''
  COLOR_GREEN=''
  COLOR_YELLOW=''
  COLOR_RED=''
  COLOR_RESET=''
fi

echo ""
echo "${COLOR_CYAN}=== MassiveCraft Subtree Initial Setup ===${COLOR_RESET}"
echo "${COLOR_YELLOW}Current branch: $(git branch --show-current)${COLOR_RESET}"
echo ""
echo "${COLOR_RED}WARNING: This will force push to the main branch of each fork,${COLOR_RESET}"
echo "${COLOR_RED}overwriting any existing content there.${COLOR_RESET}"
echo ""
read -rp "Type 'yes' to continue: " confirm
if [[ "$confirm" != "yes" ]]; then
  echo "${COLOR_YELLOW}Aborted.${COLOR_RESET}"
  exit 0
fi

existing_remotes=()
while IFS= read -r remote_name; do
  existing_remotes+=("$remote_name")
done < <(git remote)

for i in "${!PLUGIN_NAMES[@]}"; do
  name="${PLUGIN_NAMES[$i]}"
  prefix="${PLUGIN_PREFIXES[$i]}"
  remote="${PLUGIN_REMOTES[$i]}"
  url="${PLUGIN_URLS[$i]}"

  echo ""
  echo "${COLOR_CYAN}--- ${name} ---${COLOR_RESET}"

  remote_exists=0
  for existing in "${existing_remotes[@]}"; do
    if [[ "$existing" == "$remote" ]]; then
      remote_exists=1
      break
    fi
  done

  if [[ "$remote_exists" -eq 0 ]]; then
    git remote add "$remote" "$url"
    echo "${COLOR_GREEN}  Added remote: ${remote} -> ${url}${COLOR_RESET}"
    existing_remotes+=("$remote")
  else
    echo "${COLOR_YELLOW}  Remote already exists: ${remote}${COLOR_RESET}"
  fi

  split_branch="$(printf '%s' "$prefix" | tr '[:upper:]' '[:lower:]')-subtree-init"
  echo "${COLOR_CYAN}  Splitting ${prefix}/ history (this may take a while)...${COLOR_RESET}"

  if ! git subtree split --prefix="$prefix" -b "$split_branch"; then
    echo "${COLOR_RED}  ERROR: subtree split failed for ${name}. Skipping.${COLOR_RESET}"
    continue
  fi

  echo "${COLOR_CYAN}  Force pushing to ${remote}/master...${COLOR_RESET}"
  if ! git push "$remote" "${split_branch}:master" --force; then
    echo "${COLOR_RED}  ERROR: push failed for ${name}.${COLOR_RESET}"
  else
    echo "${COLOR_GREEN}  Done: ${name} pushed successfully.${COLOR_RESET}"
  fi

  git branch -D "$split_branch"
done

echo ""
echo "${COLOR_GREEN}=== Setup complete! ===${COLOR_RESET}"
echo "${COLOR_CYAN}Run sync-subtrees.sh after future PR merges to master to keep forks in sync.${COLOR_RESET}"

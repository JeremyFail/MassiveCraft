#!/usr/bin/env bash
set -uo pipefail

# sync-subtrees.sh
#
# Run after merging a PR to master in MassiveCraft to push the updated
# history for each plugin subdirectory to its corresponding fork.
#
# Should be run from the master branch after pulling the latest changes.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

PLUGIN_NAMES=(Factions CreativeGates MassiveHat MassiveBooks FactionsChat)
PLUGIN_PREFIXES=(Factions CreativeGates MassiveHat MassiveBooks FactionsChat)
PLUGIN_REMOTES=(factions-remote creativegates-remote massivehat-remote massivebooks-remote factionschat-remote)

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

trim_string() {
  local v="$1"
  v="${v#"${v%%[![:space:]]*}"}"
  v="${v%"${v##*[![:space:]]}"}"
  printf '%s' "$v"
}

to_lower() {
  printf '%s' "$1" | tr '[:upper:]' '[:lower:]'
}

# Sets SELECTED_INDICES to an array of plugin indexes to process.
# Returns 0 on success, 1 on invalid selection, 2 on ambiguous selection.
resolve_plugin_selection() {
  local choice
  choice="$(trim_string "$1")"
  SELECTED_INDICES=()

  if [[ -z "$choice" ]] || [[ "$(to_lower "$choice")" == "all" || "$(to_lower "$choice")" == "a" ]]; then
    for i in "${!PLUGIN_NAMES[@]}"; do
      SELECTED_INDICES+=("$i")
    done
    return 0
  fi

  if [[ "$choice" =~ ^[0-9]+$ ]]; then
    local idx=$((choice - 1))
    if [[ "$idx" -ge 0 && "$idx" -lt ${#PLUGIN_NAMES[@]} ]]; then
      SELECTED_INDICES+=("$idx")
      return 0
    fi
    return 1
  fi

  local choice_lc
  choice_lc="$(to_lower "$choice")"
  local match_count=0
  local last_match=-1

  for i in "${!PLUGIN_NAMES[@]}"; do
    local name_lc prefix_lc
    name_lc="$(to_lower "${PLUGIN_NAMES[$i]}")"
    prefix_lc="$(to_lower "${PLUGIN_PREFIXES[$i]}")"
    if [[ "$name_lc" == "$choice_lc" || "$prefix_lc" == "$choice_lc" ]]; then
      match_count=$((match_count + 1))
      last_match=$i
    fi
  done

  if [[ "$match_count" -eq 1 ]]; then
    SELECTED_INDICES+=("$last_match")
    return 0
  fi

  if [[ "$match_count" -gt 1 ]]; then
    return 2
  fi

  return 1
}

current_branch="$(git branch --show-current)"

echo ""
echo "${COLOR_CYAN}=== MassiveCraft Subtree Sync ===${COLOR_RESET}"
echo "${COLOR_YELLOW}Current branch: ${current_branch}${COLOR_RESET}"

if [[ "$current_branch" != "master" ]]; then
  echo ""
  echo "${COLOR_YELLOW}WARNING: You are not on master. Subtrees will be synced from '${current_branch}'.${COLOR_RESET}"
  read -rp "Continue? (y/n): " confirm
  if [[ "$confirm" != "y" ]]; then
    echo "${COLOR_YELLOW}Aborted.${COLOR_RESET}"
    exit 0
  fi
fi

echo ""
echo "${COLOR_CYAN}Available plugins:${COLOR_RESET}"
for i in "${!PLUGIN_NAMES[@]}"; do
  echo "  [$((i + 1))] ${PLUGIN_NAMES[$i]}"
done
echo ""
echo "${COLOR_YELLOW}Press Enter to sync all plugins (default), or enter a number or plugin name to sync one.${COLOR_RESET}"

SELECTED_INDICES=()
while true; do
  read -rp "Selection: " choice
  resolve_plugin_selection "$choice"
  result=$?

  if [[ "$result" -eq 1 ]]; then
    echo "${COLOR_RED}Invalid selection. Use a number (1-${#PLUGIN_NAMES[@]}), a plugin name, or press Enter for all.${COLOR_RESET}"
    continue
  fi

  if [[ "$result" -eq 2 ]]; then
    echo "${COLOR_RED}Ambiguous selection. Please be more specific.${COLOR_RESET}"
    continue
  fi

  break
done

echo ""
if [[ "${#SELECTED_INDICES[@]}" -eq 1 ]]; then
  idx="${SELECTED_INDICES[0]}"
  echo "${COLOR_CYAN}Syncing: ${PLUGIN_NAMES[$idx]}${COLOR_RESET}"
else
  selected_names=()
  for idx in "${SELECTED_INDICES[@]}"; do
    selected_names+=("${PLUGIN_NAMES[$idx]}")
  fi
  joined="$(IFS=', '; echo "${selected_names[*]}")"
  echo "${COLOR_CYAN}Syncing all plugins: ${joined}${COLOR_RESET}"
fi

echo ""

failed=()
succeeded=()

for idx in "${SELECTED_INDICES[@]}"; do
  name="${PLUGIN_NAMES[$idx]}"
  prefix="${PLUGIN_PREFIXES[$idx]}"
  remote="${PLUGIN_REMOTES[$idx]}"

  echo "${COLOR_CYAN}--- ${name} ---${COLOR_RESET}"
  echo "${COLOR_CYAN}  Pushing ${prefix}/ to ${remote}/master...${COLOR_RESET}"

  if git subtree push --prefix="$prefix" "$remote" master; then
    echo "${COLOR_GREEN}  Done: ${name}${COLOR_RESET}"
    succeeded+=("$name")
  else
    echo "${COLOR_RED}  ERROR: sync failed for ${name}.${COLOR_RESET}"
    failed+=("$name")
  fi

  echo ""
done

if [[ "${#succeeded[@]}" -gt 0 ]]; then
  joined="$(IFS=', '; echo "${succeeded[*]}")"
  echo "${COLOR_GREEN}Succeeded: ${joined}${COLOR_RESET}"
fi

if [[ "${#failed[@]}" -gt 0 ]]; then
  joined="$(IFS=', '; echo "${failed[*]}")"
  echo "${COLOR_YELLOW}=== Sync complete with errors ===${COLOR_RESET}"
  echo "${COLOR_RED}Failed plugins: ${joined}${COLOR_RESET}"
  exit 1
fi

echo "${COLOR_GREEN}=== Sync complete! All selected plugins pushed successfully. ===${COLOR_RESET}"

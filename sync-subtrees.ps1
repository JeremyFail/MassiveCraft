# sync-subtrees.ps1
#
# Run after merging a PR to master in MassiveCraft to push the updated
# history for each plugin subdirectory to its corresponding fork.
#
# Should be run from the master branch after pulling the latest changes.

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $repoRoot

$plugins = @(
    [PSCustomObject]@{ Name = "Factions";      Prefix = "Factions";      Remote = "factions-remote" },
    [PSCustomObject]@{ Name = "CreativeGates"; Prefix = "CreativeGates"; Remote = "creativegates-remote" },
    [PSCustomObject]@{ Name = "MassiveHat";    Prefix = "MassiveHat";    Remote = "massivehat-remote" },
    [PSCustomObject]@{ Name = "MassiveBooks";  Prefix = "MassiveBooks";  Remote = "massivebooks-remote" },
    [PSCustomObject]@{ Name = "FactionsChat";  Prefix = "FactionsChat";  Remote = "factionschat-remote" }
)

function Resolve-PluginSelection {
    param(
        [string]$Input,
        [array]$AllPlugins
    )

    if ([string]::IsNullOrWhiteSpace($Input)) {
        return ,$AllPlugins
    }

    $trimmed = $Input.Trim()

    if ($trimmed -match '^(all|a)$') {
        return ,$AllPlugins
    }

    if ($trimmed -match '^\d+$') {
        $index = [int]$trimmed - 1
        if ($index -ge 0 -and $index -lt $AllPlugins.Count) {
            return ,@($AllPlugins[$index])
        }
        return $null
    }

    $matches = @($AllPlugins | Where-Object {
        $_.Name -ieq $trimmed -or $_.Prefix -ieq $trimmed
    })

    if ($matches.Count -eq 1) {
        return ,$matches
    }

    if ($matches.Count -gt 1) {
        return @()
    }

    return $null
}

$currentBranch = git branch --show-current

Write-Host ""
Write-Host "=== MassiveCraft Subtree Sync ===" -ForegroundColor Cyan
Write-Host "Current branch: $currentBranch" -ForegroundColor Yellow

if ($currentBranch -ne "master") {
    Write-Host ""
    Write-Host "WARNING: You are not on master. Subtrees will be synced from '$currentBranch'." -ForegroundColor Yellow
    $confirm = Read-Host "Continue? (y/n)"
    if ($confirm -ne "y") {
        Write-Host "Aborted." -ForegroundColor Yellow
        exit 0
    }
}

Write-Host ""
Write-Host "Available plugins:" -ForegroundColor Cyan
for ($i = 0; $i -lt $plugins.Count; $i++) {
    Write-Host ("  [{0}] {1}" -f ($i + 1), $plugins[$i].Name)
}
Write-Host ""
Write-Host "Press Enter to sync all plugins (default), or enter a number or plugin name to sync one." -ForegroundColor Yellow

$selected = $null
while ($null -eq $selected) {
    $choice = Read-Host "Selection"
    $resolved = Resolve-PluginSelection -Input $choice -AllPlugins $plugins

    if ($null -eq $resolved) {
        Write-Host "Invalid selection. Use a number (1-$($plugins.Count)), a plugin name, or press Enter for all." -ForegroundColor Red
        continue
    }

    if ($resolved.Count -eq 0) {
        Write-Host "Ambiguous selection. Please be more specific." -ForegroundColor Red
        continue
    }

    $selected = $resolved
}

if ($selected.Count -eq 1) {
    Write-Host ""
    Write-Host "Syncing: $($selected[0].Name)" -ForegroundColor Cyan
} else {
    Write-Host ""
    Write-Host "Syncing all plugins: $($selected.Name -join ', ')" -ForegroundColor Cyan
}

Write-Host ""

$failed = @()
$succeeded = @()

foreach ($plugin in $selected) {
    Write-Host "--- $($plugin.Name) ---" -ForegroundColor Cyan
    Write-Host "  Pushing $($plugin.Prefix)/ to $($plugin.Remote)/master..." -ForegroundColor Cyan

    git subtree push --prefix=$($plugin.Prefix) $($plugin.Remote) master

    if ($LASTEXITCODE -eq 0) {
        Write-Host "  Done: $($plugin.Name)" -ForegroundColor Green
        $succeeded += $plugin.Name
    } else {
        Write-Host "  ERROR: sync failed for $($plugin.Name)." -ForegroundColor Red
        $failed += $plugin.Name
    }

    Write-Host ""
}

if ($succeeded.Count -gt 0) {
    Write-Host "Succeeded: $($succeeded -join ', ')" -ForegroundColor Green
}

if ($failed.Count -gt 0) {
    Write-Host "=== Sync complete with errors ===" -ForegroundColor Yellow
    Write-Host "Failed plugins: $($failed -join ', ')" -ForegroundColor Red
    exit 1
} else {
    Write-Host "=== Sync complete! All selected plugins pushed successfully. ===" -ForegroundColor Green
}

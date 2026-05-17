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

$failed = @()

foreach ($plugin in $plugins) {
    Write-Host "--- $($plugin.Name) ---" -ForegroundColor Cyan
    Write-Host "  Pushing $($plugin.Prefix)/ to $($plugin.Remote)/master..." -ForegroundColor Cyan

    git subtree push --prefix=$($plugin.Prefix) $($plugin.Remote) master

    if ($LASTEXITCODE -eq 0) {
        Write-Host "  Done: $($plugin.Name)" -ForegroundColor Green
    } else {
        Write-Host "  ERROR: sync failed for $($plugin.Name)." -ForegroundColor Red
        $failed += $plugin.Name
    }

    Write-Host ""
}

if ($failed.Count -gt 0) {
    Write-Host "=== Sync complete with errors ===" -ForegroundColor Yellow
    Write-Host "Failed plugins: $($failed -join ', ')" -ForegroundColor Red
} else {
    Write-Host "=== Sync complete! All plugins pushed successfully. ===" -ForegroundColor Green
}

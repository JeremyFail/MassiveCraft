# setup-subtrees.ps1
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
# After this, use sync-subtrees.ps1 after each PR merge to keep forks in sync.

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $repoRoot

$plugins = @(
    [PSCustomObject]@{ Name = "Factions";      Prefix = "Factions";      Remote = "factions-remote";      Url = "https://github.com/JeremyFail/Factions.git" },
    [PSCustomObject]@{ Name = "CreativeGates"; Prefix = "CreativeGates"; Remote = "creativegates-remote"; Url = "https://github.com/JeremyFail/CreativeGates.git" },
    [PSCustomObject]@{ Name = "MassiveHat";    Prefix = "MassiveHat";    Remote = "massivehat-remote";    Url = "https://github.com/JeremyFail/MassiveHat.git" },
    [PSCustomObject]@{ Name = "MassiveBooks";  Prefix = "MassiveBooks";  Remote = "massivebooks-remote";  Url = "https://github.com/JeremyFail/MassiveBooks.git" },
    [PSCustomObject]@{ Name = "FactionsChat";  Prefix = "FactionsChat";  Remote = "factionschat-remote";  Url = "https://github.com/JeremyFail/FactionsChat.git" }
)

Write-Host ""
Write-Host "=== MassiveCraft Subtree Initial Setup ===" -ForegroundColor Cyan
Write-Host "Current branch: $(git branch --show-current)" -ForegroundColor Yellow
Write-Host ""
Write-Host "WARNING: This will force push to the main branch of each fork," -ForegroundColor Red
Write-Host "overwriting any existing content there." -ForegroundColor Red
Write-Host ""
$confirm = Read-Host "Type 'yes' to continue"
if ($confirm -ne "yes") {
    Write-Host "Aborted." -ForegroundColor Yellow
    exit 0
}

$existingRemotes = git remote

foreach ($plugin in $plugins) {
    Write-Host ""
    Write-Host "--- $($plugin.Name) ---" -ForegroundColor Cyan

    # Add remote if it doesn't already exist
    if ($plugin.Remote -notin $existingRemotes) {
        git remote add $plugin.Remote $plugin.Url
        Write-Host "  Added remote: $($plugin.Remote) -> $($plugin.Url)" -ForegroundColor Green
    } else {
        Write-Host "  Remote already exists: $($plugin.Remote)" -ForegroundColor Yellow
    }

    # Split the subdirectory history into a temporary local branch
    $splitBranch = "$($plugin.Prefix.ToLower())-subtree-init"
    Write-Host "  Splitting $($plugin.Prefix)/ history (this may take a while)..." -ForegroundColor Cyan
    git subtree split --prefix=$($plugin.Prefix) -b $splitBranch

    if ($LASTEXITCODE -ne 0) {
        Write-Host "  ERROR: subtree split failed for $($plugin.Name). Skipping." -ForegroundColor Red
        continue
    }

    # Force push to the fork's master branch
    Write-Host "  Force pushing to $($plugin.Remote)/master..." -ForegroundColor Cyan
    git push $plugin.Remote "${splitBranch}:master" --force

    if ($LASTEXITCODE -ne 0) {
        Write-Host "  ERROR: push failed for $($plugin.Name)." -ForegroundColor Red
    } else {
        Write-Host "  Done: $($plugin.Name) pushed successfully." -ForegroundColor Green
    }

    # Clean up the temporary local branch
    git branch -D $splitBranch
}

Write-Host ""
Write-Host "=== Setup complete! ===" -ForegroundColor Green
Write-Host "Run sync-subtrees.ps1 after future PR merges to master to keep forks in sync." -ForegroundColor Cyan

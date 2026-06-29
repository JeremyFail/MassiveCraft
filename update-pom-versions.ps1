param(
    [Parameter(Position = 0)]
    [string]$NewVersion
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)

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
#   .\update-pom-versions.ps1              # interactive prompt
#   .\update-pom-versions.ps1 3.4.2        # non-interactive

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $repoRoot

$rootPom = Join-Path $repoRoot 'pom.xml'
$massiveSuperPom = Join-Path $repoRoot 'MassiveSuper\pom.xml'

if (-not (Test-Path -LiteralPath $rootPom)) {
    throw 'Run this script from the repository root (where ./pom.xml exists).'
}

function Get-MassiveSuperProjectVersion {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PomPath
    )

    if (-not (Test-Path -LiteralPath $PomPath)) {
        return $null
    }

    $lines = [System.IO.File]::ReadAllLines($PomPath, $Utf8NoBom)
    $depth = 0

    foreach ($line in $lines) {
        if ($depth -eq 1 -and $line -match '<version>\s*([^<]+?)\s*</version>') {
            return $Matches[1].Trim()
        }

        $openCount = ([regex]::Matches($line, '<[^/!?][^>]*>')).Count
        $closeCount = ([regex]::Matches($line, '</[A-Za-z0-9_.:-]+>')).Count
        $singleTagCount = ([regex]::Matches($line, '<[^>]+/>')).Count

        $depth += ($openCount - $singleTagCount) - $closeCount
        if ($depth -lt 0) { $depth = 0 }
    }

    return $null
}

function Test-VersionString {
    param([string]$Version)

    if ([string]::IsNullOrWhiteSpace($Version)) {
        return $false
    }

    $trimmed = $Version.Trim()
    if ($trimmed -match '[<>\s]') {
        return $false
    }

    return $true
}

function Update-MassiveSuperProjectVersion {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PomPath,

        [Parameter(Mandatory = $true)]
        [string]$Version
    )

    $lines = [System.IO.File]::ReadAllLines($PomPath, $Utf8NoBom)
    $depth = 0
    $updated = $false

    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]

        if (-not $updated -and $depth -eq 1 -and $line -match '<version>\s*[^<]+\s*</version>') {
            $lines[$i] = [regex]::Replace($line, '<version>\s*[^<]+\s*</version>', "<version>$Version</version>")
            $updated = $true
        }

        $openCount = ([regex]::Matches($line, '<[^/!?][^>]*>')).Count
        $closeCount = ([regex]::Matches($line, '</[A-Za-z0-9_.:-]+>')).Count
        $singleTagCount = ([regex]::Matches($line, '<[^>]+/>')).Count

        $depth += ($openCount - $singleTagCount) - $closeCount
        if ($depth -lt 0) { $depth = 0 }
    }

    [System.IO.File]::WriteAllLines($PomPath, $lines, $Utf8NoBom)
    return $updated
}

function Update-ParentVersionForMassiveSuper {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PomPath,

        [Parameter(Mandatory = $true)]
        [string]$Version
    )

    $lines = [System.IO.File]::ReadAllLines($PomPath, $Utf8NoBom)
    $inParent = $false
    $parentIsMassiveSuper = $false
    $updated = $false

    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]

        if ($line -match '<parent(\s|>)') {
            $inParent = $true
            $parentIsMassiveSuper = $false
        }

        if ($inParent -and $line -match '<artifactId>\s*MassiveSuper\s*</artifactId>') {
            $parentIsMassiveSuper = $true
        }

        if ($inParent -and $parentIsMassiveSuper -and -not $updated -and $line -match '<version>\s*[^<]+\s*</version>') {
            $lines[$i] = [regex]::Replace($line, '<version>\s*[^<]+\s*</version>', "<version>$Version</version>")
            $updated = $true
        }

        if ($line -match '</parent>') {
            $inParent = $false
            $parentIsMassiveSuper = $false
        }
    }

    [System.IO.File]::WriteAllLines($PomPath, $lines, $Utf8NoBom)
    return $updated
}

$currentVersion = Get-MassiveSuperProjectVersion -PomPath $massiveSuperPom

Write-Host ""
Write-Host "=== MassiveCraft POM Version Update ===" -ForegroundColor Cyan

if ($currentVersion) {
    Write-Host "Current MassiveSuper version: $currentVersion" -ForegroundColor Yellow
} else {
    Write-Host "Current MassiveSuper version: (could not read from MassiveSuper\pom.xml)" -ForegroundColor Yellow
}

if ([string]::IsNullOrWhiteSpace($NewVersion)) {
    Write-Host ""
    while (-not (Test-VersionString -Version $NewVersion)) {
        $NewVersion = Read-Host "Enter new version"
        if (-not (Test-VersionString -Version $NewVersion)) {
            Write-Host "Invalid version. Enter a non-empty version string (e.g. 3.4.2 or 3.4.2-SNAPSHOT)." -ForegroundColor Red
            $NewVersion = $null
        }
    }
} elseif (-not (Test-VersionString -Version $NewVersion)) {
    throw "Invalid version '$NewVersion'. Enter a non-empty version string without whitespace or XML characters."
}

$NewVersion = $NewVersion.Trim()

Write-Host ""
Write-Host "Will update module pom.xml files to version: $NewVersion" -ForegroundColor Cyan
if ($currentVersion -and $currentVersion -eq $NewVersion) {
    Write-Host "NOTE: New version matches the current version." -ForegroundColor Yellow
}

$versionFromCommandLine = $PSBoundParameters.ContainsKey('NewVersion')

if (-not $versionFromCommandLine) {
    $confirm = Read-Host "Continue? (y/n)"
    if ($confirm -ne 'y') {
        Write-Host "Aborted." -ForegroundColor Yellow
        exit 0
    }
}

Write-Host ""

$allPoms = Get-ChildItem -Path . -Filter 'pom.xml' -Recurse -File |
    Where-Object {
        $_.FullName -notmatch '[\\/]target[\\/]' -and
        $_.FullName -ne $rootPom
    } |
    Sort-Object FullName

$updatedCount = 0
$unchangedCount = 0

foreach ($pom in $allPoms) {
    $relative = Resolve-Path -LiteralPath $pom.FullName -Relative

    if ($relative -eq '.\MassiveSuper\pom.xml') {
        $changed = Update-MassiveSuperProjectVersion -PomPath $pom.FullName -Version $NewVersion
    } else {
        $changed = Update-ParentVersionForMassiveSuper -PomPath $pom.FullName -Version $NewVersion
    }

    if ($changed) {
        Write-Host "Updated: $relative" -ForegroundColor Green
        $updatedCount++
    } else {
        Write-Host "No changes: $relative" -ForegroundColor DarkGray
        $unchangedCount++
    }
}

Write-Host ""
if ($updatedCount -gt 0) {
    Write-Host "=== Done. Set version to: $NewVersion ($updatedCount file(s) updated, $unchangedCount unchanged) ===" -ForegroundColor Green
} else {
    Write-Host "=== Done. No files were updated (version may already be $NewVersion). ===" -ForegroundColor Yellow
}

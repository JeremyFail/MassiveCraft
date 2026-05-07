param(
    [Parameter(Mandatory = $true, Position = 0)]
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

$rootPom = Join-Path (Get-Location) 'pom.xml'
if (-not (Test-Path -LiteralPath $rootPom)) {
    throw 'Run this script from the repository root (where ./pom.xml exists).'
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

$allPoms = Get-ChildItem -Path . -Filter 'pom.xml' -Recurse -File |
    Where-Object {
        $_.FullName -notmatch '[\\/]target[\\/]' -and
        $_.FullName -ne $rootPom
    } |
    Sort-Object FullName

foreach ($pom in $allPoms) {
    $relative = Resolve-Path -LiteralPath $pom.FullName -Relative

    if ($relative -eq '.\MassiveSuper\pom.xml') {
        $changed = Update-MassiveSuperProjectVersion -PomPath $pom.FullName -Version $NewVersion
        if ($changed) {
            Write-Host "Updated: $relative"
        } else {
            Write-Host "No changes: $relative"
        }
    } else {
        $changed = Update-ParentVersionForMassiveSuper -PomPath $pom.FullName -Version $NewVersion
        if ($changed) {
            Write-Host "Updated: $relative"
        } else {
            Write-Host "No changes: $relative"
        }
    }
}

Write-Host "Done. Set version to: $NewVersion"

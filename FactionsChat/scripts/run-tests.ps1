# Run FactionsChat unit tests against Paper MockBukkit lines locally.
# Usage:
#   .\scripts\run-tests.ps1              # both 26 and 1.21
#   .\scripts\run-tests.ps1 -Line 26     # Paper 26 / Adventure 5 only
#   .\scripts\run-tests.ps1 -Line 1.21   # Paper 1.21 / Adventure 4 only

param(
    [ValidateSet("all", "26", "1.21")]
    [string] $Line = "all"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Invoke-PaperTests([string] $paperTestLine)
{
    Write-Host ""
    Write-Host "=== FactionsChat tests (paperTestLine=$paperTestLine) ===" -ForegroundColor Cyan
    # Native mvn writes SLF4J warnings to stderr; don't let that trip $ErrorActionPreference Stop.
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try
    {
        & mvn -q test "-DpaperTestLine=$paperTestLine"
        $code = $LASTEXITCODE
    }
    finally
    {
        $ErrorActionPreference = $prev
    }
    if ($code -ne 0)
    {
        throw "Tests failed for paperTestLine=$paperTestLine (exit $code)"
    }
}

switch ($Line)
{
    "26" { Invoke-PaperTests "26" }
    "1.21" { Invoke-PaperTests "1.21" }
    default {
        Invoke-PaperTests "26"
        Invoke-PaperTests "1.21"
    }
}

Write-Host ""
Write-Host "All requested FactionsChat test runs passed." -ForegroundColor Green

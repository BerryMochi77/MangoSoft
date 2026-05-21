param(
    [string]$PackageName = "com.example.comp2100miniproject",
    [string]$OutputPath = "android/app/src/main/assets/posts_seed.json"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$target = Join-Path $repoRoot $OutputPath
$targetDir = Split-Path -Parent $target
New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

$json = adb exec-out run-as $PackageName cat files/posts.json
if ([string]::IsNullOrWhiteSpace($json)) {
    throw "No posts.json was found in the app sandbox. Create at least one post and run the app once first."
}

$json | Set-Content -Path $target -Encoding UTF8
Write-Host "Exported app posts database to $OutputPath"
Write-Host "Commit this file so teammates can pull the shared seed data."

param([switch]$Verify)
$ErrorActionPreference = 'Stop'
$fabricRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $fabricRoot
& .\gradlew.bat classes exportHotSwap --console=plain
if ($LASTEXITCODE -ne 0) { throw 'Fabric preparation failed' }
$config = Get-Content -Raw -LiteralPath (Join-Path $fabricRoot 'build/hotswap-launch.json') | ConvertFrom-Json
$watchArgs = @('--add-modules','jdk.jdi,jdk.compiler',('"'+(Join-Path $PSScriptRoot 'HotSwap.java')+'"'),('"'+$fabricRoot+'"'))
if ($Verify) { $watchArgs += 'verify' }
$watcher = Start-Process -FilePath $config.java -ArgumentList $watchArgs -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $fabricRoot 'build/hotswap-watcher.log') -RedirectStandardError (Join-Path $fabricRoot 'build/hotswap-watcher-errors.log')
try { if ($Verify) { & .\gradlew.bat runClient -PhotSwapClient=true -PsmokeClient=true --console=plain } else { & .\gradlew.bat runClient -PhotSwapClient=true --console=plain } }
finally { if (!$watcher.HasExited) { Stop-Process -Id $watcher.Id } }
if ($Verify -and ((Get-Content -Raw -LiteralPath (Join-Path $fabricRoot 'build/hotswap-watcher.log')) -notmatch 'HOT SWAP SELFTEST PASS')) { throw 'Hot swap verification did not pass; inspect build/hotswap-watcher-errors.log' }


<#
.SYNOPSIS
  Starts the local sample database/backend, seeds test data, and starts the frontend.
.EXAMPLE
  .\scripts\start-local-test.ps1
.EXAMPLE
  .\scripts\start-local-test.ps1 -BackendOnly
#>
param(
    [Security.SecureString]$AccountPassword,
    [switch]$BackendOnly,
    [ValidatePattern('^[a-zA-Z0-9][a-zA-Z0-9_.-]+$')][string]$ContainerName = 'unichope-manual-db',
    [ValidateRange(1024, 65535)][int]$DatabasePort = 55433,
    [ValidateRange(1024, 65535)][int]$BackendPort = 8080,
    [ValidateRange(1024, 65535)][int]$FrontendPort = 5173
)

$ErrorActionPreference = 'Stop'
$backendDir = Split-Path $PSScriptRoot -Parent
$frontendDir = Join-Path (Split-Path $backendDir -Parent) 'frontend'
$runtimeDir = Join-Path $backendDir "build\local-test\$ContainerName"
$container = $ContainerName
$database = 'unichope_manual'
$databaseUser = 'unichope_local'
$databasePassword = 'unichope_local_only' # Synthetic, loopback-only database.
$baseUrl = "http://127.0.0.1:$BackendPort"
$frontendUrl = "http://127.0.0.1:$FrontendPort"
$adminEmail = 'admin@example.edu'
New-Item -ItemType Directory -Path $runtimeDir -Force | Out-Null

function Wait-Until([scriptblock]$Check, [string]$Description) {
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        if (& $Check) { return }
        Start-Sleep -Seconds 1
    }
    throw "Timed out waiting for $Description. See $runtimeDir."
}

function Test-Backend {
    try {
        $csrf = Invoke-RestMethod "$baseUrl/api/auth/csrf" -TimeoutSec 2
        return [bool]($csrf.headerName -and $csrf.token)
    } catch { return $false }
}

function Test-Frontend {
    try {
        $page = Invoke-WebRequest "$frontendUrl/login" -UseBasicParsing -TimeoutSec 2
        return $page.StatusCode -eq 200 -and $page.Content -match '<title>UniChope</title>'
    } catch { return $false }
}

function Invoke-Gradle([string]$Task, [string]$OutputName) {
    $outputPath = Join-Path $runtimeDir $OutputName
    Push-Location $backendDir
    try {
        & .\gradlew.bat $Task --console=plain *> $outputPath
        if ($LASTEXITCODE -ne 0) { throw "$Task failed. See $outputPath." }
    } finally { Pop-Location }
}

Get-Command docker -ErrorAction Stop | Out-Null
& docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Start Docker Desktop, then run this command again.' }

$names = @(& docker ps -a --format '{{.Names}}')
if ($LASTEXITCODE -ne 0) { throw 'Could not list local Docker containers.' }
if ($container -in $names) {
    $details = (& docker inspect $container | ConvertFrom-Json)[0]
    $binding = $details.HostConfig.PortBindings.'5432/tcp'
    if ($details.Config.Image -ne 'postgres:17' -or
        "POSTGRES_DB=$database" -notin $details.Config.Env -or
        "POSTGRES_USER=$databaseUser" -notin $details.Config.Env -or
        "POSTGRES_PASSWORD=$databasePassword" -notin $details.Config.Env -or
        @($binding).Count -ne 1 -or $binding[0].HostIp -ne '127.0.0.1' -or $binding[0].HostPort -ne "$DatabasePort") {
        throw "Container $container does not match this sample setup; it was left unchanged."
    }
    if (-not $details.State.Running) {
        & docker start $container | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Could not start $container." }
    }
} else {
    & docker run --detach --name $container --publish "127.0.0.1:${DatabasePort}:5432" `
        --env "POSTGRES_DB=$database" --env "POSTGRES_USER=$databaseUser" `
        --env "POSTGRES_PASSWORD=$databasePassword" postgres:17 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the local sample database.' }
}
Wait-Until { & docker exec $container pg_isready -U $databaseUser -d $database *> $null; return $LASTEXITCODE -eq 0 } 'PostgreSQL'

if (-not $AccountPassword) {
    $AccountPassword = Read-Host 'Test account password (reuse the same password on later runs)' -AsSecureString
}
$passwordPtr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($AccountPassword)
$password = $null
$savedEnvironment = @{}
$settings = @{
    UNICHOPE_JDBC_URL = "jdbc:postgresql://127.0.0.1:$DatabasePort/unichope_manual"
    UNICHOPE_JDBC_USER = $databaseUser
    UNICHOPE_JDBC_PASSWORD = $databasePassword
    UNICHOPE_ADMIN_NAME = 'Local Test Admin'
    UNICHOPE_ADMIN_EMAIL = $adminEmail
    UNICHOPE_ADMIN_PASSWORD = $null
    API_PROXY_TARGET = $baseUrl
}
try {
    $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPtr)
    if ($password.Length -lt 8 -or $password.Length -gt 128) { throw 'Use a test password of 8-128 characters.' }
    $settings.UNICHOPE_ADMIN_PASSWORD = $password
    foreach ($key in $settings.Keys) {
        $savedEnvironment[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
        [Environment]::SetEnvironmentVariable($key, $settings[$key], 'Process')
    }

    Invoke-Gradle 'bootJar' 'build.txt'
    $hasMarkerTable = & docker exec $container psql -U $databaseUser -d $database -Atc "SELECT to_regclass('public.bootstrap_state') IS NOT NULL"
    if ($LASTEXITCODE -ne 0) { throw 'Could not check the sample database.' }
    $bootstrapped = $false
    if ($hasMarkerTable -eq 't') {
        $marker = & docker exec $container psql -U $databaseUser -d $database -Atc 'SELECT EXISTS(SELECT 1 FROM bootstrap_state)'
        if ($LASTEXITCODE -ne 0) { throw 'Could not check the administrator marker.' }
        $bootstrapped = $marker -eq 't'
    }
    if (-not $bootstrapped) { Invoke-Gradle 'bootstrapAdmin' 'bootstrap.txt' }

    # Setup credentials are not inherited by the long-running application.
    foreach ($key in @('UNICHOPE_ADMIN_NAME', 'UNICHOPE_ADMIN_EMAIL', 'UNICHOPE_ADMIN_PASSWORD')) {
        [Environment]::SetEnvironmentVariable($key, $null, 'Process')
    }
    if (-not (Test-Backend)) {
        $java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
        $jar = Join-Path $backendDir 'build\libs\unichope-0.0.1-SNAPSHOT.jar'
        $app = Start-Process -FilePath $java -ArgumentList '-jar', ('"' + $jar + '"'), '--spring.profiles.active=app-api,local', "--server.port=$BackendPort", '--server.address=127.0.0.1' `
            -WorkingDirectory $backendDir -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $runtimeDir 'backend.out.txt') -RedirectStandardError (Join-Path $runtimeDir 'backend.err.txt')
        Wait-Until { if ($app.HasExited) { throw "Backend exited. See $runtimeDir." }; Test-Backend } 'the backend'
    }
    & (Join-Path $PSScriptRoot 'seed-local.ps1') -BaseUrl $baseUrl -AdminEmail $adminEmail -AdminPassword $password -SeedPassword $password

    if (-not $BackendOnly) {
        Get-Command npm.cmd -ErrorAction Stop | Out-Null
        if (-not (Test-Path (Join-Path $frontendDir 'node_modules\vite\bin\vite.js'))) {
            Push-Location $frontendDir
            try { & npm.cmd ci; if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' } }
            finally { Pop-Location }
        }
        if (-not (Test-Frontend)) {
            $node = (Get-Command node -ErrorAction Stop).Source
            $vite = Join-Path $frontendDir 'node_modules\vite\bin\vite.js'
            $ui = Start-Process -FilePath $node -ArgumentList ('"' + $vite + '"'), '--host', '127.0.0.1', '--port', "$FrontendPort", '--strictPort' `
                -WorkingDirectory $frontendDir -WindowStyle Hidden -PassThru `
                -RedirectStandardOutput (Join-Path $runtimeDir 'frontend.out.txt') -RedirectStandardError (Join-Path $runtimeDir 'frontend.err.txt')
            Wait-Until { if ($ui.HasExited) { throw "Frontend exited. See $runtimeDir." }; Test-Frontend } 'the frontend'
        }
        $proxy = Invoke-RestMethod "$frontendUrl/api/auth/csrf" -TimeoutSec 5
        if (-not ($proxy.headerName -and $proxy.token)) { throw 'The frontend API proxy is not ready.' }
    }
    if ($BackendOnly) { Write-Host "Backend ready: $baseUrl" }
    else { Write-Host "Ready: $frontendUrl/login (backend: $baseUrl)" }
    Write-Host 'Accounts: admin@example.edu, student1@example.edu, student2@example.edu, tutor1@example.edu, tutor2@example.edu'
    Write-Host 'All sample accounts use the password you supplied. Existing passwords are never reset.'
    Write-Host "Runtime output: $runtimeDir"
    Write-Host 'Database data survives stopping/restarting its container. Removing the container deletes this sample database.'
} finally {
    foreach ($key in $savedEnvironment.Keys) { [Environment]::SetEnvironmentVariable($key, $savedEnvironment[$key], 'Process') }
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPtr)
    $settings.UNICHOPE_ADMIN_PASSWORD = $null
    $password = $null
}

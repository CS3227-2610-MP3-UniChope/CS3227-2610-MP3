<#
.SYNOPSIS
  Fills a LOCAL UniChope backend with synthetic test data through the real HTTP API.

.DESCRIPTION
  Local development only. Refuses any base URL that is not localhost/127.0.0.1.
  Every record goes through the normal Admin, Tutor and Student endpoints, so the
  usual validation, locking and CSRF rules apply. Safe to rerun: existing modules,
  accounts, assignments and overlapping slots are reused or skipped.

  Prerequisites: the backend is running with --spring.profiles.active=app-api,local
  and the initial Admin was created with `gradlew bootstrapAdmin`.

  Creates (all synthetic, example.edu addresses):
    modules  CS2030, CS2040, MA1521
    tutors   tutor1@example.edu (CS2030, CS2040), tutor2@example.edu (CS2040, MA1521)
    students student1@example.edu, student2@example.edu
    slots    about 20 over the next 7 days (SGT), including same-time slots from both
             tutors, back-to-back slots and one overnight slot
    bookings student1: one upcoming booking and one cancelled booking

  No passwords are stored in this file. You are prompted for the Admin password and
  for one password shared by all seeded accounts (8-128 characters).

.EXAMPLE
  .\scripts\seed-local.ps1 -AdminEmail admin@example.edu
#>
param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [Parameter(Mandatory = $true)][string]$AdminEmail,
    [string]$AdminPassword,
    [string]$SeedPassword
)

$ErrorActionPreference = 'Stop'

$uri = [Uri]$BaseUrl
if ($uri.Host -notin @('localhost', '127.0.0.1')) {
    throw "Refusing to seed '$BaseUrl': synthetic data is for a local backend only."
}

function Read-Plain([string]$prompt) {
    $secure = Read-Host -Prompt $prompt -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}
if (-not $AdminPassword) { $AdminPassword = Read-Plain 'Admin password' }
if (-not $SeedPassword) { $SeedPassword = Read-Plain 'Password for seeded tutor/student accounts (8-128 chars)' }
if ($SeedPassword.Length -lt 8 -or $SeedPassword.Length -gt 128) { throw 'Seed password must be 8-128 characters.' }

# One HTTP call with the session's cookies and CSRF header. Returns status and parsed body.
function Invoke-Api($Session, [string]$Method, [string]$Path, $Body = $null) {
    $headers = @{ Accept = 'application/json' }
    if ($Method -ne 'GET' -and $Session.Csrf) { $headers[$Session.Csrf.headerName] = $Session.Csrf.token }
    $params = @{ Uri = "$BaseUrl$Path"; Method = $Method; WebSession = $Session.Web; Headers = $headers; UseBasicParsing = $true }
    if ($Method -ne 'GET') {
        $json = if ($null -eq $Body) { '{}' } else { $Body | ConvertTo-Json -Compress -Depth 5 }
        $params.Body = [Text.Encoding]::UTF8.GetBytes($json)
        $params.ContentType = 'application/json'
    }
    try {
        $response = Invoke-WebRequest @params
        $data = if ($response.Content) { $response.Content | ConvertFrom-Json } else { $null }
        return @{ Status = [int]$response.StatusCode; Body = $data }
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        if (-not $status) { throw }
        $data = $null
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) { try { $data = $_.ErrorDetails.Message | ConvertFrom-Json } catch {} }
        return @{ Status = [int]$status; Body = $data }
    }
}

function Update-Csrf($Session) {
    $Session.Csrf = $null
    $Session.Csrf = (Invoke-Api $Session 'GET' '/api/auth/csrf').Body
}

function New-Session([string]$Email, [string]$Password) {
    $session = @{ Web = New-Object Microsoft.PowerShell.Commands.WebRequestSession; Csrf = $null }
    Update-Csrf $session
    $login = Invoke-Api $session 'POST' '/api/auth/login' @{ email = $Email; password = $Password }
    if ($login.Status -ne 200) { throw "Login failed for $Email (HTTP $($login.Status) $($login.Body.code))." }
    Update-Csrf $session   # the token changes after login
    Write-Host "Signed in as $Email ($($login.Body.role))"
    return $session
}

function Assert-Status($Result, [int[]]$Allowed, [string]$What) {
    if ($Result.Status -notin $Allowed) { throw "$What failed: HTTP $($Result.Status) $($Result.Body.code) $($Result.Body.message)" }
}

# ---- Admin: modules, tutors, students, assignments ----
$admin = New-Session $AdminEmail $AdminPassword

$moduleSpecs = @(
    @{ code = 'CS2030'; name = 'Programming Methodology II' },
    @{ code = 'CS2040'; name = 'Data Structures and Algorithms' },
    @{ code = 'MA1521'; name = 'Calculus for Computing' }
)
foreach ($m in $moduleSpecs) {
    $r = Invoke-Api $admin 'POST' '/api/admin/modules' $m
    Assert-Status $r @(201, 409) "Create module $($m.code)"
}
$modules = @{}
foreach ($m in (Invoke-Api $admin 'GET' '/api/admin/modules').Body) { $modules[$m.code] = $m.id }

$userSpecs = @(
    @{ role = 'TUTOR'; name = 'Alex Tan'; email = 'tutor1@example.edu' },
    @{ role = 'TUTOR'; name = 'Priya Nair'; email = 'tutor2@example.edu' },
    @{ role = 'STUDENT'; name = 'Jamie Lee'; email = 'student1@example.edu' },
    @{ role = 'STUDENT'; name = 'Sam Wong'; email = 'student2@example.edu' }
)
foreach ($u in $userSpecs) {
    $r = Invoke-Api $admin 'POST' '/api/admin/users' @{ role = $u.role; name = $u.name; email = $u.email; password = $SeedPassword }
    Assert-Status $r @(201, 409) "Create $($u.email)"
}
$users = @{}
foreach ($u in (Invoke-Api $admin 'GET' '/api/admin/users').Body) { $users[$u.email] = $u.id }

$assignments = @(
    @('tutor1@example.edu', 'CS2030'), @('tutor1@example.edu', 'CS2040'),
    @('tutor2@example.edu', 'CS2040'), @('tutor2@example.edu', 'MA1521')
)
foreach ($a in $assignments) {
    $r = Invoke-Api $admin 'PUT' "/api/admin/assignments/$($users[$a[0]])/$($modules[$a[1]])" @{}
    Assert-Status $r @(204) "Assign $($a[0]) to $($a[1])"
}
Write-Host 'Modules, accounts and assignments ready.'

# ---- Tutors: slots over the next 7 days (times are SGT, sent as UTC) ----
$todaySgt = [DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(8)).Date
function To-Utc([int]$dayOffset, [string]$hhmm) {
    $parts = $hhmm.Split(':')
    $sgt = New-Object DateTimeOffset ($todaySgt.AddDays($dayOffset).AddHours([int]$parts[0]).AddMinutes([int]$parts[1])), ([TimeSpan]::FromHours(8))
    return $sgt.UtcDateTime.ToString("yyyy-MM-dd'T'HH:mm:ss'Z'")
}

# day offset, start SGT, minutes, module
$slotPlan = @{
    'tutor1@example.edu' = @(
        @(1, '09:00', 60, 'CS2030'), @(1, '10:00', 30, 'CS2030'), @(1, '14:00', 60, 'CS2040'),
        @(2, '11:00', 60, 'CS2040'), @(3, '09:30', 45, 'CS2030'), @(3, '16:00', 60, 'CS2040'),
        @(4, '13:00', 30, 'CS2030'), @(5, '10:00', 60, 'CS2040'), @(6, '15:00', 60, 'CS2030'),
        @(7, '09:00', 60, 'CS2030')
    )
    'tutor2@example.edu' = @(
        @(1, '09:00', 60, 'MA1521'), @(1, '14:30', 60, 'CS2040'), @(2, '11:00', 30, 'MA1521'),
        @(2, '23:30', 60, 'CS2040'), @(3, '10:00', 60, 'MA1521'), @(4, '13:00', 60, 'CS2040'),
        @(5, '17:00', 45, 'MA1521'), @(6, '08:00', 60, 'MA1521'), @(7, '12:00', 30, 'CS2040'),
        @(7, '18:00', 60, 'MA1521')
    )
}
$created = 0; $skipped = 0
foreach ($tutorEmail in $slotPlan.Keys) {
    $tutor = New-Session $tutorEmail $SeedPassword
    foreach ($s in $slotPlan[$tutorEmail]) {
        $startUtc = To-Utc $s[0] $s[1]
        $endUtc = ([DateTimeOffset]::Parse($startUtc)).AddMinutes($s[2]).UtcDateTime.ToString("yyyy-MM-dd'T'HH:mm:ss'Z'")
        $r = Invoke-Api $tutor 'POST' '/api/tutor/slots' @{ moduleId = $modules[$s[3]]; startTime = $startUtc; endTime = $endUtc }
        if ($r.Status -eq 201) { $created++ }
        elseif ($r.Status -eq 409 -and $r.Body.code -eq 'SLOT_OVERLAP') { $skipped++ }
        else { Assert-Status $r @(201) "Create slot $tutorEmail $($s[3]) day+$($s[0]) $($s[1])" }
    }
}
Write-Host "Slots: $created created, $skipped already present."

# ---- Student: keep an upcoming sample and a cancellation history example ----
$student = New-Session 'student1@example.edu' $SeedPassword
$existing = @((Invoke-Api $student 'GET' '/api/student/bookings').Body)
$hasUpcoming = @($existing | Where-Object { $_.status -eq 'ACTIVE' -and [DateTimeOffset]::Parse($_.startTime) -gt [DateTimeOffset]::UtcNow }).Count -gt 0
$hasCancelled = @($existing | Where-Object { $_.status -eq 'CANCELLED' }).Count -gt 0

function New-SampleBooking([string]$Purpose) {
    $available = @((Invoke-Api $student 'GET' '/api/student/slots').Body)
    foreach ($slot in $available) {
        $result = Invoke-Api $student 'POST' '/api/student/bookings' @{ slotId = $slot.id }
        if ($result.Status -eq 201) { return $result.Body }
        # Only choose a different slot after an explicit rejection. Never retry an
        # uncertain mutation, and never alter an existing booking to make room.
        if ($result.Status -eq 409 -and $result.Body.code -in @('BOOKING_OVERLAP', 'SLOT_UNAVAILABLE')) { continue }
        Assert-Status $result @(201) $Purpose
    }
    throw "No available non-overlapping slot for $Purpose; existing bookings were left unchanged."
}

if (-not $hasUpcoming) {
    $null = New-SampleBooking 'upcoming sample booking'
    Write-Host 'student1: created an upcoming sample booking.'
}
if (-not $hasCancelled) {
    $temporary = New-SampleBooking 'cancelled sample booking'
    $cancel = Invoke-Api $student 'POST' "/api/student/bookings/$($temporary.id)/cancel" @{}
    Assert-Status $cancel @(200) 'Cancel sample booking'
    Write-Host 'student1: created a cancelled sample booking.'
}
if ($hasUpcoming -and $hasCancelled) { Write-Host 'student1: upcoming and cancelled samples already present; left unchanged.' }

Write-Host ''
Write-Host "Done. Local sample backend: $BaseUrl"
Write-Host '  student1@example.edu  (has bookings)   student2@example.edu  (no bookings)'
Write-Host '  tutor1@example.edu, tutor2@example.edu, and your Admin account'
Write-Host 'Seeded accounts use the password you entered for this run.'

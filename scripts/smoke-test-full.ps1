# Full platform smoke test — auth, accounts, transfers, limits, scheduled transfers
$ErrorActionPreference = "Stop"

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$mvn = "mvn"
if (Get-Command mvn -ErrorAction SilentlyContinue) { $mvn = "mvn" }

function Wait-ForHealth($url, $label, $maxSeconds = 60) {
    $deadline = (Get-Date).AddSeconds($maxSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $r = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 3
            if ($r.status -eq "UP") {
                Write-Host "  OK  $label"
                return
            }
        } catch { Start-Sleep -Seconds 2 }
    }
    throw "$label did not become healthy at $url"
}

function Invoke-Api($Method, $Uri, $Body = $null, $Token = $null) {
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    $params = @{ Method = $Method; Uri = $Uri; Headers = $headers }
    if ($Body) { $params.Body = ($Body | ConvertTo-Json -Depth 5 -Compress) }
    return Invoke-RestMethod @params
}

Write-Host "`n=== Banking Platform Smoke Test ===`n"

Write-Host "==> PostgreSQL"
docker exec banking-postgres pg_isready -U banking -d banking | Out-Null
if ($LASTEXITCODE -ne 0) {
    docker compose -f "$PSScriptRoot\..\docker-compose.yml" up -d postgres
    Start-Sleep -Seconds 8
}
Write-Host "  OK  PostgreSQL"

$startedAuth = $null
$startedAccount = $null

try {
    # Start auth if not running
    try {
        Wait-ForHealth "http://localhost:8081/api/v1/auth/health" "auth-service (already running)" 5
    } catch {
        Write-Host "==> Starting auth-service..."
        $startedAuth = Start-Process -FilePath $mvn -ArgumentList "-pl","auth-service","-am","spring-boot:run","-q" -WorkingDirectory "$PSScriptRoot\.." -PassThru -WindowStyle Hidden
        Wait-ForHealth "http://localhost:8081/api/v1/auth/health" "auth-service" 90
    }

    # Start account if not running
    try {
        Wait-ForHealth "http://localhost:8082/api/v1/accounts/health" "account-service (already running)" 5
    } catch {
        Write-Host "==> Starting account-service..."
        $startedAccount = Start-Process -FilePath $mvn -ArgumentList "-pl","account-service","-am","spring-boot:run","-q" -WorkingDirectory "$PSScriptRoot\.." -PassThru -WindowStyle Hidden
        Wait-ForHealth "http://localhost:8082/api/v1/accounts/health" "account-service" 90
    }

    Wait-ForHealth "http://localhost:8083/api/v1/transactions/health" "transaction-service" 10

    $suffix = [guid]::NewGuid().ToString("N").Substring(0, 8)
    $userEmail = "smoke-user-$suffix@example.com"
    $adminEmail = "smoke-admin-$suffix@example.com"
    $password = "password123"

    Write-Host "`n==> Register user + admin"
    $userReg = Invoke-Api Post "http://localhost:8081/api/v1/auth/register" @{
        email = $userEmail; password = $password; firstName = "Smoke"; lastName = "User"; phone = "9876543210"
    }
    Invoke-Api Post "http://localhost:8081/api/v1/auth/register" @{
        email = $adminEmail; password = $password; firstName = "Smoke"; lastName = "Admin"; phone = "9876543211"
    } | Out-Null

    Write-Host "==> Promote admin via SQL"
    docker exec banking-postgres psql -U banking -d banking -c `
        "UPDATE auth.users SET role = 'ADMIN' WHERE email = '$adminEmail';" | Out-Null

    Write-Host "==> KYC submit + approve"
    Invoke-Api Post "http://localhost:8081/api/v1/kyc/submit" @{
        documentType = "PASSPORT"; documentNumber = "P$suffix"; dateOfBirth = "1995-05-15"
        addressLine1 = "123 Main St"; city = "Mumbai"; state = "Maharashtra"; postalCode = "400001"; country = "India"
    } $userReg.accessToken | Out-Null

    $adminLogin = Invoke-Api Post "http://localhost:8081/api/v1/auth/login" @{ email = $adminEmail; password = $password }
    $userId = $userReg.user.id

    Invoke-Api Post "http://localhost:8081/api/v1/kyc/admin/$userId/approve" $null $adminLogin.accessToken | Out-Null

    Write-Host "==> Re-login for VERIFIED JWT"
    $login = Invoke-Api Post "http://localhost:8081/api/v1/auth/login" @{ email = $userEmail; password = $password }
    $token = $login.accessToken

    Write-Host "==> Create two accounts"
    $acct1 = Invoke-Api Post "http://localhost:8082/api/v1/accounts" @{ accountType = "SAVINGS"; currency = "INR" } $token
    $acct2 = Invoke-Api Post "http://localhost:8082/api/v1/accounts" @{ accountType = "CURRENT"; currency = "INR" } $token
    Write-Host "  Account 1: $($acct1.accountNumber) | Account 2: $($acct2.accountNumber)"

    Write-Host "==> Fund source account (SQL for test)"
    docker exec banking-postgres psql -U banking -d banking -c `
        "UPDATE account.accounts SET balance = 10000.00 WHERE id = '$($acct1.id)';" | Out-Null

    Write-Host "==> Check daily transfer limits"
    $limits = Invoke-Api Get "http://localhost:8083/api/v1/transactions/limits" $null $token
    Write-Host "  Limit: $($limits.dailyLimit) | Used: $($limits.usedToday) | Remaining: $($limits.remaining)"
    if ($limits.dailyLimit -ne 50000) { throw "Unexpected daily limit" }

    Write-Host "==> Immediate transfer"
    $tx = Invoke-Api Post "http://localhost:8083/api/v1/transactions/transfer" @{
        fromAccountId = $acct1.id; toAccountNumber = $acct2.accountNumber; amount = 250.00; description = "Smoke transfer"
    } $token
    Write-Host "  Transfer ref: $($tx.reference) | status: $($tx.status)"

    Write-Host "==> Transaction history"
    $history = Invoke-Api Get "http://localhost:8083/api/v1/transactions" $null $token
    if ($history.Count -lt 1) { throw "Expected transaction history" }
    Write-Host "  History count: $($history.Count)"

    Write-Host "==> Schedule future transfer"
    $scheduledAt = (Get-Date).ToUniversalTime().AddSeconds(70).ToString("yyyy-MM-ddTHH:mm:ssZ")
    $scheduled = Invoke-Api Post "http://localhost:8083/api/v1/scheduled-transfers" @{
        fromAccountId = $acct1.id; toAccountNumber = $acct2.accountNumber; amount = 100.00
        description = "Scheduled smoke"; scheduledAt = $scheduledAt
    } $token
    Write-Host "  Scheduled id: $($scheduled.id) | at: $($scheduled.scheduledAt) | status: $($scheduled.status)"

    Write-Host "==> Wait for scheduled job (up to 130s)..."
    $finalStatus = "PENDING"
    for ($i = 0; $i -lt 26; $i++) {
        Start-Sleep -Seconds 5
        $list = Invoke-Api Get "http://localhost:8083/api/v1/scheduled-transfers" $null $token
        $item = $list | Where-Object { $_.id -eq $scheduled.id } | Select-Object -First 1
        $finalStatus = $item.status
        if ($finalStatus -eq "COMPLETED" -or $finalStatus -eq "FAILED") { break }
    }
    Write-Host "  Final scheduled status: $finalStatus"
    if ($finalStatus -ne "COMPLETED") { throw "Scheduled transfer did not complete (status: $finalStatus)" }

    Write-Host "==> Beneficiary add/list"
    Invoke-Api Post "http://localhost:8083/api/v1/beneficiaries" @{
        nickname = "Friend"; accountNumber = $acct2.accountNumber; accountHolderName = "Smoke User"
    } $token | Out-Null
    $beneficiaries = Invoke-Api Get "http://localhost:8083/api/v1/beneficiaries" $null $token
    Write-Host "  Beneficiaries: $($beneficiaries.Count)"

    Write-Host "`n=== ALL SMOKE TESTS PASSED ===`n"
}
finally {
    if ($startedAuth -and -not $startedAuth.HasExited) {
        Stop-Process -Id $startedAuth.Id -Force -ErrorAction SilentlyContinue
        Write-Host "==> Stopped auth-service (started by smoke test)"
    }
    if ($startedAccount -and -not $startedAccount.HasExited) {
        Stop-Process -Id $startedAccount.Id -Force -ErrorAction SilentlyContinue
        Write-Host "==> Stopped account-service (started by smoke test)"
    }
}

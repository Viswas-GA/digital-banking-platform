# Phase 7 smoke test — Kafka notifications end-to-end
$ErrorActionPreference = "Stop"

$env:Path = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin;$env:USERPROFILE\tools\apache-maven-3.9.16\bin;" + $env:Path
$root = Split-Path $PSScriptRoot -Parent

function Wait-ForHealth($url, $label, $maxSeconds = 90) {
    $deadline = (Get-Date).AddSeconds($maxSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $r = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 3
            if ($r.status -eq "UP") { Write-Host "  OK  $label"; return }
        } catch { Start-Sleep -Seconds 2 }
    }
    throw "$label not healthy at $url"
}

function Invoke-Api($Method, $Uri, $Body = $null, $Token = $null) {
    $headers = @{}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    if ($Body -ne $null) {
        $headers["Content-Type"] = "application/json"
        return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $headers -Body ($Body | ConvertTo-Json -Depth 5 -Compress)
    }
    return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $headers
}

Write-Host "`n=== Phase 7: Kafka Notification Smoke Test ===`n"

Write-Host "==> Start infrastructure"
docker compose -f "$root\docker-compose.yml" up -d postgres zookeeper kafka | Out-Null
Start-Sleep -Seconds 15
docker exec banking-postgres psql -U banking -d banking -c "CREATE SCHEMA IF NOT EXISTS notification;" | Out-Null

Write-Host "==> Build services"
Push-Location $root
mvn -B -pl auth-service,account-service,transaction-service,notification-service -am package -DskipTests -q
Pop-Location

$jars = @{
    auth = Get-ChildItem "$root\auth-service\target\auth-service-*.jar" | Where-Object { $_.Name -notlike "*.original" } | Select-Object -First 1
    account = Get-ChildItem "$root\account-service\target\account-service-*.jar" | Where-Object { $_.Name -notlike "*.original" } | Select-Object -First 1
    transaction = Get-ChildItem "$root\transaction-service\target\transaction-service-*.jar" | Where-Object { $_.Name -notlike "*.original" } | Select-Object -First 1
    notification = Get-ChildItem "$root\notification-service\target\notification-service-*.jar" | Where-Object { $_.Name -notlike "*.original" } | Select-Object -First 1
}

$procs = @()
try {
    foreach ($svc in $jars.Keys) {
        $procs += Start-Process -FilePath "java" -ArgumentList "-jar", $jars[$svc].FullName -PassThru -WindowStyle Hidden
    }

    Wait-ForHealth "http://localhost:8081/api/v1/auth/health" "auth-service"
    Wait-ForHealth "http://localhost:8082/api/v1/accounts/health" "account-service"
    Wait-ForHealth "http://localhost:8083/api/v1/transactions/health" "transaction-service"
    Wait-ForHealth "http://localhost:8084/api/v1/notifications/health" "notification-service"

    $suffix = [guid]::NewGuid().ToString("N").Substring(0,8)
    $userEmail = "kafka-user-$suffix@example.com"
    $adminEmail = "kafka-admin-$suffix@example.com"
    $password = "password123"

    Write-Host "`n==> Register + KYC approve"
    $userReg = Invoke-Api Post "http://localhost:8081/api/v1/auth/register" @{
        email=$userEmail; password=$password; firstName="Kafka"; lastName="User"; phone="9876543210"
    }
    Invoke-Api Post "http://localhost:8081/api/v1/auth/register" @{
        email=$adminEmail; password=$password; firstName="Kafka"; lastName="Admin"; phone="9876543211"
    } | Out-Null
    docker exec banking-postgres psql -U banking -d banking -c "UPDATE auth.users SET role = 'ADMIN' WHERE email = '$adminEmail';" | Out-Null

    Invoke-Api Post "http://localhost:8081/api/v1/kyc/submit" @{
        documentType="PASSPORT"; documentNumber="K$suffix"; dateOfBirth="1995-05-15"
        addressLine1="123 Main St"; city="Mumbai"; state="Maharashtra"; postalCode="400001"; country="India"
    } $userReg.accessToken | Out-Null

    $adminLogin = Invoke-Api Post "http://localhost:8081/api/v1/auth/login" @{ email=$adminEmail; password=$password }
    Invoke-Api Post "http://localhost:8081/api/v1/kyc/admin/$($userReg.user.id)/approve" $null $adminLogin.accessToken | Out-Null
    $login = Invoke-Api Post "http://localhost:8081/api/v1/auth/login" @{ email=$userEmail; password=$password }
    $token = $login.accessToken

    Write-Host "==> Create accounts + fund + transfer"
    $acct1 = Invoke-Api Post "http://localhost:8082/api/v1/accounts" @{ accountType="SAVINGS"; currency="INR" } $token
    $acct2 = Invoke-Api Post "http://localhost:8082/api/v1/accounts" @{ accountType="CURRENT"; currency="INR" } $token
    docker exec banking-postgres psql -U banking -d banking -c "UPDATE account.accounts SET balance = 5000.00 WHERE id = '$($acct1.id)';" | Out-Null
    Invoke-Api Post "http://localhost:8083/api/v1/transactions/transfer" @{
        fromAccountId=$acct1.id; toAccountNumber=$acct2.accountNumber; amount=150.00; description="Kafka smoke transfer"
    } $token | Out-Null

    Write-Host "==> Wait for Kafka notifications (up to 30s)"
    $expectedTypes = @("USER_REGISTERED", "KYC_APPROVED", "TRANSFER_COMPLETED")
    $found = @()
    for ($i = 0; $i -lt 15; $i++) {
        Start-Sleep -Seconds 2
        $notifications = Invoke-Api Get "http://localhost:8084/api/v1/notifications" $null $token
        $found = $notifications | ForEach-Object { $_.eventType } | Select-Object -Unique
        Write-Host "  poll $($i+1): found $($found -join ', ')"
        if (($expectedTypes | Where-Object { $found -contains $_ }).Count -eq $expectedTypes.Count) { break }
    }

    $unread = Invoke-Api Get "http://localhost:8084/api/v1/notifications/unread-count" $null $token
    Write-Host "  unread count: $($unread.unreadCount)"

    foreach ($type in $expectedTypes) {
        if ($found -notcontains $type) { throw "Missing notification type: $type" }
    }

    Write-Host "`n=== PHASE 7 SMOKE TEST PASSED ==="
    Write-Host "Notifications received: $($found -join ', ')`n"
}
finally {
    foreach ($p in $procs) {
        if ($p -and -not $p.HasExited) { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue }
    }
}

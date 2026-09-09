# API Gateway smoke test — all client traffic via port 8080
$ErrorActionPreference = "Stop"

$env:Path = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin;$env:USERPROFILE\tools\apache-maven-3.9.16\bin;" + $env:Path
$root = Split-Path $PSScriptRoot -Parent
$gateway = "http://localhost:8080"

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

Write-Host "`n=== Phase 9: API Gateway Smoke Test ===`n"

Write-Host "==> Infrastructure"
docker compose -f "$root\docker-compose.yml" up -d postgres | Out-Null
Start-Sleep -Seconds 5

Write-Host "==> Build"
Push-Location $root
mvn -B -pl api-gateway,auth-service,account-service,transaction-service,notification-service -am package -DskipTests -q
Pop-Location

$jars = @{
    auth         = Get-ChildItem "$root\auth-service\target\auth-service-*.jar" | Where-Object { $_.Name -notlike "*.original" } | Select-Object -First 1
    account      = Get-ChildItem "$root\account-service\target\account-service-*.jar" | Where-Object { $_.Name -notlike "*.original" } | Select-Object -First 1
    transaction  = Get-ChildItem "$root\transaction-service\target\transaction-service-*.jar" | Where-Object { $_.Name -notlike "*.original" } | Select-Object -First 1
    notification = Get-ChildItem "$root\notification-service\target\notification-service-*.jar" | Where-Object { $_.Name -notlike "*.original" } | Select-Object -First 1
    gateway      = Get-ChildItem "$root\api-gateway\target\api-gateway-*.jar" | Where-Object { $_.Name -notlike "*.original" } | Select-Object -First 1
}

$procs = @()
try {
    foreach ($key in @("auth", "account", "transaction", "notification", "gateway")) {
        $procs += Start-Process -FilePath "java" -ArgumentList "-jar", $jars[$key].FullName -PassThru -WindowStyle Hidden
    }

    Wait-ForHealth "$gateway/api/v1/gateway/health" "api-gateway"
    Wait-ForHealth "http://localhost:8081/api/v1/auth/health" "auth-service (direct)"
    Wait-ForHealth "http://localhost:8082/api/v1/accounts/health" "account-service (direct)"

    Write-Host "`n==> Gateway routes auth health"
    $authHealth = Invoke-RestMethod "$gateway/api/v1/auth/health"
    if ($authHealth.status -ne "UP") { throw "Auth health via gateway failed" }

    $suffix = [guid]::NewGuid().ToString("N").Substring(0,8)
    $userEmail = "gw-user-$suffix@example.com"
    $adminEmail = "gw-admin-$suffix@example.com"
    $password = "password123"

    Write-Host "==> Register via gateway"
    $userReg = Invoke-Api Post "$gateway/api/v1/auth/register" @{
        email=$userEmail; password=$password; firstName="Gateway"; lastName="User"; phone="9876543210"
    }
    Invoke-Api Post "$gateway/api/v1/auth/register" @{
        email=$adminEmail; password=$password; firstName="Gateway"; lastName="Admin"; phone="9876543211"
    } | Out-Null
    docker exec banking-postgres psql -U banking -d banking -c "UPDATE auth.users SET role = 'ADMIN' WHERE email = '$adminEmail';" | Out-Null

    Write-Host "==> KYC via gateway"
    Invoke-Api Post "$gateway/api/v1/kyc/submit" @{
        documentType="PASSPORT"; documentNumber="G$suffix"; dateOfBirth="1995-05-15"
        addressLine1="123 Main St"; city="Mumbai"; state="Maharashtra"; postalCode="400001"; country="India"
    } $userReg.accessToken | Out-Null
    $adminLogin = Invoke-Api Post "$gateway/api/v1/auth/login" @{ email=$adminEmail; password=$password }
    Invoke-Api Post "$gateway/api/v1/kyc/admin/$($userReg.user.id)/approve" $null $adminLogin.accessToken | Out-Null
    $login = Invoke-Api Post "$gateway/api/v1/auth/login" @{ email=$userEmail; password=$password }
    $token = $login.accessToken

    Write-Host "==> Accounts + transfer via gateway"
    $acct1 = Invoke-Api Post "$gateway/api/v1/accounts" @{ accountType="SAVINGS"; currency="INR" } $token
    $acct2 = Invoke-Api Post "$gateway/api/v1/accounts" @{ accountType="CURRENT"; currency="INR" } $token
    docker exec banking-postgres psql -U banking -d banking -c "UPDATE account.accounts SET balance = 5000.00 WHERE id = '$($acct1.id)';" | Out-Null
    $tx = Invoke-Api Post "$gateway/api/v1/transactions/transfer" @{
        fromAccountId=$acct1.id; toAccountNumber=$acct2.accountNumber; amount=100.00; description="Gateway transfer"
    } $token
    Write-Host "  transfer: $($tx.reference)"

    Write-Host "==> Notifications via gateway"
    Start-Sleep -Seconds 3
    $notifications = Invoke-Api Get "$gateway/api/v1/notifications" $null $token
    Write-Host "  notifications: $($notifications.Count)"

    Write-Host "`n=== PHASE 9 GATEWAY SMOKE TEST PASSED ===`n"
}
finally {
    foreach ($p in $procs) {
        if ($p -and -not $p.HasExited) { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue }
    }
}

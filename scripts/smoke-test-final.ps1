# Final smoke test — gateway + audit logs via Kafka
$ErrorActionPreference = "Stop"

$env:Path = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin;$env:USERPROFILE\tools\apache-maven-3.9.16\bin;" + $env:Path
$root = Split-Path $PSScriptRoot -Parent
$gw = "http://localhost:8080"

function Wait-ForHealth($url, $label, $maxSeconds = 120) {
    $deadline = (Get-Date).AddSeconds($maxSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $r = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 3
            if ($r.status -eq "UP") { Write-Host "  OK  $label"; return }
        } catch { Start-Sleep -Seconds 2 }
    }
    throw "$label not healthy"
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

Write-Host "`n=== Final Smoke Test (Gateway + Audit) ===`n"

docker compose -f "$root\docker-compose.yml" up -d postgres zookeeper kafka | Out-Null
Start-Sleep -Seconds 15
docker exec banking-postgres psql -U banking -d banking -c "CREATE SCHEMA IF NOT EXISTS audit;" | Out-Null

Push-Location $root
mvn -B -pl auth-service,account-service,transaction-service,notification-service,audit-service,api-gateway -am package -DskipTests -q
Pop-Location

$jars = @("auth-service","account-service","transaction-service","notification-service","audit-service","api-gateway") | ForEach-Object {
    @{ Name = $_; Jar = (Get-ChildItem "$root\$_\target\*-*.jar" | Where-Object { $_.Name -notlike "*.original" } | Select-Object -First 1) }
}

$procs = @()
try {
    foreach ($item in $jars) {
        $procs += Start-Process -FilePath "java" -ArgumentList "-jar", $item.Jar.FullName -PassThru -WindowStyle Hidden
    }

    Wait-ForHealth "$gw/api/v1/gateway/health" "api-gateway"
    Wait-ForHealth "$gw/api/v1/admin/audit-logs/health" "audit-service"

    $suffix = [guid]::NewGuid().ToString("N").Substring(0,8)
    $userEmail = "final-user-$suffix@example.com"
    $adminEmail = "final-admin-$suffix@example.com"
    $password = "password123"

    $userReg = Invoke-Api Post "$gw/api/v1/auth/register" @{
        email=$userEmail; password=$password; firstName="Final"; lastName="User"; phone="9876543210"
    }
    Invoke-Api Post "$gw/api/v1/auth/register" @{
        email=$adminEmail; password=$password; firstName="Final"; lastName="Admin"; phone="9876543211"
    } | Out-Null
    docker exec banking-postgres psql -U banking -d banking -c "UPDATE auth.users SET role = 'ADMIN' WHERE email = '$adminEmail';" | Out-Null

    Invoke-Api Post "$gw/api/v1/kyc/submit" @{
        documentType="PASSPORT"; documentNumber="F$suffix"; dateOfBirth="1995-05-15"
        addressLine1="123 Main St"; city="Mumbai"; state="Maharashtra"; postalCode="400001"; country="India"
    } $userReg.accessToken | Out-Null

    $adminLogin = Invoke-Api Post "$gw/api/v1/auth/login" @{ email=$adminEmail; password=$password }
    Invoke-Api Post "$gw/api/v1/kyc/admin/$($userReg.user.id)/approve" $null $adminLogin.accessToken | Out-Null
    $login = Invoke-Api Post "$gw/api/v1/auth/login" @{ email=$userEmail; password=$password }
    $token = $login.accessToken

    $acct1 = Invoke-Api Post "$gw/api/v1/accounts" @{ accountType="SAVINGS"; currency="INR" } $token
    $acct2 = Invoke-Api Post "$gw/api/v1/accounts" @{ accountType="CURRENT"; currency="INR" } $token
    docker exec banking-postgres psql -U banking -d banking -c "UPDATE account.accounts SET balance = 5000.00 WHERE id = '$($acct1.id)';" | Out-Null
    Invoke-Api Post "$gw/api/v1/transactions/transfer" @{
        fromAccountId=$acct1.id; toAccountNumber=$acct2.accountNumber; amount=75.00; description="Final smoke"
    } $token | Out-Null

    Write-Host "==> Wait for audit logs (up to 30s)"
    $expected = @("USER_REGISTERED","KYC_SUBMITTED","KYC_APPROVED","TRANSFER_COMPLETED")
    $found = @()
    for ($i = 0; $i -lt 15; $i++) {
        Start-Sleep -Seconds 2
        $page = Invoke-Api Get "$gw/api/v1/admin/audit-logs?page=0&size=50" $null $adminLogin.accessToken
        $found = $page.content | ForEach-Object { $_.action } | Select-Object -Unique
        Write-Host "  poll $($i+1): $($found -join ', ')"
        if (($expected | Where-Object { $found -contains $_ }).Count -eq $expected.Count) { break }
    }

    foreach ($action in $expected) {
        if ($found -notcontains $action) { throw "Missing audit action: $action" }
    }

    Write-Host "`n=== PROJECT COMPLETE - ALL SMOKE TESTS PASSED ==="
    Write-Host "Audit actions logged: $($found -join ', ')"
    Write-Host ""
}
finally {
    foreach ($p in $procs) {
        if ($p -and -not $p.HasExited) { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue }
    }
}

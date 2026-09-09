# Auth service smoke test (requires PostgreSQL on localhost:5432)
$ErrorActionPreference = "Stop"

Write-Host "==> Checking PostgreSQL..."
docker exec banking-postgres pg_isready -U banking -d banking 2>$null
if ($LASTEXITCODE -ne 0) {
    docker compose up -d postgres
    Start-Sleep -Seconds 8
}

Write-Host "==> Building auth-service..."
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:MAVEN_HOME = "$env:USERPROFILE\tools\apache-maven-3.9.16"
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;" + [Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [Environment]::GetEnvironmentVariable("Path", "User")
mvn -B -pl auth-service -am package -DskipTests

Write-Host "==> Starting auth-service..."
$jar = Get-ChildItem "auth-service\target\auth-service-*.jar" | Select-Object -First 1
$process = Start-Process -FilePath "java" -ArgumentList "-jar", $jar.FullName -PassThru -NoNewWindow
Start-Sleep -Seconds 15

try {
    Write-Host "==> Health check"
    $health = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/health" -Method Get
    Write-Host ($health | ConvertTo-Json -Compress)

    $email = "smoke-$([guid]::NewGuid().ToString('N').Substring(0,8))@example.com"
    $registerBody = @{
        email = $email
        password = "password123"
        firstName = "Smoke"
        lastName = "Test"
        phone = "1234567890"
    } | ConvertTo-Json

    Write-Host "==> Register user $email"
    $register = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/register" -Method Post -Body $registerBody -ContentType "application/json"
    Write-Host "Token received: $($register.accessToken.Substring(0, 20))..."

    Write-Host "==> GET /me with JWT"
    $headers = @{ Authorization = "Bearer $($register.accessToken)" }
    $me = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/me" -Method Get -Headers $headers
    Write-Host ($me | ConvertTo-Json -Compress)

    Write-Host "==> Login"
    $loginBody = @{ email = $email; password = "password123" } | ConvertTo-Json
    $login = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    Write-Host "Login token received: $($login.accessToken.Substring(0, 20))..."

    Write-Host "SMOKE TEST PASSED"
}
finally {
    if ($process -and -not $process.HasExited) {
        Stop-Process -Id $process.Id -Force
        Write-Host "==> Stopped auth-service"
    }
}

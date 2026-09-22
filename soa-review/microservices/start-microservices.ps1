$ErrorActionPreference = 'Stop'
$base = 'C:\Users\mekas\Downloads\soa-review\soa-review\microservices'
$logs = Join-Path $base 'live-logs'
New-Item -ItemType Directory -Force -Path $logs | Out-Null
Get-NetTCPConnection -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -in 8761,8080,8081,8082,8083,8084,8085 } | ForEach-Object { try { Stop-Process -Id $_.OwningProcess -Force -ErrorAction Stop } catch {} }

$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:DB_URL = 'jdbc:postgresql://localhost:5432/talentsync'
$env:DB_USERNAME = 'postgres'
$env:DB_PASSWORD = 'Srk@1207'
$env:MAIL_HOST = 'smtp.gmail.com'
$env:MAIL_PORT = '587'
$env:MAIL_USERNAME = 'sairajakrishna2007@gmail.com'
$env:MAIL_PASSWORD = 'wgamcqogemhztimt'
$env:MAIL_FROM = 'sairajakrishna2007@gmail.com'
$env:MAIL_SMTP_AUTH = 'true'
$env:MAIL_STARTTLS_ENABLE = 'true'
$env:MAIL_STARTTLS_REQUIRED = 'true'
$env:MAIL_SMTP_SSL_TRUST = 'smtp.gmail.com'
$env:JWT_SECRET = 'talent-sync-jwt-secret-key-change-me-2026'

Set-Location $base
& 'C:\Users\mekas\Downloads\soa-review\maven\apache-maven-3.9.11\bin\mvn.cmd' -q package -DskipTests

Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" -ArgumentList '-jar', '.\eureka-server\target\eureka-server-1.0.0.jar' -WorkingDirectory $base -RedirectStandardOutput "$logs\eureka.log" -RedirectStandardError "$logs\eureka.err" -WindowStyle Hidden
Start-Sleep -Seconds 10
Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" -ArgumentList '-jar', '.\auth-service\target\auth-service-1.0.0.jar' -WorkingDirectory $base -RedirectStandardOutput "$logs\auth.log" -RedirectStandardError "$logs\auth.err" -WindowStyle Hidden
Start-Sleep -Seconds 10
Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" -ArgumentList '-jar', '.\job-service\target\job-service-1.0.0.jar' -WorkingDirectory $base -RedirectStandardOutput "$logs\job.log" -RedirectStandardError "$logs\job.err" -WindowStyle Hidden
Start-Sleep -Seconds 10
Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" -ArgumentList '-jar', '.\application-service\target\application-service-1.0.0.jar' -WorkingDirectory $base -RedirectStandardOutput "$logs\application.log" -RedirectStandardError "$logs\application.err" -WindowStyle Hidden
Start-Sleep -Seconds 10
Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" -ArgumentList '-jar', '.\gateway\target\gateway-1.0.0.jar' -WorkingDirectory $base -RedirectStandardOutput "$logs\gateway.log" -RedirectStandardError "$logs\gateway.err" -WindowStyle Hidden

Write-Host 'Microservice startup launched successfully.'
Write-Host "Eureka: http://localhost:8761"
Write-Host "Gateway health: http://localhost:8080/actuator/health"
Write-Host "Auth health: http://localhost:8081/api/auth/health"

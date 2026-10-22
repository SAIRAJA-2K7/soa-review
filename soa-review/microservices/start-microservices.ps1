$ErrorActionPreference = 'Stop'
$base = $PSScriptRoot
$repoRoot = Split-Path -Parent $base
$workspaceRoot = Split-Path -Parent $repoRoot
$logs = Join-Path $base 'live-logs'
New-Item -ItemType Directory -Force -Path $logs | Out-Null

$envFile = Join-Path $base '.env'
if (Test-Path $envFile) {
	foreach ($line in Get-Content $envFile) {
		$entry = $line.Trim()
		if (-not $entry -or $entry.StartsWith('#') -or -not $entry.Contains('=')) { continue }
		$separator = $entry.IndexOf('=')
		$name = $entry.Substring(0, $separator).Trim()
		$value = $entry.Substring($separator + 1).Trim()
		if ($name -match '^[A-Za-z_][A-Za-z0-9_]*$' -and [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name, 'Process'))) {
			if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
				$value = $value.Substring(1, $value.Length - 2)
			}
			[Environment]::SetEnvironmentVariable($name, $value, 'Process')
		}
	}
}

function Resolve-Java21Home {
	$candidates = @()
	if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
	$candidates += 'C:\Program Files\Java\jdk-21'
	foreach ($dir in @('C:\Program Files\Java', 'C:\Program Files\Eclipse Adoptium')) {
		if (-not (Test-Path $dir)) { continue }
		$candidates += (Get-ChildItem $dir -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -match 'jdk-21|jdk-21\.' } | Select-Object -ExpandProperty FullName)
	}
	$seen = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
	foreach ($candidate in $candidates) {
		if ([string]::IsNullOrWhiteSpace($candidate)) { continue }
		$trimmed = $candidate.TrimEnd('\')
		if ((Split-Path -Leaf $trimmed) -ieq 'bin') { $trimmed = Split-Path -Parent $trimmed }
		if ($seen.Add($trimmed)) {
			$java = Join-Path $trimmed 'bin\java.exe'
			if (Test-Path $java) {
				$javaVersion = & $java -version 2>&1 | Out-String
				if ($LASTEXITCODE -eq 0 -and $javaVersion -match 'version "21(?:\.|")') {
					return $trimmed
				}
			}
		}
	}
	return $null
}

$resolvedJavaHome = Resolve-Java21Home
if (-not $resolvedJavaHome) {
	throw 'The microservices require Java 21. Install or point JAVA_HOME at a Java 21 JDK.'
}
$env:JAVA_HOME = $resolvedJavaHome
if (-not $env:DB_URL) { $env:DB_URL = 'jdbc:postgresql://localhost:5432/talentsync' }
if (-not $env:DB_USERNAME) { $env:DB_USERNAME = 'postgres' }
$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
if (-not (Test-Path $java)) {
	throw 'Set JAVA_HOME to an installed Java 21 JDK.'
}
if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
	throw 'Set DB_PASSWORD in the process environment or microservices/.env.'
}
if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET) -or $env:JWT_SECRET.Length -lt 32) {
	throw 'Set JWT_SECRET to at least 32 characters in the process environment or microservices/.env.'
}
if ([string]::IsNullOrWhiteSpace($env:JOB_SYNC_SECRET) -or $env:JOB_SYNC_SECRET.Length -lt 32) {
	throw 'Set JOB_SYNC_SECRET to at least 32 characters in the process environment or microservices/.env.'
}

$maven = Join-Path $workspaceRoot 'maven\apache-maven-3.9.11\bin\mvn.cmd'
if (-not (Test-Path $maven)) {
	$mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
	if (-not $mavenCommand) { throw 'Maven 3.9.11 is not available in the workspace or PATH.' }
	$maven = $mavenCommand.Source
}

function Assert-PortsAvailable([int[]]$Ports) {
	$listeners = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
		Where-Object { $_.LocalPort -in $Ports }
	if ($listeners) {
		$occupied = $listeners | ForEach-Object { "$($_.LocalPort) (PID $($_.OwningProcess))" } | Sort-Object -Unique
		throw "Required ports are already in use: $($occupied -join ', '). Stop only the intended processes, then retry."
	}
}

function Start-ServiceProcess([string]$JarPath, [string]$StdOut, [string]$StdErr) {
	$process = Start-Process -FilePath $java -ArgumentList '-jar', $JarPath -WorkingDirectory $base `
		-RedirectStandardOutput $StdOut -RedirectStandardError $StdErr -WindowStyle Hidden -PassThru
	$startedProcesses.Add($process)
	return $process
}

function Wait-ForEndpoint([string]$Uri, [string]$ServiceName, [System.Diagnostics.Process]$Process) {
	for ($attempt = 0; $attempt -lt 60; $attempt++) {
		$Process.Refresh()
		if ($Process.HasExited) {
			throw "$ServiceName exited before becoming healthy. Check logs under $logs."
		}
		try {
			$response = Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec 2
			if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) { return }
		} catch {}
		Start-Sleep -Seconds 2
	}
	throw "$ServiceName did not become healthy at $Uri. Check logs under $logs."
}

Set-Location $base
Assert-PortsAvailable @(8761, 8080, 8081, 8083, 8084)
& $maven -q package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Maven package failed with exit code $LASTEXITCODE." }

$startedProcesses = [System.Collections.Generic.List[System.Diagnostics.Process]]::new()
try {
	$process = Start-ServiceProcess '.\eureka-server\target\eureka-server-1.0.0.jar' "$logs\eureka.log" "$logs\eureka.err"
	Wait-ForEndpoint 'http://localhost:8761' 'Eureka' $process
	$process = Start-ServiceProcess '.\auth-service\target\auth-service-1.0.0.jar' "$logs\auth.log" "$logs\auth.err"
	Wait-ForEndpoint 'http://localhost:8081/api/auth/health' 'Auth service' $process
	$process = Start-ServiceProcess '.\job-service\target\job-service-1.0.0.jar' "$logs\job.log" "$logs\job.err"
	Wait-ForEndpoint 'http://localhost:8083/actuator/health' 'Job service' $process
	$process = Start-ServiceProcess '.\application-service\target\application-service-1.0.0.jar' "$logs\application.log" "$logs\application.err"
	Wait-ForEndpoint 'http://localhost:8084/actuator/health' 'Application service' $process
	$process = Start-ServiceProcess '.\gateway\target\gateway-1.0.0.jar' "$logs\gateway.log" "$logs\gateway.err"
	Wait-ForEndpoint 'http://localhost:8080/actuator/health' 'Gateway' $process
} catch {
	foreach ($startedProcess in $startedProcesses) {
		$startedProcess.Refresh()
		if (-not $startedProcess.HasExited) {
			Stop-Process -Id $startedProcess.Id -Force -ErrorAction SilentlyContinue
		}
	}
	throw
}

Write-Host 'Microservice startup launched successfully.'
Write-Host "Eureka: http://localhost:8761"
Write-Host "Gateway health: http://localhost:8080/actuator/health"
Write-Host "Auth health: http://localhost:8081/api/auth/health"

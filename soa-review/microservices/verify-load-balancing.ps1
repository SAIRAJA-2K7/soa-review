param(
	[Uri]$GatewayUrl = 'http://localhost:8080',
	[Uri]$EurekaUrl = 'http://localhost:8761',
	[string]$ServiceId = 'JOB-SERVICE',
	[string]$RequestPath = '/api/jobs',
	[string]$BearerToken,
	[int]$RequestCount = 50,
	[int]$ExpectedInstanceCount = 1,
	[switch]$RequireExactInstanceCount,
	[switch]$RequireDistribution
)

$ErrorActionPreference = 'Stop'
if ($RequestCount -lt 1) { throw 'RequestCount must be at least 1.' }
if ($ExpectedInstanceCount -lt 1) { throw 'ExpectedInstanceCount must be at least 1.' }

$registryUri = "$($EurekaUrl.ToString().TrimEnd('/'))/eureka/apps/$ServiceId"
$registry = Invoke-RestMethod -Uri $registryUri -Headers @{ Accept = 'application/json' }
$instances = @($registry.application.instance | Where-Object status -eq 'UP')
$activeInstances = $instances | ForEach-Object {
	$port = $_.port.'$'
	[pscustomobject]@{
		InstanceId = $_.instanceId
		Address = "$($_.hostName):$port"
	}
}

Write-Host "Eureka $ServiceId instances currently UP: $($activeInstances.Count)"
$activeInstances | Format-Table -AutoSize
if ($activeInstances.Count -lt $ExpectedInstanceCount) {
	throw "Expected at least $ExpectedInstanceCount UP $ServiceId instances; found $($activeInstances.Count)."
}
if ($RequireExactInstanceCount -and $activeInstances.Count -ne $ExpectedInstanceCount) {
	throw "Expected exactly $ExpectedInstanceCount UP $ServiceId instances; found $($activeInstances.Count)."
}

$headers = @{}
if (-not [string]::IsNullOrWhiteSpace($BearerToken)) {
	$headers.Authorization = "Bearer $BearerToken"
}
$requestUri = [Uri]::new($GatewayUrl, $RequestPath)
$client = [System.Net.Http.HttpClient]::new()
$client.Timeout = [TimeSpan]::FromSeconds(10)
$samples = [System.Collections.Generic.List[object]]::new()
$clock = [System.Diagnostics.Stopwatch]::StartNew()

try {
	for ($index = 1; $index -le $RequestCount; $index++) {
		$requestClock = [System.Diagnostics.Stopwatch]::StartNew()
		$status = 'ERROR'
		$instance = 'not reported'
		$request = $null
		$response = $null
		try {
			$request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Get, $requestUri)
			foreach ($header in $headers.GetEnumerator()) {
				[void]$request.Headers.TryAddWithoutValidation($header.Key, $header.Value)
			}
			$response = $client.SendAsync($request).GetAwaiter().GetResult()
			$status = [int]$response.StatusCode
			$instanceValues = [System.Collections.Generic.IEnumerable[string]]$null
			if ($response.Headers.TryGetValues('X-Service-Instance', [ref]$instanceValues)) {
				$instance = $instanceValues -join ','
			}
		} catch {
			$status = 'ERROR'
			Write-Warning "Request $index failed: $($_.Exception.Message)"
		} finally {
			if ($response) { $response.Dispose() }
			if ($request) { $request.Dispose() }
		}
		$requestClock.Stop()
		$samples.Add([pscustomobject]@{
			Request = $index
			Status = $status
			Instance = $instance
			ElapsedMs = [math]::Round($requestClock.Elapsed.TotalMilliseconds, 2)
		})
	}
} finally {
	$clock.Stop()
	$client.Dispose()
}

$successful = @($samples | Where-Object { $_.Status -is [int] -and $_.Status -ge 200 -and $_.Status -lt 300 })
$latencies = @($successful | ForEach-Object ElapsedMs | Sort-Object)
if ($latencies.Count -gt 0) {
	$p95Index = [math]::Min($latencies.Count - 1, [math]::Ceiling($latencies.Count * 0.95) - 1)
	$middle = [int][math]::Floor($latencies.Count / 2)
	$median = if ($latencies.Count % 2 -eq 0) {
		($latencies[$middle - 1] + $latencies[$middle]) / 2
	} else {
		$latencies[$middle]
	}
	$summary = [pscustomobject]@{
		Requests = $RequestCount
		Successful = $successful.Count
		Errors = $RequestCount - $successful.Count
		ElapsedSeconds = [math]::Round($clock.Elapsed.TotalSeconds, 3)
		RequestsPerSecond = [math]::Round($successful.Count / $clock.Elapsed.TotalSeconds, 2)
		MeanLatencyMs = [math]::Round(($latencies | Measure-Object -Average).Average, 2)
		MedianLatencyMs = [math]::Round($median, 2)
		P95LatencyMs = $latencies[$p95Index]
	}
	$summary | Format-List
	$samples | Format-Table Request, Status, Instance, ElapsedMs -AutoSize
	$samples | Group-Object Instance, Status | Select-Object @{Name = 'Instance / Status'; Expression = { $_.Name }}, Count | Format-Table -AutoSize
} else {
	$samples | Format-Table -AutoSize
}

$observedInstances = @($successful | Where-Object Instance -ne 'not reported' | Select-Object -ExpandProperty Instance -Unique)
if ($RequireDistribution -and $observedInstances.Count -lt $ExpectedInstanceCount) {
	throw "Observed responses from $($observedInstances.Count) distinct instances; expected at least $ExpectedInstanceCount. Confirm the opt-in X-Service-Instance header is enabled."
}
if ($successful.Count -ne $RequestCount) {
	throw "Only $($successful.Count) of $RequestCount requests succeeded. See the per-request status output above."
}
if ($RequireDistribution) {
	Write-Host "Observed backend distribution:"
	$successful | Group-Object Instance | Select-Object @{Name = 'Instance'; Expression = { $_.Name }}, Count | Format-Table -AutoSize
} elseif ($observedInstances.Count -eq 0) {
	Write-Host 'No X-Service-Instance response header observed; enable the opt-in diagnostic header to measure distribution.'
}

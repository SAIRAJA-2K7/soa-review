param(
    [Uri]$GatewayUrl = 'http://127.0.0.1:8080',
    [Uri]$EurekaUrl = 'http://127.0.0.1:8761',
    [int]$ExpectedInstanceCount = 2,
    [int]$JobCount = 10,
    [switch]$AllowLocalTestWrites
)

$ErrorActionPreference = 'Stop'

function Assert-LoopbackUrl([Uri]$Url, [string]$Name) {
    if ($Url.Scheme -ne 'http' -or $Url.Host -notin @('localhost', '127.0.0.1', '::1')) {
        throw "$Name must use HTTP on localhost or a loopback address. This verifier is restricted to local test stacks."
    }
}

if (-not $AllowLocalTestWrites) {
    throw 'This verifier creates, updates, and deletes test jobs. Re-run with -AllowLocalTestWrites only against a disposable local test database.'
}
Assert-LoopbackUrl $GatewayUrl 'GatewayUrl'
Assert-LoopbackUrl $EurekaUrl 'EurekaUrl'
if ($ExpectedInstanceCount -lt 2) { throw 'ExpectedInstanceCount must be at least 2.' }
if ($JobCount -lt $ExpectedInstanceCount) { throw 'JobCount must be at least ExpectedInstanceCount.' }
if ([string]::IsNullOrWhiteSpace($env:TALENTSYNC_TEST_RECRUITER_TOKEN)) {
    throw 'Set TALENTSYNC_TEST_RECRUITER_TOKEN to a verified recruiter JWT. The token is not printed.'
}

$registryUri = "$($EurekaUrl.ToString().TrimEnd('/'))/eureka/apps/JOB-SERVICE"
$registry = Invoke-RestMethod -Uri $registryUri -Headers @{ Accept = 'application/json' }
$activeInstances = @($registry.application.instance | Where-Object status -eq 'UP')
if ($activeInstances.Count -ne $ExpectedInstanceCount) {
    throw "Expected exactly $ExpectedInstanceCount UP JOB-SERVICE instances; found $($activeInstances.Count)."
}

$client = [System.Net.Http.HttpClient]::new()
$client.Timeout = [TimeSpan]::FromSeconds(30)
$operations = [System.Collections.Generic.List[object]]::new()
$createdJobs = [System.Collections.Generic.List[object]]::new()
$baseUri = $GatewayUrl.ToString().TrimEnd('/')
$runId = [Guid]::NewGuid().ToString('N')

function Invoke-GatewayRequest {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body,
        [switch]$DoNotTrack
    )

    $request = [System.Net.Http.HttpRequestMessage]::new(
        [System.Net.Http.HttpMethod]::new($Method),
        "$baseUri$Path"
    )
    [void]$request.Headers.TryAddWithoutValidation(
        'Authorization',
        "Bearer $env:TALENTSYNC_TEST_RECRUITER_TOKEN"
    )
    if ($null -ne $Body) {
        $json = ConvertTo-Json -InputObject $Body -Depth 10 -Compress
        $request.Content = [System.Net.Http.StringContent]::new(
            $json,
            [System.Text.Encoding]::UTF8,
            'application/json'
        )
    }

    $response = $null
    try {
        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        $responseText = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        $responseBody = $null
        if (-not [string]::IsNullOrWhiteSpace($responseText)) {
            try {
                $responseBody = ConvertFrom-Json -InputObject $responseText -ErrorAction Stop
            } catch {
                $responseBody = $responseText
            }
        }

        $instance = 'not reported'
        $instanceValues = [System.Collections.Generic.IEnumerable[string]]$null
        if ($response.Headers.TryGetValues('X-Service-Instance', [ref]$instanceValues)) {
            $instance = $instanceValues -join ','
        }
        $result = [pscustomobject]@{
            Method = $Method
            Path = $Path
            Status = [int]$response.StatusCode
            Instance = $instance
            Body = $responseBody
        }
        if (-not $DoNotTrack) { $operations.Add($result) }
        return $result
    } finally {
        if ($response) { $response.Dispose() }
        $request.Dispose()
    }
}

function Assert-ResponseStatus($Response, [int[]]$ExpectedStatus) {
    if ($Response.Status -notin $ExpectedStatus) {
        throw "$($Response.Method) $($Response.Path) returned HTTP $($Response.Status); expected $($ExpectedStatus -join ', ')."
    }
}

function Get-JobId($Response) {
    $id = $Response.Body.id
    if ($null -eq $id -or [string]::IsNullOrWhiteSpace([string]$id)) {
        throw "POST $($Response.Path) succeeded without returning a job id."
    }
    return [string]$id
}

function Assert-MirroredJob([string]$Id, [bool]$ShouldExist) {
    $response = Invoke-GatewayRequest -Method 'GET' -Path '/api/admin/jobs' -DoNotTrack
    Assert-ResponseStatus $response @(200)
    $matches = @($response.Body | Where-Object { [string]$_.id -eq $Id })
    if ($ShouldExist -and $matches.Count -ne 1) {
        throw "The application-service job mirror did not contain job $Id after synchronization."
    }
    if (-not $ShouldExist -and $matches.Count -ne 0) {
        throw "The application-service job mirror still contained deleted job $Id."
    }
}

try {
    for ($index = 1; $index -le $JobCount; $index++) {
        $title = "Load balance verification $runId $index"
        $payload = @{
            title = $title
            companyName = 'TalentSync Verification'
            location = 'Remote'
            type = 'REMOTE'
            experience = 'Test'
            description = 'Temporary job created by the local CRUD load-balancing verifier.'
            skills = @('verification')
            responsibilities = @()
            requirements = @()
            salaryMin = 1
            salaryMax = 2
        }

        $created = Invoke-GatewayRequest -Method 'POST' -Path '/api/jobs' -Body $payload
        Assert-ResponseStatus $created @(200, 201)
        $jobId = Get-JobId $created
        $createdJobs.Add([pscustomobject]@{ Id = $jobId; Deleted = $false })
        Assert-MirroredJob $jobId $true

        $read = Invoke-GatewayRequest -Method 'GET' -Path "/api/jobs/$jobId"
        Assert-ResponseStatus $read @(200)
        if ($read.Body.title -ne $title) { throw "Created job $jobId did not read back with its original title." }

        $updatedPayload = $payload.Clone()
        $updatedPayload.title = "$title updated"
        $updated = Invoke-GatewayRequest -Method 'PUT' -Path "/api/jobs/$jobId" -Body $updatedPayload
        Assert-ResponseStatus $updated @(200)

        $readUpdated = Invoke-GatewayRequest -Method 'GET' -Path "/api/jobs/$jobId"
        Assert-ResponseStatus $readUpdated @(200)
        if ($readUpdated.Body.title -ne "$title updated") { throw "Updated job $jobId did not persist its new title." }

        $unpublish = Invoke-GatewayRequest -Method 'PATCH' -Path "/api/jobs/$jobId/published" -Body @{ published = $false }
        Assert-ResponseStatus $unpublish @(200)
        if ($unpublish.Body.published -ne $false) { throw "Job $jobId did not persist its unpublished state." }

        $republish = Invoke-GatewayRequest -Method 'PATCH' -Path "/api/jobs/$jobId/published" -Body @{ published = $true }
        Assert-ResponseStatus $republish @(200)
        if ($republish.Body.published -ne $true) { throw "Job $jobId did not persist its republished state." }

        $deleted = Invoke-GatewayRequest -Method 'DELETE' -Path "/api/jobs/$jobId"
        Assert-ResponseStatus $deleted @(200, 204)
        $createdJobs[$createdJobs.Count - 1].Deleted = $true
        $readDeleted = Invoke-GatewayRequest -Method 'GET' -Path "/api/jobs/$jobId"
        Assert-ResponseStatus $readDeleted @(404)
        Assert-MirroredJob $jobId $false
    }
} finally {
    foreach ($job in $createdJobs | Where-Object { -not $_.Deleted }) {
        try {
            $cleanup = Invoke-GatewayRequest -Method 'DELETE' -Path "/api/jobs/$($job.Id)" -DoNotTrack
            if ($cleanup.Status -notin @(200, 204, 404)) {
                Write-Warning "Cleanup for temporary job $($job.Id) returned HTTP $($cleanup.Status)."
            }
        } catch {
            Write-Warning "Could not clean up temporary job $($job.Id); inspect the disposable test database."
        }
    }
    $client.Dispose()
}

$unreported = @($operations | Where-Object Instance -eq 'not reported')
if ($unreported.Count -gt 0) {
    throw 'X-Service-Instance was missing. Enable LOAD_BALANCING_INSTANCE_HEADER_ENABLED=true on all job-service instances.'
}

$distribution = @($operations | Group-Object Method | ForEach-Object {
    $instanceCount = @($_.Group | Select-Object -ExpandProperty Instance -Unique).Count
    [pscustomobject]@{
        Method = $_.Name
        Requests = $_.Count
        DistinctInstances = $instanceCount
    }
})
$distribution | Sort-Object Method | Format-Table -AutoSize
$failed = @($distribution | Where-Object DistinctInstances -lt $ExpectedInstanceCount)
if ($failed.Count -gt 0) {
    throw "CRUD requests did not reach all $ExpectedInstanceCount instances for methods: $($failed.Method -join ', ')."
}

Write-Host "PASS: $JobCount temporary jobs completed create, read, update, publish/unpublish, delete, read-after-write, and application-service mirror checks across $ExpectedInstanceCount job-service instances."

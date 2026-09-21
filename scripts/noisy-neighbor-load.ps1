param(
    [string]$BaseUrl = "http://localhost:30000",

    [Parameter(Mandatory = $true)]
    [string]$TenantAToken,

    [Parameter(Mandatory = $true)]
    [string]$TenantBToken,

    [int]$TenantARequests = 10,
    [int]$TenantBRequests = 90,

    [int]$DelayMilliseconds = 100
)

$ErrorActionPreference = "Stop"

function Send-TenantRequests {
    param(
        [string]$TenantName,
        [string]$Token,
        [int]$RequestCount
    )

    $headers = @{
        Authorization = "Bearer $Token"
    }

    $successCount = 0
    $failureCount = 0
    $totalDurationMs = 0.0

    Write-Host ""
    Write-Host "Starting $TenantName workload: $RequestCount requests"

    for ($i = 1; $i -le $RequestCount; $i++) {

        $stopwatch =
            [System.Diagnostics.Stopwatch]::StartNew()

        try {

            Invoke-WebRequest `
                -Uri "$BaseUrl/api/projects" `
                -Method Get `
                -Headers $headers `
                -UseBasicParsing `
                | Out-Null

            $successCount++

        }
        catch {

            $failureCount++

            Write-Warning (
                "$TenantName request $i failed: " +
                $_.Exception.Message
            )
        }
        finally {

            $stopwatch.Stop()

            $totalDurationMs +=
                $stopwatch.Elapsed.TotalMilliseconds
        }

        if ($DelayMilliseconds -gt 0) {
            Start-Sleep `
                -Milliseconds $DelayMilliseconds
        }
    }

    $averageDurationMs =
        if ($RequestCount -eq 0) {
            0
        }
        else {
            $totalDurationMs / $RequestCount
        }

    [PSCustomObject]@{
        Tenant            = $TenantName
        Requests          = $RequestCount
        Successful        = $successCount
        Failed            = $failureCount
        AverageDurationMs = [math]::Round(
            $averageDurationMs,
            2
        )
    }
}

Write-Host "CloudPilot noisy-neighbor load simulation"
Write-Host "Base URL: $BaseUrl"
Write-Host ""

$tenantAResult =
    Send-TenantRequests `
        -TenantName "Tenant-A" `
        -Token $TenantAToken `
        -RequestCount $TenantARequests

$tenantBResult =
    Send-TenantRequests `
        -TenantName "Tenant-B" `
        -Token $TenantBToken `
        -RequestCount $TenantBRequests

Write-Host ""
Write-Host "Simulation complete"
Write-Host ""

@(
    $tenantAResult
    $tenantBResult
) | Format-Table -AutoSize

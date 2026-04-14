# Performance Benchmarking (k6)

This folder provides a repeatable load profile to measure dispatch scalability with real metrics.

## Prerequisites

1. Start backend locally or in a test environment.
2. Ensure test data has enough ambulances/drivers to serve dispatch.
3. Install k6: https://k6.io/docs/get-started/installation/

## Run

```powershell
k6 run .\perf\dispatch-load.js
```

Or target a remote base URL:

```powershell
$env:BASE_URL="https://your-host"; k6 run .\perf\dispatch-load.js
```

## What to capture for resume-safe claims

- Total requests and throughput (`http_reqs`)
- Error rate (`http_req_failed`)
- Latency percentiles (`http_req_duration`, especially p95 and p99)
- Max VUs sustained before SLA violation

## Suggested claim format

"Load-tested emergency dispatch workflow at X virtual users with p95 latency Y ms and error rate Z%."

Only publish values from your own benchmark report.

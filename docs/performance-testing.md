# Performance testing

TaskFlow Pro includes a small Python 3.10+ standard-library load runner so measurement does not require a paid service or an extra package. It authenticates once, discovers the demo workspace, warms the dashboard and task-list caches, then executes a controlled mix of authenticated reads.

## Run

Start the full stack, then run:

```bash
python load-tests/load_test.py --base-url http://localhost:8080 --users 10 --requests 200
```

To compare cached and uncached behavior, run once with `CACHE_ENABLED=true`, save the output, then restart the backend with `CACHE_ENABLED=false` and repeat under the same machine conditions. Do not compare runs with different request counts or concurrency.

The script exits non-zero if any request fails and writes a machine-readable report when `--output` is supplied:

```bash
python load-tests/load_test.py --users 10 --requests 200 \
  --output load-tests/results/local-cache-enabled.json
```

## Interpretation

The report includes wall time, throughput, successful/failed counts, and min/mean/p50/p95/p99/max latency. These are local observations, not production capacity guarantees. Results depend on CPU, available memory, Docker configuration, database state, network path, JVM warmup, and query mix.

No performance result belongs in a resume or README until the exact run has completed successfully and its raw JSON result is retained. The repository's README performance section records the current verified state.

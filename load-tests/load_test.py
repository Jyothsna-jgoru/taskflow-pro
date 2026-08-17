#!/usr/bin/env python3
"""Dependency-free, controlled authenticated read load test for TaskFlow Pro."""

from __future__ import annotations

import argparse
import concurrent.futures
import json
import math
import pathlib
import statistics
import sys
import time
import urllib.error
import urllib.request
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from typing import Any


@dataclass
class Observation:
    endpoint: str
    status: int
    latency_ms: float
    error: str | None = None


def request_json(
    url: str,
    token: str | None = None,
    payload: dict[str, Any] | None = None,
) -> tuple[int, Any]:
    body = json.dumps(payload).encode() if payload is not None else None
    headers = {"Accept": "application/json"}
    if payload is not None:
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(
        url,
        data=body,
        headers=headers,
        method="POST" if payload is not None else "GET",
    )
    with urllib.request.urlopen(request, timeout=15) as response:
        content = response.read()
        return response.status, json.loads(content) if content else None


def percentile(values: list[float], fraction: float) -> float:
    if not values:
        return 0.0
    index = max(0, math.ceil(len(values) * fraction) - 1)
    return sorted(values)[index]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--email", default="admin@taskflow.local")
    parser.add_argument("--password", default="Admin123!")
    parser.add_argument("--users", type=int, default=10)
    parser.add_argument("--requests", type=int, default=200)
    parser.add_argument("--output", type=pathlib.Path)
    args = parser.parse_args()
    if not 1 <= args.users <= 100 or not 1 <= args.requests <= 100_000:
        parser.error("users must be 1..100 and requests must be 1..100000")

    base = args.base_url.rstrip("/")
    try:
        _, auth = request_json(
            f"{base}/api/auth/login",
            payload={"email": args.email, "password": args.password},
        )
        token = auth["accessToken"]
        _, workspaces = request_json(f"{base}/api/workspaces", token)
        workspace_id = workspaces[0]["id"]
    except (urllib.error.URLError, KeyError, IndexError, TypeError) as error:
        print(f"Setup failed: {error}", file=sys.stderr)
        return 2

    endpoints = [
        ("dashboard", f"{base}/api/workspaces/{workspace_id}/dashboard"),
        (
            "tasks",
            f"{base}/api/workspaces/{workspace_id}/tasks"
            "?page=0&size=20&sort=updatedAt&direction=desc",
        ),
        ("projects", f"{base}/api/workspaces/{workspace_id}/projects"),
    ]
    for _, url in endpoints:
        request_json(url, token)

    def run_one(index: int) -> Observation:
        name, url = endpoints[index % len(endpoints)]
        started = time.perf_counter()
        try:
            status, _ = request_json(url, token)
            return Observation(name, status, (time.perf_counter() - started) * 1000)
        except urllib.error.HTTPError as error:
            return Observation(
                name,
                error.code,
                (time.perf_counter() - started) * 1000,
                str(error),
            )
        except Exception as error:  # noqa: BLE001 - every transport error belongs in the measurement
            return Observation(name, 0, (time.perf_counter() - started) * 1000, str(error))

    started = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.users) as executor:
        observations = list(executor.map(run_one, range(args.requests)))
    wall_seconds = time.perf_counter() - started
    successful = [item for item in observations if 200 <= item.status < 300]
    latencies = [item.latency_ms for item in successful]
    report = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "baseUrl": base,
        "concurrency": args.users,
        "requests": args.requests,
        "successful": len(successful),
        "failed": args.requests - len(successful),
        "wallSeconds": round(wall_seconds, 3),
        "requestsPerSecond": round(len(successful) / wall_seconds, 2),
        "latencyMs": {
            "min": round(min(latencies), 2) if latencies else None,
            "mean": round(statistics.fmean(latencies), 2) if latencies else None,
            "p50": round(percentile(latencies, 0.50), 2) if latencies else None,
            "p95": round(percentile(latencies, 0.95), 2) if latencies else None,
            "p99": round(percentile(latencies, 0.99), 2) if latencies else None,
            "max": round(max(latencies), 2) if latencies else None,
        },
        "byEndpoint": {
            name: {
                "requests": sum(1 for item in observations if item.endpoint == name),
                "failed": sum(
                    1
                    for item in observations
                    if item.endpoint == name and not 200 <= item.status < 300
                ),
            }
            for name, _ in endpoints
        },
        "errors": [asdict(item) for item in observations if item.error][:20],
    }
    rendered = json.dumps(report, indent=2)
    print(rendered)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered + "\n", encoding="utf-8")
    return 0 if report["failed"] == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())

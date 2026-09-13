from collections import Counter, deque
import re
from threading import Lock
from time import monotonic
from uuid import uuid4

from app.telemetry import record_active, record_request


class RequestMetrics:
    def __init__(self, window_size: int = 1000):
        self._lock = Lock()
        self._window = deque(maxlen=window_size)
        self._outcomes = Counter()
        self._active = 0

    def start(self) -> float:
        with self._lock:
            self._active += 1
        record_active(1)
        return monotonic()

    def finish(self, started: float, outcome: str) -> None:
        duration_ms = round((monotonic() - started) * 1000, 2)
        with self._lock:
            self._active = max(0, self._active - 1)
            self._outcomes[outcome] += 1
            self._window.append(duration_ms)
        record_active(-1)
        record_request(duration_ms, outcome)

    def snapshot(self, p95_limit_ms: float = 2000, error_rate_limit: float = 0.05) -> dict:
        with self._lock:
            durations = sorted(self._window)
            outcomes = dict(self._outcomes)
            active = self._active
        total = sum(outcomes.values())

        def percentile(ratio: float) -> float:
            if not durations:
                return 0.0
            index = min(len(durations) - 1, max(0, int(len(durations) * ratio) - 1))
            return durations[index]

        errors = outcomes.get("error", 0)
        p95 = percentile(0.95)
        error_rate = errors / total if total else 0.0
        return {
            "requests": total,
            "active": active,
            "outcomes": outcomes,
            "errorRate": round(error_rate, 4),
            "latencyMs": {
                "p50": percentile(0.50),
                "p95": p95,
                "p99": percentile(0.99),
                "max": durations[-1] if durations else 0.0,
            },
            "sla": {
                "status": "healthy" if p95 <= p95_limit_ms and error_rate <= error_rate_limit else "degraded",
                "p95LimitMs": p95_limit_ms,
                "errorRateLimit": error_rate_limit,
            },
            "windowSize": len(durations),
        }


metrics = RequestMetrics()


def trace_id(value: str = "") -> str:
    candidate = value.strip()
    return candidate if re.fullmatch(r"[A-Za-z0-9._:-]{1,64}", candidate) else uuid4().hex

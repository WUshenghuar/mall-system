from collections import Counter, deque
from threading import Lock
from time import monotonic


class RequestMetrics:
    def __init__(self, window_size: int = 1000):
        self._lock = Lock()
        self._window = deque(maxlen=window_size)
        self._outcomes = Counter()
        self._active = 0

    def start(self) -> float:
        with self._lock:
            self._active += 1
        return monotonic()

    def finish(self, started: float, outcome: str) -> None:
        with self._lock:
            self._active = max(0, self._active - 1)
            self._outcomes[outcome] += 1
            self._window.append(round((monotonic() - started) * 1000, 2))

    def snapshot(self) -> dict:
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
        return {
            "requests": total,
            "active": active,
            "outcomes": outcomes,
            "errorRate": round(errors / total, 4) if total else 0.0,
            "latencyMs": {
                "p50": percentile(0.50),
                "p95": percentile(0.95),
                "p99": percentile(0.99),
                "max": durations[-1] if durations else 0.0,
            },
            "windowSize": len(durations),
        }


metrics = RequestMetrics()

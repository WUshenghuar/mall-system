from app.observability import RequestMetrics


def test_request_metrics_reports_outcomes_and_percentiles():
    metrics = RequestMetrics(window_size=3)
    for outcome in ("completed", "completed", "error", "disabled"):
        started = metrics.start()
        metrics.finish(started, outcome)

    result = metrics.snapshot()

    assert result["requests"] == 4
    assert result["active"] == 0
    assert result["outcomes"] == {"completed": 2, "error": 1, "disabled": 1}
    assert result["errorRate"] == 0.25
    assert result["windowSize"] == 3
    assert result["latencyMs"]["p95"] >= result["latencyMs"]["p50"]

from app.observability import RequestMetrics, trace_id


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
    assert result["sla"]["status"] == "degraded"


def test_request_metrics_marks_sla_degraded_when_error_rate_exceeds_threshold():
    metrics = RequestMetrics()
    started = metrics.start()
    metrics.finish(started, "error")

    assert metrics.snapshot(error_rate_limit=0)["sla"]["status"] == "degraded"


def test_trace_id_accepts_safe_correlation_id_and_replaces_invalid_value():
    assert trace_id("request-123") == "request-123"
    assert len(trace_id("bad trace id")) == 32

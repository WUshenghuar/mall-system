from app import telemetry


def test_telemetry_is_noop_without_otlp_endpoint(monkeypatch):
    monkeypatch.delenv("OTEL_EXPORTER_OTLP_ENDPOINT", raising=False)
    monkeypatch.delenv("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", raising=False)
    monkeypatch.delenv("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT", raising=False)
    monkeypatch.delenv("CBEC_OTEL_LOCAL_TRACES_ENDPOINT", raising=False)
    monkeypatch.delenv("CBEC_OTEL_LOCAL_METRICS_ENDPOINT", raising=False)
    monkeypatch.setattr(telemetry, "_configured", False)

    assert telemetry.configure_telemetry() is False
    assert telemetry.telemetry_configured() is False
    with telemetry.span("test.span"):
        pass


def test_otlp_endpoint_and_headers_support_signal_specific_paths(monkeypatch):
    monkeypatch.setenv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://collector:4318/v1/traces")
    monkeypatch.delenv("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", raising=False)
    monkeypatch.delenv("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT", raising=False)
    monkeypatch.setenv("OTEL_EXPORTER_OTLP_HEADERS", "Authorization=Basic abc==,x-tenant=cbec")
    monkeypatch.setenv("OTEL_EXPORTER_OTLP_TRACES_HEADERS", "x-tenant=trace")

    assert telemetry._endpoint("TRACES") == "http://collector:4318/v1/traces"
    assert telemetry._endpoint("METRICS") == "http://collector:4318/v1/metrics"
    assert telemetry._headers("TRACES") == {"Authorization": "Basic abc==", "x-tenant": "trace"}


def test_mark_error_is_safe_without_a_recording_span():
    telemetry.mark_error()


def test_record_tool_plan_records_outcome_and_duration(monkeypatch):
    class Counter:
        def __init__(self):
            self.calls = []

        def add(self, value, attributes):
            self.calls.append((value, attributes))

    class Histogram:
        def __init__(self):
            self.calls = []

        def record(self, value, attributes):
            self.calls.append((value, attributes))

    counter, histogram = Counter(), Histogram()
    monkeypatch.setattr(telemetry, "_tool_plan_counter", counter)
    monkeypatch.setattr(telemetry, "_tool_plan_duration", histogram)

    telemetry.record_tool_plan(12.5, "planned")

    assert counter.calls == [(1, {"outcome": "planned"})]
    assert histogram.calls == [(12.5, {"outcome": "planned"})]


def test_langfuse_configuration_builds_otlp_trace_auth(monkeypatch):
    monkeypatch.delenv("OTEL_EXPORTER_OTLP_ENDPOINT", raising=False)
    monkeypatch.delenv("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", raising=False)
    monkeypatch.delenv("OTEL_EXPORTER_OTLP_HEADERS", raising=False)
    monkeypatch.setenv("LANGFUSE_BASE_URL", "https://langfuse.test")
    monkeypatch.setenv("LANGFUSE_PUBLIC_KEY", "pk")
    monkeypatch.setenv("LANGFUSE_SECRET_KEY", "sk")

    assert telemetry.langfuse_configured() is True
    assert telemetry._endpoint("TRACES") == "https://langfuse.test/api/public/otel/v1/traces"
    assert telemetry._headers("TRACES") == {
        "Authorization": "Basic cGs6c2s=", "x-langfuse-ingestion-version": "4"
    }
    assert telemetry._headers("METRICS") == {}


def test_trace_context_is_scoped_and_merges_nested_attributes():
    assert telemetry._trace_attributes.get() == {}


def test_remote_context_is_safe_for_missing_or_invalid_parent():
    with telemetry.remote_context(""):
        with telemetry.span("test.remote"):
            pass
    with telemetry.trace_context({"langfuse.session.id": "session-1"}):
        assert telemetry._trace_attributes.get() == {"langfuse.session.id": "session-1"}
        with telemetry.trace_context({"langfuse.trace.name": "chat"}):
            assert telemetry._trace_attributes.get() == {
                "langfuse.session.id": "session-1", "langfuse.trace.name": "chat"
            }
        assert telemetry._trace_attributes.get() == {"langfuse.session.id": "session-1"}
    assert telemetry._trace_attributes.get() == {}

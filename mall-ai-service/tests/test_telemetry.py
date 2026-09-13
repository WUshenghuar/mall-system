from app import telemetry


def test_telemetry_is_noop_without_otlp_endpoint(monkeypatch):
    monkeypatch.delenv("OTEL_EXPORTER_OTLP_ENDPOINT", raising=False)
    monkeypatch.delenv("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", raising=False)
    monkeypatch.delenv("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT", raising=False)
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

    assert telemetry._endpoint("TRACES") == "http://collector:4318/v1/traces"
    assert telemetry._endpoint("METRICS") == "http://collector:4318/v1/metrics"
    assert telemetry._headers() == {"Authorization": "Basic abc==", "x-tenant": "cbec"}


def test_mark_error_is_safe_without_a_recording_span():
    telemetry.mark_error()

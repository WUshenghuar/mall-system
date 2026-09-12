from app import telemetry


def test_telemetry_is_noop_without_otlp_endpoint(monkeypatch):
    monkeypatch.delenv("OTEL_EXPORTER_OTLP_ENDPOINT", raising=False)
    monkeypatch.delenv("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", raising=False)
    monkeypatch.setattr(telemetry, "_configured", False)

    assert telemetry.configure_telemetry() is False
    assert telemetry.telemetry_configured() is False
    with telemetry.span("test.span"):
        pass

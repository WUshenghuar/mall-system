import os
import base64
from contextlib import contextmanager, nullcontext
from contextvars import ContextVar

try:
    from opentelemetry import metrics as otel_metrics
    from opentelemetry import trace
    from opentelemetry.exporter.otlp.proto.http.metric_exporter import OTLPMetricExporter
    from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
    from opentelemetry.sdk.resources import SERVICE_NAME, Resource
    from opentelemetry.sdk.metrics import MeterProvider
    from opentelemetry.sdk.metrics.export import PeriodicExportingMetricReader
    from opentelemetry.trace import Status, StatusCode
    from opentelemetry.sdk.trace import TracerProvider
    from opentelemetry.sdk.trace.export import BatchSpanProcessor
except ImportError:  # optional telemetry dependency
    otel_metrics = None
    trace = None


_configured = False
_request_counter = None
_request_duration = None
_active_requests = None
_trace_attributes = ContextVar("ai_trace_attributes", default={})


def _endpoint(signal_name: str) -> str:
    specific = os.getenv(f"OTEL_EXPORTER_OTLP_{signal_name}_ENDPOINT", "").strip()
    if specific:
        return specific.rstrip("/")
    endpoint = os.getenv("OTEL_EXPORTER_OTLP_ENDPOINT", "").strip().rstrip("/")
    if endpoint:
        for signal in ("traces", "metrics"):
            if endpoint.endswith(f"/v1/{signal}"):
                endpoint = endpoint[:-(len(signal) + 4)]
                break
        suffix = f"/v1/{signal_name.lower()}"
        return endpoint if endpoint.endswith(suffix) else endpoint + suffix
    if signal_name == "TRACES" and langfuse_configured():
        endpoint = os.getenv("LANGFUSE_BASE_URL", "").strip().rstrip("/")
        if not endpoint.endswith("/api/public/otel"):
            endpoint += "/api/public/otel"
        return endpoint + "/v1/traces"
    return os.getenv(f"CBEC_OTEL_LOCAL_{signal_name}_ENDPOINT", "").strip().rstrip("/")


def langfuse_configured() -> bool:
    return all(os.getenv(name, "").strip() for name in ("LANGFUSE_BASE_URL", "LANGFUSE_PUBLIC_KEY", "LANGFUSE_SECRET_KEY"))


def _parse_headers(value: str) -> dict[str, str]:
    result = {}
    for item in value.split(","):
        if "=" not in item:
            continue
        key, value = item.split("=", 1)
        if key.strip():
            result[key.strip()] = value.strip()
    return result


def _headers(signal_name: str = "") -> dict[str, str]:
    result = _parse_headers(os.getenv("OTEL_EXPORTER_OTLP_HEADERS", ""))
    if signal_name:
        result.update(_parse_headers(os.getenv(f"OTEL_EXPORTER_OTLP_{signal_name}_HEADERS", "")))
    if signal_name == "TRACES" and langfuse_configured():
        auth = base64.b64encode(
            f"{os.getenv('LANGFUSE_PUBLIC_KEY')}:{os.getenv('LANGFUSE_SECRET_KEY')}".encode()
        ).decode()
        result.setdefault("Authorization", f"Basic {auth}")
        result.setdefault("x-langfuse-ingestion-version", "4")
    return result


def _metric_interval() -> int:
    try:
        return max(1000, int(os.getenv("OTEL_METRIC_EXPORT_INTERVAL", "10000")))
    except ValueError:
        return 10000


def configure_telemetry() -> bool:
    global _active_requests, _configured, _request_counter, _request_duration
    if _configured or trace is None:
        return _configured
    resource = Resource.create({SERVICE_NAME: os.getenv("OTEL_SERVICE_NAME", "cbec-ai-service")})
    trace_headers = _headers("TRACES")
    metric_headers = _headers("METRICS")
    configured = False
    trace_endpoint = _endpoint("TRACES")
    if trace_endpoint:
        provider = TracerProvider(resource=resource)
        provider.add_span_processor(BatchSpanProcessor(OTLPSpanExporter(endpoint=trace_endpoint, headers=trace_headers)))
        trace.set_tracer_provider(provider)
        configured = True

    metric_endpoint = _endpoint("METRICS")
    if otel_metrics is not None and metric_endpoint:
        reader = PeriodicExportingMetricReader(
            OTLPMetricExporter(endpoint=metric_endpoint, headers=metric_headers),
            export_interval_millis=_metric_interval(),
        )
        otel_metrics.set_meter_provider(MeterProvider(resource=resource, metric_readers=[reader]))
        meter = otel_metrics.get_meter("cbec.ai")
        _request_counter = meter.create_counter("ai_chat_requests", unit="{request}")
        _request_duration = meter.create_histogram("ai_chat_request_duration", unit="ms")
        _active_requests = meter.create_up_down_counter("ai_chat_active_requests", unit="{request}")
        configured = True

    _configured = configured
    return configured


def telemetry_configured() -> bool:
    return _configured


@contextmanager
def trace_context(attributes: dict[str, str]):
    token = _trace_attributes.set({**_trace_attributes.get(), **attributes})
    try:
        yield
    finally:
        _trace_attributes.reset(token)


def record_active(delta: int) -> None:
    if _active_requests is not None:
        _active_requests.add(delta)


def record_request(duration_ms: float, outcome: str) -> None:
    if _request_counter is None or _request_duration is None:
        return
    attributes = {"outcome": outcome}
    _request_counter.add(1, attributes)
    _request_duration.record(duration_ms, attributes)


def mark_error(description: str = "ai_request_error") -> None:
    if trace is None:
        return
    current = trace.get_current_span()
    if current.is_recording():
        current.set_status(Status(StatusCode.ERROR, description))


def set_span_attributes(attributes: dict) -> None:
    if trace is None:
        return
    current = trace.get_current_span()
    if current.is_recording():
        for key, value in attributes.items():
            current.set_attribute(key, value)


def span(name: str, attributes: dict | None = None):
    if trace is None:
        return nullcontext()
    merged = {**_trace_attributes.get(), **(attributes or {})}
    return trace.get_tracer("cbec.ai").start_as_current_span(name, attributes=merged)

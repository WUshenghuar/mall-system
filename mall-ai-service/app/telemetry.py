from contextlib import nullcontext
import os

try:
    from opentelemetry import trace
    from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
    from opentelemetry.sdk.resources import SERVICE_NAME, Resource
    from opentelemetry.sdk.trace import TracerProvider
    from opentelemetry.sdk.trace.export import BatchSpanProcessor
except ImportError:  # optional telemetry dependency
    trace = None


_configured = False


def configure_telemetry() -> bool:
    global _configured
    if _configured or trace is None:
        return _configured
    endpoint = os.getenv("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", "").strip()
    if not endpoint:
        endpoint = os.getenv("OTEL_EXPORTER_OTLP_ENDPOINT", "").strip()
        if endpoint:
            endpoint = endpoint.rstrip("/") + "/v1/traces"
    if not endpoint:
        return False
    provider = TracerProvider(resource=Resource.create({SERVICE_NAME: os.getenv("OTEL_SERVICE_NAME", "cbec-ai-service")}))
    provider.add_span_processor(BatchSpanProcessor(OTLPSpanExporter(endpoint=endpoint)))
    trace.set_tracer_provider(provider)
    _configured = True
    return True


def telemetry_configured() -> bool:
    return _configured


def span(name: str, attributes: dict | None = None):
    if trace is None:
        return nullcontext()
    return trace.get_tracer("cbec.ai").start_as_current_span(name, attributes=attributes or {})

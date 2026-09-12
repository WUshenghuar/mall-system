import asyncio

from app.main import health


def test_health_reports_ai_feature_flags():
    result = asyncio.run(health())

    assert result["status"] == "ok"
    assert result["enabled"] is True
    assert isinstance(result["modelConfigured"], bool)
    assert isinstance(result["embeddingConfigured"], bool)
    assert result["toolPlanningEnabled"] == result["modelConfigured"]

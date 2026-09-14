from app.config import _bounded_float


def test_bounded_float_uses_default_for_invalid_or_out_of_range_values(monkeypatch):
    monkeypatch.setenv("TEST_AI_FLOAT", "not-a-number")
    assert _bounded_float("TEST_AI_FLOAT", 2000, 1) == 2000

    monkeypatch.setenv("TEST_AI_FLOAT", "nan")
    assert _bounded_float("TEST_AI_FLOAT", 2000, 1) == 2000

    monkeypatch.setenv("TEST_AI_FLOAT", "0")
    assert _bounded_float("TEST_AI_FLOAT", 2000, 1) == 2000

    monkeypatch.setenv("TEST_AI_FLOAT", "1.5")
    assert _bounded_float("TEST_AI_FLOAT", 0.05, 0, 1) == 0.05


def test_bounded_float_keeps_valid_sla_values(monkeypatch):
    monkeypatch.setenv("TEST_AI_FLOAT", "1500")
    assert _bounded_float("TEST_AI_FLOAT", 2000, 1) == 1500

    monkeypatch.setenv("TEST_AI_FLOAT", "0.1")
    assert _bounded_float("TEST_AI_FLOAT", 0.05, 0, 1) == 0.1

from evaluate import evaluate


def test_deterministic_policy_evaluation_is_green():
    result = evaluate()
    assert result == {"passed": 6, "total": 6, "score": 100}

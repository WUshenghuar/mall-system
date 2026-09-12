from app.api.chat import business_tool_names


def test_business_tool_names_filters_unknown_tools_and_caps_batch():
    assert business_tool_names("query_member,query_product,drop_database,query_tax,query_order") == [
        "query_member", "query_product", "query_tax"
    ]

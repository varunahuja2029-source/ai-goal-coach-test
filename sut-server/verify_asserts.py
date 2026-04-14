#!/usr/bin/env python3
"""Assert mock coach responses: schema, keywords, and HTTP POST /api/coach."""

from __future__ import annotations

import json
from typing import Any

from app import app, resolve_coach_payload


def _assert_schema(body: dict[str, Any]) -> None:
    assert isinstance(body.get("refined_goal"), str), body
    kr = body.get("key_results")
    assert isinstance(kr, list) and kr, body
    assert all(isinstance(x, str) for x in kr), body
    cs = body.get("confidence_score")
    assert isinstance(cs, int) and 1 <= cs <= 10, body


def main() -> None:
    client = app.test_client()

    # --- resolve_coach_payload (mock layer) ---
    upskill = resolve_coach_payload("I want to upskill")
    assert upskill["confidence_score"] == 9
    assert len(upskill["key_results"]) == 3
    _assert_schema(upskill)

    sales = resolve_coach_payload("sales growth")
    assert sales["confidence_score"] == 8
    assert "SPIN Selling" in sales["key_results"][-1]
    _assert_schema(sales)

    gib = resolve_coach_payload("asdf nonsense")
    assert gib["confidence_score"] == 2
    assert gib["refined_goal"].startswith("Unable to determine")
    _assert_schema(gib)

    many = resolve_coach_payload("too many results please")
    assert many["confidence_score"] == 7
    assert many["key_results"] == [f"Task {i}" for i in range(1, 7)]
    _assert_schema(many)

    default = resolve_coach_payload("completely unmatched xyz")
    assert default["confidence_score"] == 5
    _assert_schema(default)

    empty = resolve_coach_payload("")
    assert empty == default
    _assert_schema(empty)

    # --- HTTP /api/coach ---
    r = client.post("/api/coach", json={"q": "upskill"})
    assert r.status_code == 200, r.data
    assert r.is_json
    _assert_schema(r.get_json())

    r2 = client.post(
        "/api/coach",
        data=json.dumps({"q": "sales"}),
        content_type="application/json",
    )
    assert r2.status_code == 200
    assert r2.get_json()["confidence_score"] == 8

    r3 = client.post("/api/coach", data="not-json", content_type="application/json")
    assert r3.status_code == 200
    _assert_schema(r3.get_json())

    print("verify_asserts: all assertions passed")


if __name__ == "__main__":
    main()

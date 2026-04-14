"""
Minimal mock "AI Goal Coach" API for local testing.
POST /api/coach — JSON body: {"q": "<user goal text>"}
"""

from __future__ import annotations

from flask import Flask, jsonify, request

# --- Mock data layer (responses keyed by substring / exact-match triggers) ---

_COACH_RESPONSES: list[tuple[str, dict]] = [
    (
        "too many results",
        {
            "refined_goal": "Improve everything immediately.",
            "key_results": [
                "Task 1",
                "Task 2",
                "Task 3",
                "Task 4",
                "Task 5",
                "Task 6",
            ],
            "confidence_score": 7,
        },
    ),
    (
        "upskill",
        {
            "refined_goal": "Acquire advanced technical skills to transition into a senior engineering role within 6 months.",
            "key_results": [
                "Complete 3 advanced cloud architecture courses.",
                "Contribute to 2 major open-source projects.",
                "Lead one internal technical workshop.",
            ],
            "confidence_score": 9,
        },
    ),
    (
        "sales",
        {
            "refined_goal": "Increase quarterly sales conversions by focusing on targeted outreach and pipeline management.",
            "key_results": [
                "Make 50 cold calls per week.",
                "Close 5 enterprise deals by the end of Q3.",
                "Read and implement strategies from 'SPIN Selling'.",
            ],
            "confidence_score": 8,
        },
    ),
    (
        "asdf",
        {
            "refined_goal": "Unable to determine a clear goal from the input provided.",
            "key_results": [
                "Please clarify your original statement.",
                "Provide specific context on what you want to achieve.",
            ],
            "confidence_score": 2,
        },
    ),
]

_DEFAULT_RESPONSE: dict = {
    "refined_goal": "No specific coaching template matched your input; refine your goal with more detail.",
    "key_results": [
        "Restate your goal in one concrete sentence.",
        "Add a timeframe and measurable outcome.",
        "Identify one skill or habit to develop first.",
    ],
    "confidence_score": 5,
}


def resolve_coach_payload(q: str) -> dict:
    """Match `q` against predefined keywords (substring, case-insensitive); else fallback."""
    text = (q or "").strip().lower()
    if not text:
        return dict(_DEFAULT_RESPONSE)
    for keyword, payload in _COACH_RESPONSES:
        if keyword in text:
            return dict(payload)
    return dict(_DEFAULT_RESPONSE)


# --- HTTP layer ---

app = Flask(__name__)


@app.route("/api/coach", methods=["POST"])
def coach():
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        data = {}
    raw_q = data.get("q", "")
    q = raw_q if isinstance(raw_q, str) else str(raw_q)
    return jsonify(resolve_coach_payload(q))


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=8002, debug=True)

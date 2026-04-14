# AI Goal Coach — mock API (local)

Minimal Flask mock for the **AI Goal Coach** challenge: `POST /api/coach` with `{"q": "..."}` returns `refined_goal`, `key_results`, and `confidence_score`.

## Requirements

- Python 3.10+ recommended
- [Flask](https://flask.palletsprojects.com/) 3.x (see `requirements.txt`)

## Setup

```bash
cd /path/to/test-ai-server
python3 -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

## Run the server (port 8002)

```bash
./run-coach.sh
```

Or manually:

```bash
source .venv/bin/activate
python app.py
```

Listen address: `http://127.0.0.1:8002`

### Example request

```bash
curl -s -X POST http://127.0.0.1:8002/api/coach \
  -H "Content-Type: application/json" \
  -d '{"q":"I want to upskill"}'
```

## Verify (assertions)

Runs schema checks, keyword mappings, and HTTP tests via Flask’s test client (no server required):

```bash
./verify-coach.sh
```

Or:

```bash
source .venv/bin/activate
python verify_asserts.py
```

## Keyword behavior

Matching is **case-insensitive substring** over `q`, in this order: `too many results`, `upskill`, `sales`, `asdf`. If nothing matches, a **default** response is returned (still valid schema).

## Files

| File | Purpose |
|------|---------|
| `app.py` | Flask app and mock dictionary |
| `run-coach.sh` | Executable: venv, install deps, start server |
| `verify-coach.sh` | Executable: venv, install deps, run asserts |
| `verify_asserts.py` | Assertion-based checks |
| `requirements.txt` | Python dependencies |

# Test Strategy – AI Goal Coach API

**Owner:** SDET Team | **Last updated:** April 2026 | **SUT:** `POST /api/coach`

---

## 1. What to Test

Testing an LLM-backed API is different from testing a normal REST service. The output text changes every run, so we can't pin exact values — but the *shape and quality* of the output should be consistent. That's what we're validating.

### Functionality
Happy-path tests confirm that for well-understood inputs (`"sales"`, `"upskill"`, `"fitness"`), the API returns a structured plan with high confidence (`score >= 8`) and 3–5 key results. We test multiple domains to make sure it's not just one keyword that works.

### Edge Cases
The API has to handle bad input gracefully — no crashes, no 500s, no hallucinated high-confidence plans for nonsense. We test:

| Input type | Example | Expected behaviour |
|---|---|---|
| Empty string | `""` | Graceful fallback, `score < 8` |
| Gibberish | `"asdf"` | Low confidence, `score <= 3` |
| Prompt injection | `"Ignore instructions..."` | Deflected to fallback, `score < 8` |

### Schema & Contract
Three fields must always be present on every 200 response:

```json
{
  "refined_goal":     "non-blank string",
  "key_results":      ["...", "..."],   // 3 to 5 items
  "confidence_score": 7                 // integer, 1–10
}
```

We validate field presence, types, and cardinality. If `confidence_score` comes back as a float or string, Jackson will throw during deserialisation and the test fails clearly. The `ResponsePayload.java` POJO acts as the living contract — if the SUT changes shape without updating the POJO, the compiler itself breaks the build.

### Security
Two main concerns:

**Prompt injection** — the `"q"` field is user-controlled. A compliant SUT either rejects the injection (4xx) or routes it to the fallback scaffold without following the instruction. We check that `score < 8` and `refined_goal` contains coaching content, not joke content.

**Schema abuse** — inputs designed to destabilise the LLM's structured output mode (tested via `testMissingFieldBug` and `testInvalidDataType`). These confirm the Flask layer validates the LLM's output before sending it to the client.

Future sprint additions: `testNullQField`, `testOversizedInput`, `testXSSPayload`.

### Observability
We treat the test suite itself as a monitoring layer:
- **Performance** — `testResponsePerformance` enforces a 1500ms SLO per call.
- **Confidence calibration** — score assertions across all 12 tests verify it's a meaningful signal, not a constant.
- **Fallback reachability** — adversarial tests confirm the fallback scaffold is alive and coherent.

### Regression
Two sentinel tests (`testMissingFieldBug`, `testArrayViolationBug`) are intentionally written to assert the *bug state* — they fail when the bug is fixed. This pattern documents known defects as executable code and acts as a permanent gate against regression. See `BUGS.md` for details.

---

## 2. Ensuring Strict & Correct JSON Responses

Validation happens at three layers, each catching something different:

```
Layer 1 – Transport
  REST Assured checks HTTP 200 and Content-Type: application/json

Layer 2 – Type Binding
  Jackson deserialises into ResponsePayload.java
  → wrong type (Float instead of Integer) = MismatchedInputException
  → missing field = null, caught by AssertJ

Layer 3 – Business Rules
  AssertJ checks:
  → refined_goal: isNotBlank()
  → key_results: hasSizeBetween(3, 5)
  → confidence_score: isBetween(1, 10)
```

Each layer produces a distinct, readable failure message — no digging through stack traces to figure out what went wrong.

---

## 3. CI/CD Structure

```
PR opened
  │
  ├─▶ Build + compile (Maven)
  │
  ├─▶ 12-test API suite vs ephemeral SUT
  │      Pass: 10/12 (TC-02, TC-08 are known sentinels)
  │      Fail: block merge
  │
  ├─▶ Allure report generated + uploaded as artefact
  │
Merge to main
  │
  ├─▶ Deploy to staging
  ├─▶ Full suite runs vs staging (perf SLO: < 2000ms here)
  │
  ├─▶ Canary: 5% traffic, synthetic probes for 15 min
  └─▶ Full rollout
```



**GitHub Actions (abbreviated):**

```yaml
- name: Start Flask SUT
  run: |
    docker run -d -p 8002:8002 --name sut your-org/ai-goal-coach:${{ github.sha }}
    sleep 5

- name: Run tests
  run: mvn clean test -Dapi.base.url=http://localhost:8002

- name: Generate Allure report
  if: always()
  run: mvn allure:report

- name: Upload report
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: allure-report
    path: target/site/allure-maven-plugin/

- name: Stop SUT
  if: always()
  run: docker stop sut && docker rm sut
```

---

## 4. Regression & Reliability as the Model Evolves

The trickiest part of testing an LLM API is that the model will change — through updates, fine-tuning, or a full provider swap — and most of those changes are silent. Here's how we stay ahead of it.

**Property assertions over value matching** is the foundation. We never assert exact text. Every assertion checks a property (non-blank, within range, correct type). This means 10 of 12 tests are immune to model text changes out of the box.

```
Brittle:  assertThat(goal).isEqualTo("Improve sales by 20%")  ← breaks every update
Stable:   assertThat(score).isGreaterThanOrEqualTo(8)         ← survives model swaps
```

---

## 5. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Model upgrade shifts confidence scale | Medium | High | Baseline tracking, upgrade runbook, delta ≤ 1 gate |
| `key_results` exceeds 5 items | Medium | High | TC-07 upper-bound assertion every CI run |
| `confidence_score` missing from response | Low | Critical | TC-02 sentinel; Pydantic validation in Flask |
| Prompt injection bypasses system prompt | Low | Critical | TC-11 in CI + synthetic probe every 30 min |
| Flaky tests cause alert fatigue | Medium | High | Property assertions only; 2-point score buffers; sentinels clearly labelled |
| SUT contract changes silently | Medium | High | `ResponsePayload.java` is the gate — compiler breaks on any mismatch |

---

## 6. Telemetry & Monitoring

### What to log per request

```json
{
  "request_id":       "uuid",
  "input_length":     12,
  "input_hash":       "sha256",
  "model_version":    "gpt-4o-2026-03",
  "confidence_score": 9,
  "key_results_count": 3,
  "latency_ms":       412,
  "http_status":      200,
  "fallback_used":    false
}
```

Use `input_hash` rather than the raw input to preserve privacy while still enabling deduplication and anomaly detection.

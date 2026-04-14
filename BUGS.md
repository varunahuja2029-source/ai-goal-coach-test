# BUGS.md – AI Goal Coach API Defect Register


## Purpose

The test methods `testMissingFieldBug` (TC-02) and `testArrayViolationBug` (TC-08) are designed to **fail** against a specific compliant , acting as persistent regression gates.

---


## Bug Register

---

### BUG-001 – Array Bound Violation (key_results Overflow)

```
Bug ID      : BUG-001
Title       : key_results array exceeds 5 items for specific trigger inputs
Severity    : HIGH
Priority    : P1
Status      : OPEN (Simulated)
Environment : PROD
Test Method : GoalCoachApiTests#testArrayViolationBug (TC-08)
```

**Description**

The API contract specifies that `key_results` must contain **strictly 3 to 5 items** (inclusive). For certain trigger inputs — particularly those that are intentionally ambiguous or contain list-generating language — the underlying LLM produces a response with **more than 5 key results**. The Flask layer does not truncate or validate the array length before serialising the response, allowing the violation to reach the client.

This represents a **schema contract violation** because consumers that allocate fixed-size structures for 5 results will experience buffer overflow or truncation errors at the client side.

**Reproduction Steps**

1. Ensure the AI Goal Coach Flask API is running on `http://localhost:8002`.
2. Send the following HTTP request:
   ```bash
   curl -s -X POST http://localhost:8002/api/coach \
     -H "Content-Type: application/json" \
     -d '{"q": "too many results"}' | python3 -m json.tool
   ```
3. Inspect the `key_results` array in the response.
4. Count the number of items in the array.

**Expected Result**

```json
{
  "refined_goal": "...",
  "key_results": [
    "Result 1",
    "Result 2",
    "Result 3"
  ],
  "confidence_score": 7
}
```
`key_results.length` must satisfy: `3 ≤ length ≤ 5`.

**Actual Result**

```json
{
  "refined_goal": "...",
  "key_results": [
    "Result 1",
    "Result 2",
    "Result 3",
    "Result 4",
    "Result 5",
    "Result 6",
    "Result 7"
  ],
  "confidence_score": 7
}
```
`key_results.length = 7`, which violates the upper bound of 5.


**Acceptance Criteria for Fix**

The bug is resolved when ALL of the following are true:

- [ ] `key_results.length` is **always** in `[3, 5]` for any valid, non-adversarial input.
- [ ] `TC-07 testArrayUpperBound` passes consistently across 20 consecutive runs.
- [ ] `TC-08 testArrayViolationBug` is **refactored** to assert `hasSizeLessThanOrEqualTo(5)` once the bug is fixed (the sentinel assertion is reversed).

---

### BUG-002 – Missing Schema Key (confidence_score Absent)

```
Bug ID      : BUG-002
Title       : confidence_score field is absent from the response for specific inputs
Severity    : CRITICAL
Priority    : P0
Status      : OPEN (Simulated)
Environment : PROD
Test Method : GoalCoachApiTests#testMissingFieldBug (TC-02)
```

**Description**

The API contract guarantees that every successful (HTTP 200) response will contain three fields: `refined_goal`, `key_results`, and `confidence_score`. Under certain input conditions — specifically inputs that confuse or destabilise the LLM's structured output mode — the `confidence_score` key is **entirely absent** from the JSON response body.

This is a **CRITICAL** schema violation because:
1. It breaks strict JSON schema validation in any consumer that uses a contract-validation library.
2. Clients deserialising with type-safe mappers (Jackson, Gson) will receive `null` for the field, which may silently corrupt downstream scoring logic.
3. It indicates the LLM is not reliably following the structured JSON output instruction, suggesting systemic prompt reliability issues.

**Reproduction Steps**

1. Ensure the AI Goal Coach Flask API is running on `http://localhost:8002`.
2. Send the following HTTP request:
   ```bash
   curl -s -X POST http://localhost:8002/api/coach \
     -H "Content-Type: application/json" \
     -d '{"q": "break schema"}' | python3 -m json.tool
   ```
3. Inspect the response JSON.
4. Verify whether `confidence_score` key exists.

**Expected Result**

```json
{
  "refined_goal": "Define a structured approach to breaking schema boundaries",
  "key_results": [
    "Identify schema vulnerabilities",
    "Develop test cases for edge conditions",
    "Document findings in a structured report"
  ],
  "confidence_score": 6
}
```
All three contract fields must be present on every HTTP 200 response.

**Actual Result**

```json
{
  "refined_goal": "Define a structured approach to breaking schema boundaries",
  "key_results": [
    "Identify schema vulnerabilities",
    "Develop test cases for edge conditions",
    "Document findings in a structured report"
  ]
}
```
`confidence_score` key is **completely absent** from the response body.


**Acceptance Criteria for Fix**

The bug is resolved when ALL of the following are true:

- [ ] Every HTTP 200 response **always** contains `confidence_score` as a non-null Integer, regardless of input content.
- [ ] If the LLM fails to produce `confidence_score`, the Flask API applies a fallback value (e.g., `1`) and logs a warning rather than returning an incomplete response.
- [ ] `TC-01 testValidSchema` passes consistently across 20 consecutive runs for the input `"break schema"`.
- [ ] `TC-02 testMissingFieldBug` is **refactored** to assert `isNotNull()` once the bug is fixed (the sentinel assertion is reversed).

---

## Defect Summary Table

| Bug ID  | Title                                             | Severity | Priority | Status          | Sentinel Test        |
|---------|---------------------------------------------------|----------|----------|-----------------|----------------------|
| BUG-001 | `key_results` array exceeds 5 items for specific inputs | HIGH  | P1       | OPEN (Simulated)| TC-08 `testArrayViolationBug` |
| BUG-002 | `confidence_score` field absent from response         | CRITICAL | P0   | OPEN (Simulated)| TC-02 `testMissingFieldBug`   |

---

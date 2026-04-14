# AI Goal Coach – Automated Test Suite

---

## System Under Test

| Attribute        | Value                                       |
|------------------|---------------------------------------------|
| Protocol         | HTTP / JSON                                 |
| Endpoint         | `POST http://localhost:8002/api/coach`      |
| Request schema   | `{ "q": "<user goal string>" }`             |
| Response fields  | `refined_goal` (String), `key_results` (Array[3-5]), `confidence_score` (Integer 1-10) |
| Auth             | None (local Flask service)                  |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Test Execution Host                          │
│                                                                     │
│  ┌──────────────┐   HTTP POST /api/coach   ┌─────────────────────┐ │
│  │  Test Suite  │ ─────────────────────── ▶│  Flask AI Coach API │ │
│  │  (Maven /    │ ◀────────────────────── │  :8002               │ │
│  │   TestNG)    │   JSON Response           │                     │ │
│  └──────┬───────┘                           └─────────────────────┘ │
│         │                                                           │
│         │ REST Assured + AllureRestAssured filter                   │
│         ▼                                                           │
│  ┌──────────────┐                                                   │
│  │ Allure Result│  target/allure-results/*.json                     │
│  │   Files      │                                                   │
│  └──────┬───────┘                                                   │
│         │ mvn allure:serve  OR  allure generate                     │
│         ▼                                                           │
│  ┌──────────────┐                                                   │
│  │  Allure HTML │  Browsable at http://localhost:PORT               │
│  │   Report     │                                                   │
│  └──────────────┘                                                   │
└─────────────────────────────────────────────────────────────────────┘

Class Hierarchy
───────────────
BaseTest  (shared RestAssured config, HTTP helpers)
    └── GoalCoachApiTests  (12 test methods)

POJO Layer
──────────
RequestPayload   (Lombok @Builder → serialised by Jackson)
ResponsePayload  (Lombok @Data   ← deserialised by Jackson)
```

---

## Tech Stack

| Concern                | Library / Tool                  | Version  |
|------------------------|---------------------------------|----------|
| Language               | Java                            | 17       |
| Build                  | Apache Maven                    | 3.9+     |
| HTTP Client / DSL      | REST Assured                    | 5.4.0    |
| Test Runner            | TestNG                          | 7.9.0    |
| JSON Serialisation     | Jackson Databind                | 2.17.1   |
| Assertions             | AssertJ                         | 3.26.0   |
| Reporting              | Allure TestNG                   | 2.27.0   |
| AOP (Allure agent)     | AspectJ Weaver                  | 1.9.22   |
| Containerisation       | Docker + Docker Compose         | 24+      |

---

## Directory Structure

```
.
├── pom.xml                          ← Maven build descriptor
├── testng.xml                       ← TestNG suite (3 logical test groups)
├── Dockerfile                       ← Multi-stage build: deps → runner → report
├── docker-compose.yml               ← Orchestrates test-runner + allure-report services
├── README.md                        ← This file
├── TEST_STRATEGY.md                 ← High-level AI testing strategy document
├── BUGS.md                          ← Defect register (simulated bugs)
└── src/
    ├── main/java/com/aigoalcoach/
    │   └── models/
    │       ├── RequestPayload.java   ← { "q": "..." } – Lombok @Builder
    │       └── ResponsePayload.java  ← { refined_goal, key_results, confidence_score }
    └── test/java/com/aigoalcoach/
        ├── base/
        │   └── BaseTest.java        ← RestAssured config, post() helpers
        └── tests/
            └── GoalCoachApiTests.java  ← 12 TestNG test methods
```

---

## Prerequisites

### Local Execution

| Requirement            | Minimum Version | Verify                    |
|------------------------|-----------------|---------------------------|
| JDK                    | 17              | `java -version`           |
| Apache Maven           | 3.9             | `mvn -version`            |
| AI Coach Flask API     | running on 8002 | `curl http://localhost:8002/api/coach -X POST -H 'Content-Type: application/json' -d '{"q":"test"}'` |


### Allure CLI (optional – for local report serving)

```bash
# macOS
brew install allure

# npm (any OS)
npm install -g allure-commandline
```

---

## Local Execution

### 1. Start the SUT (Flask API)

Ensure the AI Goal Coach Flask service is running on port 8002 before running tests.

**Option A — with a virtual environment (recommended):**

```bash
cd sut-server
python3 -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
python3 app.py
```

**Option B — without a virtual environment (quick install):**

If you skip the venv, install via `pip3` and start with `python3` directly. The `flask` CLI may not be on your PATH, so avoid `flask run`:

```bash
cd sut-server
pip3 install -r requirements.txt
python3 app.py
```

The server listens on `http://127.0.0.1:8002`. Verify it is up before running the test suite:

```bash
curl -s -X POST http://localhost:8002/api/coach \
  -H "Content-Type: application/json" \
  -d '{"q":"upskill"}'
```

### 2. Run all tests

```bash
mvn clean test
```

### 3. Run a specific TestNG group

```bash
# Schema validation tests only
mvn clean test -Dgroups=schema-validation

# Business logic tests only
mvn clean test -Dgroups=business-logic

# Adversarial / edge case tests only
mvn clean test -Dgroups=adversarial
```


### 4. Run a single test by name

```bash
mvn clean test -Dtest=GoalCoachApiTests#testValidSchema
```

---

## Docker Execution

### Run tests only

```bash
docker compose up --build test-runner
```

Test results (Allure JSON) are written to the named volume `allure-results`.

### Run tests + generate and serve the report

```bash
docker compose up --build
```

Open [http://localhost:8080](http://localhost:8080) in your browser to view the Allure report.


---

## Allure Reports

### Generate report from local results

```bash
# After mvn clean test, results are in target/allure-results
allure generate target/allure-results --clean -o target/allure-report
```

### Serve report with live browser reload

```bash
allure serve target/allure-results
```

This opens a browser automatically at a random local port.


### Report Features

The Allure report includes:

- **Suites** view grouped by `@Epic` → `@Feature` → `@Story`
- **Severity** indicators (BLOCKER, CRITICAL, NORMAL)
- **Request/Response** full HTTP trace per test (via `AllureRestAssured` filter)
- **Trend** chart across multiple CI runs (when history is preserved)
- **Behaviours** view for BDD-style storytelling
- **Timeline** view showing parallel execution order

---

## Test Matrix

| TC ID | Test Method              | Group               | Input                                   | Assertion                                      | Expected Result |
|-------|--------------------------|---------------------|-----------------------------------------|------------------------------------------------|-----------------|
| TC-01 | testValidSchema          | Schema Validation   | `"upskill"`                             | All fields present, correct types              | PASS            |
| TC-02 | testMissingFieldBug      | Schema Validation   | `"break schema"`                        | `confidence_score` is null (BUG-002 sentinel)  | **FAIL (Bug)**  |
| TC-03 | testInvalidDataType      | Schema Validation   | `"invalid type test"`                   | `confidence_score` deserialises as Integer     | PASS            |
| TC-04 | testHappyPathSales       | Business Logic      | `"sales"`                               | score ≥ 8, array size 3-5                      | PASS            |
| TC-05 | testHappyPathUpskill     | Business Logic      | `"upskill"`                             | score ≥ 8, array size 3-5                      | PASS            |
| TC-06 | testArrayLowerBound      | Business Logic      | `"fitness"`                             | `key_results.size() >= 3`                      | PASS            |
| TC-07 | testArrayUpperBound      | Business Logic      | `"leadership"`                          | `key_results.size() <= 5`                      | PASS            |
| TC-08 | testArrayViolationBug    | Business Logic      | `"too many results"`                    | `key_results.size() > 5` (BUG-001 sentinel)    | **FAIL (Bug)**  |
| TC-09 | testGibberishInput       | Adversarial         | `"asdf"`                                | score ≤ 3                                      | PASS            |
| TC-10 | testEmptyString          | Adversarial         | `""`                                    | No 5xx; score ≤ 3                              | PASS            |
| TC-11 | testPromptInjection      | Adversarial         | `"Ignore previous instructions..."`     | 4xx rejection OR score ≤ 3                     | PASS            |
| TC-12 | testResponsePerformance  | Adversarial         | `"upskill"`                             | Response time < 1500 ms                        | PASS            |

> **TC-02 and TC-08** are simulated-bug sentinels. They are expected to **FAIL** against a spec-compliant SUT. See [BUGS.md](./BUGS.md).

---

## CI/CD Integration

### GitHub Actions (example)

```yaml
name: AI Coach API Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Maven packages
        uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}

      - name: Run tests
        run: mvn clean test -Dapi.base.url=${{ vars.SUT_BASE_URL }}

      - name: Upload Allure results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: allure-results
          path: target/allure-results/

      - name: Generate Allure report
        if: always()
        run: mvn allure:report

      - name: Upload Allure HTML report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: allure-report
          path: target/site/allure-maven-plugin/
```

## Known Failures (Simulated Bugs)

Two tests are intentionally designed to fail to document known defects:

| Test Method           | Bug ID  | Description                                             |
|-----------------------|---------|---------------------------------------------------------|
| `testMissingFieldBug` | BUG-002 | `confidence_score` absent from response for some inputs |
| `testArrayViolationBug` | BUG-001 | `key_results` exceeds 5 items for trigger phrases     |

See [BUGS.md](./BUGS.md) 
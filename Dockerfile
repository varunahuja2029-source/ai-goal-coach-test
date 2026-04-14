# ──────────────────────────────────────────────────────────────────────────────
# Stage 1 – dependency cache
# Downloads all Maven dependencies so subsequent builds are fast when source
# changes but pom.xml does not.
# ──────────────────────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS deps

WORKDIR /build

# Copy only the POM first to exploit Docker layer caching
COPY pom.xml .

# Download all dependencies (offline-friendly after first pull)
RUN mvn dependency:go-offline -B --no-transfer-progress

# ──────────────────────────────────────────────────────────────────────────────
# Stage 2 – test runner
# Copies source on top of the cached layer and runs the full test suite.
# ──────────────────────────────────────────────────────────────────────────────
FROM deps AS runner

WORKDIR /build

# Copy the rest of the project
COPY src ./src
COPY testng.xml .

# HOST is injected at runtime (see docker-compose.yml).
# Defaults to host.docker.internal so tests can reach the Flask API on the host.
ENV API_HOST=http://host.docker.internal:8002

# Run tests; results land in /build/target/allure-results
RUN mvn clean test -B --no-transfer-progress \
    -Dapi.base.url=${API_HOST} \
    || true   # keep container alive so reports can be extracted even on failure

# ──────────────────────────────────────────────────────────────────────────────
# Stage 3 – report server (optional convenience target)
# Serves the Allure HTML report on port 8080 so you can view it in a browser.
# Build with: docker build --target report -t aicoach-report .
# ──────────────────────────────────────────────────────────────────────────────
FROM runner AS report

RUN mvn allure:report -B --no-transfer-progress || true

# Minimal HTTP server to serve the static report
RUN apt-get update -qq && apt-get install -y --no-install-recommends python3 && \
    rm -rf /var/lib/apt/lists/*

EXPOSE 8080

CMD ["python3", "-m", "http.server", "8080", \
     "--directory", "/build/target/site/allure-maven-plugin"]

package com.aigoalcoach.base;

import com.aigoalcoach.models.RequestPayload;
import com.aigoalcoach.models.ResponsePayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.BeforeSuite;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;

/**
 * BaseTest – shared infrastructure for every test class.
 *
 * Responsibilities:
 *  1. Bootstrap REST Assured with the target base URI (resolved from the
 *     system property {@code api.base.url} or the environment variable
 *     {@code API_HOST}, falling back to {@code http://localhost:8002}).
 *  2. Configure a reusable {@link RequestSpecification} that attaches
 *     Allure-friendly request/response logging to every call.
 *  3. Expose a convenience {@link #post(RequestPayload)} helper that sends
 *     the coach request and returns the raw REST Assured {@link Response}.
 *  4. Expose a typed {@link #postAndDeserialise(RequestPayload)} variant that
 *     unmarshals the body into a {@link ResponsePayload}.
 */
public abstract class BaseTest {

    /** Shared Jackson mapper – reuse to avoid per-test overhead. */
    protected static final ObjectMapper MAPPER = new ObjectMapper();

    /** API path for the coach endpoint. */
    private static final String COACH_PATH = "/api/coach";

    /** Resolved base URL (system property → env var → default). */
    private static String baseUrl;

    /** Shared REST Assured request spec built once per suite. */
    protected static RequestSpecification requestSpec;

    // ── Suite-level setup ────────────────────────────────────────────────────

    @BeforeSuite(alwaysRun = true)
    public void configureSuite() {
        baseUrl = resolveBaseUrl();

        RestAssured.config = RestAssuredConfig.config()
                .objectMapperConfig(
                        ObjectMapperConfig.objectMapperConfig()
                                .jackson2ObjectMapperFactory((cls, charset) -> MAPPER));

        requestSpec = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                // Allure captures full request + response details for each test step
                .addFilter(new AllureRestAssured())
                // Console logging aids local debugging; suppressed in CI via log level
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();

        System.out.printf("[BaseTest] Suite configured – SUT base URL: %s%n", baseUrl);
    }

    // ── HTTP helpers ─────────────────────────────────────────────────────────

    /**
     * Posts the given payload to {@code /api/coach} and returns the raw
     * REST Assured {@link Response} so tests can inspect status, headers,
     * timing, and body independently.
     *
     * @param payload request body (serialised to JSON by REST Assured + Jackson)
     * @return raw HTTP response
     */
    protected Response post(RequestPayload payload) {
        return given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post(COACH_PATH)
                .then()
                .extract()
                .response();
    }

    /**
     * Posts the given payload and deserialises the 200-OK body directly into
     * a {@link ResponsePayload} POJO via Jackson.
     *
     * @param payload request body
     * @return deserialised response payload
     */
    protected ResponsePayload postAndDeserialise(RequestPayload payload) {
        return post(payload).as(ResponsePayload.class);
    }

    /**
     * Measures the round-trip response time in milliseconds.
     * Delegates to REST Assured's built-in timing measured from the moment
     * the request is sent until the full response body is received.
     *
     * @param payload request body
     * @return elapsed time in milliseconds
     */
    protected long measureResponseTimeMs(RequestPayload payload) {
        return post(payload).getTimeIn(TimeUnit.MILLISECONDS);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Resolves the SUT base URL with the following priority:
     * <ol>
     *   <li>JVM system property {@code api.base.url} (set by Maven Surefire or -D flag)</li>
     *   <li>Environment variable {@code API_HOST} (injected by Docker / CI)</li>
     *   <li>Hardcoded default {@code http://localhost:8002}</li>
     * </ol>
     */
    private static String resolveBaseUrl() {
        String fromProp = System.getProperty("api.base.url");
        if (fromProp != null && !fromProp.isBlank()) return fromProp;

        String fromEnv = System.getenv("API_HOST");
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv;

        return "http://localhost:8002";
    }
}

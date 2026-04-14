package com.aigoalcoach.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /api/coach.
 *
 * <pre>
 * {
 *   "q": "<user goal input>"
 * }
 * </pre>
 *
 * Lombok generates:
 *  - @Data       → getters, setters, equals, hashCode, toString
 *  - @Builder    → fluent builder (RequestPayload.builder().q("upskill").build())
 *  - @NoArgsConstructor / @AllArgsConstructor → required by Jackson & TestNG data providers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestPayload {

    /**
     * The raw, potentially vague user goal that the AI Coach will refine.
     * Maps to the "q" key in the JSON request body.
     */
    @JsonProperty("q")
    private String q;
}

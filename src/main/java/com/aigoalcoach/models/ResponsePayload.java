package com.aigoalcoach.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Deserialised response body from POST /api/coach.
 *
 * <pre>
 * {
 *   "refined_goal"    : "String",
 *   "key_results"     : ["String", ...],   // 3-5 items per contract
 *   "confidence_score": Integer            // 1-10 inclusive
 * }
 * </pre>
 *
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} tolerates forward-compatible
 * fields added by the SUT without breaking deserialisation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponsePayload {

    /**
     * AI-generated, actionable version of the user's raw goal.
     */
    @JsonProperty("refined_goal")
    private String refinedGoal;

    /**
     * Ordered list of measurable key results that operationalise the refined goal.
     * Contract: 3 ≤ size ≤ 5.
     */
    @JsonProperty("key_results")
    private List<String> keyResults;

    /**
     * Model confidence that the produced plan is coherent and achievable.
     * Contract: Integer in range [1, 10].
     */
    @JsonProperty("confidence_score")
    private Integer confidenceScore;
}

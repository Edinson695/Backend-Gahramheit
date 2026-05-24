package com.example.gahramheit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // <-- AGREGA ESTO
public class JikanReviewResponse {
    private List<ReviewData> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true) // <-- AGREGA ESTO
    public static class ReviewData {
        private Integer score;
        @JsonProperty("review")
        private String comment;
    }
}
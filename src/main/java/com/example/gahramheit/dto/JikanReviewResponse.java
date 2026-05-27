package com.example.gahramheit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class JikanReviewResponse {
    private List<ReviewData> data;

    @Data
    public static class ReviewData {
        private Integer score;
        // En Jikan el texto de la reseña viene como "review", lo mapeamos a tu variable "comment"
        @JsonProperty("review")
        private String comment;
    }
}
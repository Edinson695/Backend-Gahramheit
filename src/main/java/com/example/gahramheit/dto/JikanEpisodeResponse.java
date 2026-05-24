package com.example.gahramheit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // <-- AGREGA ESTO
public class JikanEpisodeResponse {
    private List<EpisodeData> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true) // <-- AGREGA ESTO
    public static class EpisodeData {
        @JsonProperty("mal_id")
        private Integer episodeNumber;
        private String title;
    }
}
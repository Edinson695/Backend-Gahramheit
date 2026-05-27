package com.example.gahramheit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class JikanEpisodeResponse {
    private List<EpisodeData> data;

    @Data
    public static class EpisodeData {
        // En Jikan el número del episodio viene como mal_id, lo mapeamos a tu variable
        @JsonProperty("mal_id")
        private Integer episodeNumber;
        private String title;
    }
}
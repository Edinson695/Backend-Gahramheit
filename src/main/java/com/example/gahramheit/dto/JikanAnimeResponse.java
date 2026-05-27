package com.example.gahramheit.dto;

import lombok.Data;
import java.util.List;

@Data
public class JikanAnimeResponse {

    private AnimeData data;

    @Data
    public static class AnimeData {
        // Atrapa el año de lanzamiento
        private Integer year;

        // Atrapa la lista de estudios
        private List<Studio> studios;

        private String status;
    }

    @Data
    public static class Studio {
        // Solo necesitamos el nombre del estudio
        private String name;
    }
}
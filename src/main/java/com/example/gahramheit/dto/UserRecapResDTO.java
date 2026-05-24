package com.example.gahramheit.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRecapResDTO {
    private Integer anio;
    private Integer totalEpisodiosVistos;
    private Long tiempoTotalMinutos;
    private String generoFavorito;
    private TopAnime animeMejorCalificado;
    private String insigniaDestacadaAnual;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopAnime {
        private Long id;
        private String title;
        private Integer score;
    }
}
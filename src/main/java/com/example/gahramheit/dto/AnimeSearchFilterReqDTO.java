package com.example.gahramheit.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnimeSearchFilterReqDTO {
    private String query; // Búsqueda por texto
    private String type;  // TV, Movie, OVA
    private Integer genreId;
}
package com.example.gahramheit.dto;

import lombok.*;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnimeDTO {
    private Long id;
    private String title;
    private Integer malId;
    private Integer episodesCount;
    private String imageUrl;
    private Set<String> genreNames;
}


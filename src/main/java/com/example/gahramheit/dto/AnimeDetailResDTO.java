package com.example.gahramheit.dto;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnimeDetailResDTO {
    private Long id;
    private Integer malId;
    private String title;
    private String synopsis;
    private Integer episodesCount;
    private String imageUrl;
    private String studio;
    private String director;
    private Integer releaseYear;
    private List<String> genres;
    private List<String> actoresVoz;
}
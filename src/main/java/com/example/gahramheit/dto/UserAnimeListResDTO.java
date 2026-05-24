package com.example.gahramheit.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAnimeListResDTO {
    private Long animeId;
    private String title;
    private String imageUrl;
    private AnimeStatus status;
    private Integer currentEpisode;
    private Integer episodesCount;
}
package com.example.gahramheit.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnimeCardResDTO {
    private Long id; 
    private Integer malId; // ID de Jikan API
    private String title;
    private String imageUrl;
}
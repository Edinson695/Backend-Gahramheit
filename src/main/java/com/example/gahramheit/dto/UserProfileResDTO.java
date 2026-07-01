package com.example.gahramheit.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResDTO {
    private Long id;
    private String username;
    private String role;
    private String rango;
    private Integer episodiosVistos;
    private Integer animesCompletados;
    private String logrosDesbloqueados;
    private List<AchievementResDTO> logros;
}
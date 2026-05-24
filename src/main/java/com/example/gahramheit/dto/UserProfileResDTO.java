package com.example.gahramheit.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResDTO {
    private Long id;
    private String username;
    private String rango; // Ej: "Otaku en formación"
    private Integer episodiosVistos;
    private Integer animesCompletados;
    private String logrosDesbloqueados; // Ej: "0/6"
}
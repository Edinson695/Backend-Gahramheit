package com.example.gahramheit.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserAnimeListReqDTO {
    @NotNull(message = "El ID del anime es mandatorio.")
    private Long animeId;

    @NotNull(message = "El estado de seguimiento es requerido.")
    private AnimeStatus status;

    @Min(value = 0, message = "El episodio actual no puede ser menor a 0.")
    private Integer currentEpisode;
}
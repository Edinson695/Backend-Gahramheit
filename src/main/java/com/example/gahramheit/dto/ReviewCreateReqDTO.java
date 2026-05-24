package com.example.gahramheit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCreateReqDTO {
    @NotNull(message = "El ID del anime es obligatorio.")
    private Long animeId;

    @Min(value = 1, message = "La calificación mínima es 1 estrella.")
    @Max(value = 5, message = "La calificación máxima es 5 estrellas.")
    private Integer score;

    private String comment;
}
package com.example.gahramheit.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAnimeListId implements Serializable {
    private Long userId;
    private Long animeId;
}


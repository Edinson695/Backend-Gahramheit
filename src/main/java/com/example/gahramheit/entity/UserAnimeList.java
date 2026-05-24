package com.example.gahramheit.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_anime_list")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAnimeList {

    @EmbeddedId
    private UserAnimeListId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("animeId")
    @JoinColumn(name = "anime_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Anime anime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "current_episode")
    private Integer currentEpisode;
}


package com.example.gahramheit.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "episodes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Episode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Anime anime;

    @Column(name = "episode_number", nullable = false)
    private Integer episodeNumber;

    private String title;
}


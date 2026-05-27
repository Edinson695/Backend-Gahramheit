package com.example.gahramheit.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "animes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Anime {
    @Id
    private Long id;

    @NotBlank(message = "El título del anime no puede estar vacío")
    @Column(nullable = false)
    private String title;

    @Column(name = "episodes_count")
    private Integer episodesCount;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    private String studio;

    private String director;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "voice_actors")
    private String voiceActors;

    @Column(length = 100)
    private String status;

    @OneToMany(mappedBy = "anime", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Episode> episodes = new ArrayList<>();

    @OneToMany(mappedBy = "anime", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Review> reviews = new HashSet<>();

    @OneToMany(mappedBy = "anime", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<UserAnimeList> userAnimeLists = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "anime_genres",
            joinColumns = @JoinColumn(name = "anime_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Genre> genres = new HashSet<>();

    @OneToMany(mappedBy = "anime", cascade = CascadeType.ALL, orphanRemoval = true) // Si lo pones en User, cambia "anime" por "user"
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Comment> comments = new ArrayList<>();
}


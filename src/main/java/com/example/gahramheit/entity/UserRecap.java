package com.example.gahramheit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_recaps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRecap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "recap_year", nullable = false)
    private Integer year;

    @Column(name = "total_episodes_watched")
    private Integer totalEpisodesWatched;

    @Column(name = "total_time_minutes")
    private Long totalTimeMinutes;

    @Column(name = "top_genre", length = 50)
    private String topGenre;

    @Column(name = "top_5_animes", columnDefinition = "TEXT")
    private String top5Animes;

    @Column(name = "average_score")
    private Double averageScore;

    @Column(name = "favorite_anime_id")
    private Long favoriteAnimeId;

    @Column(name = "favorite_anime_title", length = 255)
    private String favoriteAnimeTitle;

    @Column(name = "favorite_anime_score")
    private Integer favoriteAnimeScore;

    @Column(name = "badge_earned", length = 100)
    private String badgeEarned;

    @Column(name = "ai_personalized_message", columnDefinition = "TEXT")
    private String aiPersonalizedMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

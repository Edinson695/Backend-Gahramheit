package com.example.gahramheit.repository;

import com.example.gahramheit.entity.Anime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnimeRepository extends JpaRepository<Anime, Long> {
    List<Anime> findByTitleContainingIgnoreCase(String title);
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT g.name FROM user_anime_list ual " +
                    "JOIN anime_genre ag ON ual.anime_id = ag.anime_id " +
                    "JOIN genres g ON ag.genre_id = g.id " +
                    "WHERE ual.user_id = :userId " +
                    "GROUP BY g.name " +
                    "ORDER BY COUNT(g.name) DESC LIMIT 1",
            nativeQuery = true)
    String getMostWatchedGenreByUser(@org.springframework.data.repository.query.Param("userId") Long userId);

}


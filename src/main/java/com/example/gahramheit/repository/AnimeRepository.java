package com.example.gahramheit.repository;

import com.example.gahramheit.entity.Anime;
import com.example.gahramheit.entity.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnimeRepository extends JpaRepository<Anime, Long> {
    List<Anime> findByTitleContainingIgnoreCase(String title);
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT g.name FROM user_anime_list ual " +
                    "JOIN anime_genres ag ON ual.anime_id = ag.anime_id " +
                    "JOIN genres g ON ag.genre_id = g.id " +
                    "WHERE ual.user_id = :userId " +
                    "GROUP BY g.name " +
                    "ORDER BY COUNT(g.name) DESC LIMIT 1",
            nativeQuery = true)
    String getMostWatchedGenreByUser(@org.springframework.data.repository.query.Param("userId") Long userId);

    List<Anime> findByStudioIsNullOrReleaseYearIsNull();

    Page<Anime> findByGenres_NameIgnoreCase(String name, Pageable pageable);

    // EL SUPER QUERY CORREGIDO
    @Query("SELECT DISTINCT a FROM Anime a LEFT JOIN a.genres g " +
            "WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "AND (:genre IS NULL OR g.name = :genre)")
    Page<Anime> findWithFilters(@Param("keyword") String keyword,
                                @Param("genre") String genre,
                                Pageable pageable);
}


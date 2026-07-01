package com.example.gahramheit.repository;

import com.example.gahramheit.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByAnime_Id(Long animeId);

    @Query("SELECT ROUND(AVG(r.score), 1) FROM Review r WHERE r.user.id = :userId")
    Double getAverageScoreByUser(@Param("userId") Long userId);

    @Query("SELECT r.anime.id, r.anime.title, r.score FROM Review r " +
           "WHERE r.user.id = :userId ORDER BY r.score DESC")
    List<Object[]> findTopAnimeByUser(@Param("userId") Long userId);

    long countByUser_Id(Long userId);
}


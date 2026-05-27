package com.example.gahramheit.repository;

import com.example.gahramheit.entity.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, Long> {
    List<Episode> findByAnime_IdOrderByEpisodeNumberAsc(Long animeId);
}


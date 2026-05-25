package com.example.gahramheit.event;

import lombok.Getter;

@Getter
public class AnimeReviewedEvent {
    private final Long animeId;
    private final String username;
    private final Integer score;

    public AnimeReviewedEvent(Long animeId, String username, Integer score) {
        this.animeId = animeId;
        this.username = username;
        this.score = score;
    }
}
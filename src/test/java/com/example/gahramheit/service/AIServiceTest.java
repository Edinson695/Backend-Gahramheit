package com.example.gahramheit.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AIServiceTest {

    private final AIService aiService = new AIService();

    @Test
    void shouldGenerateGenerousProfileWhenAverageScoreIsHigh() {
        String profile = aiService.generateOtakuProfile("john", "Action", 9.4);

        assertThat(profile).contains("john", "Action", "9.4/10", "sumamente generoso");
    }

    @Test
    void shouldGenerateBalancedProfileWhenAverageScoreIsMedium() {
        String profile = aiService.generateOtakuProfile("john", "Drama", 7.5);

        assertThat(profile).contains("Drama", "gusto equilibrado");
    }

    @Test
    void shouldUseDefaultGenreWhenTopGenreIsMissing() {
        String profile = aiService.generateOtakuProfile("john", null, 6.0);

        assertThat(profile).contains("Misterio", "implacable");
    }
}

package com.example.gahramheit.repository;

import com.example.gahramheit.entity.Anime;
import com.example.gahramheit.entity.Episode;
import com.example.gahramheit.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EpisodeRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private AnimeRepository animeRepository;

    @Test
    void shouldSaveEpisodeWhenAnimeExists() {
        Anime anime = animeRepository.saveAndFlush(createAnime("Solo Leveling"));
        Episode episode = createEpisode(anime, 1, "I'm Used to It");

        Episode savedEpisode = episodeRepository.saveAndFlush(episode);

        assertThat(savedEpisode.getId()).isNotNull();
        assertThat(savedEpisode.getAnime().getId()).isEqualTo(anime.getId());
        assertThat(savedEpisode.getEpisodeNumber()).isEqualTo(1);
    }

    @Test
    void shouldFindEpisodeWhenEpisodeExists() {
        Anime anime = animeRepository.saveAndFlush(createAnime("Mob Psycho 100"));
        Episode savedEpisode = episodeRepository.saveAndFlush(createEpisode(anime, 5, "Ochimusha"));

        Optional<Episode> foundEpisode = episodeRepository.findById(savedEpisode.getId());

        assertThat(foundEpisode).isPresent();
        assertThat(foundEpisode.get().getTitle()).isEqualTo("Ochimusha");
    }

    @Test
    void shouldRejectEpisodeWhenAnimeIsMissing() {
        Episode episode = createEpisode(null, 1, "Invalid Episode");

        assertThatThrownBy(() -> episodeRepository.saveAndFlush(episode))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectEpisodeWhenEpisodeNumberIsMissing() {
        Anime anime = animeRepository.saveAndFlush(createAnime("One Punch Man"));
        Episode episode = createEpisode(anime, null, "Invalid Episode");

        assertThatThrownBy(() -> episodeRepository.saveAndFlush(episode))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Anime createAnime(String title) {
        Anime anime = new Anime();
        anime.setTitle(title);
        anime.setMalId(100);
        anime.setEpisodesCount(12);
        anime.setImageUrl("https://example.com/anime.jpg");
        return anime;
    }

    private Episode createEpisode(Anime anime, Integer episodeNumber, String title) {
        Episode episode = new Episode();
        episode.setAnime(anime);
        episode.setEpisodeNumber(episodeNumber);
        episode.setTitle(title);
        return episode;
    }
}

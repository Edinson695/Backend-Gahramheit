package com.example.gahramheit.repository;

import com.example.gahramheit.entity.Anime;
import com.example.gahramheit.entity.Genre;
import com.example.gahramheit.support.AbstractRepositoryTest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnimeRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private AnimeRepository animeRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Test
    void shouldSaveAnimeWhenAnimeIsValid() {
        Anime anime = createAnime("Frieren", 52991, 28);

        Anime savedAnime = animeRepository.saveAndFlush(anime);

        assertThat(savedAnime.getId()).isNotNull();
        assertThat(savedAnime.getTitle()).isEqualTo("Frieren");
        assertThat(savedAnime.getMalId()).isEqualTo(52991);
    }

    @Test
    void shouldUpdateAnimeWhenAnimeExists() {
        Anime savedAnime = animeRepository.saveAndFlush(createAnime("Old Title", 1, 12));

        savedAnime.setTitle("Updated Title");
        savedAnime.setEpisodesCount(24);
        Anime updatedAnime = animeRepository.saveAndFlush(savedAnime);

        assertThat(updatedAnime.getTitle()).isEqualTo("Updated Title");
        assertThat(updatedAnime.getEpisodesCount()).isEqualTo(24);
    }

    @Test
    void shouldDeleteAnimeWhenAnimeExists() {
        Anime savedAnime = animeRepository.saveAndFlush(createAnime("Delete Me", 2, 10));

        animeRepository.delete(savedAnime);
        animeRepository.flush();

        assertThat(animeRepository.findById(savedAnime.getId())).isEmpty();
    }

    @Test
    void shouldRejectAnimeWhenTitleIsBlank() {
        Anime anime = createAnime("", 3, 12);

        assertThatThrownBy(() -> animeRepository.saveAndFlush(anime))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldPersistAnimeGenreRelationWhenAnimeHasGenres() {
        Genre genre = genreRepository.saveAndFlush(createGenre("Adventure"));
        Anime anime = createAnime("Dungeon Meshi", 52701, 24);
        anime.getGenres().add(genre);

        Anime savedAnime = animeRepository.saveAndFlush(anime);
        Optional<Anime> foundAnime = animeRepository.findById(savedAnime.getId());

        assertThat(foundAnime).isPresent();
        assertThat(foundAnime.get().getGenres())
                .extracting(Genre::getName)
                .containsExactly("Adventure");
    }

    private Anime createAnime(String title, Integer malId, Integer episodesCount) {
        Anime anime = new Anime();
        anime.setTitle(title);
        anime.setMalId(malId);
        anime.setEpisodesCount(episodesCount);
        anime.setImageUrl("https://example.com/" + malId + ".jpg");
        return anime;
    }

    private Genre createGenre(String name) {
        Genre genre = new Genre();
        genre.setName(name);
        return genre;
    }
}

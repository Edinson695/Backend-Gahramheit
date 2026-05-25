package com.example.gahramheit.repository;

import com.example.gahramheit.entity.Genre;
import com.example.gahramheit.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenreRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private GenreRepository genreRepository;

    @Test
    void shouldSaveGenreWhenNameIsValid() {
        Genre genre = createGenre("Shonen");

        Genre savedGenre = genreRepository.saveAndFlush(genre);

        assertThat(savedGenre.getId()).isNotNull();
        assertThat(savedGenre.getName()).isEqualTo("Shonen");
    }

    @Test
    void shouldRejectDuplicateGenreWhenNameAlreadyExists() {
        genreRepository.saveAndFlush(createGenre("Drama"));

        Genre duplicateGenre = createGenre("Drama");

        assertThatThrownBy(() -> genreRepository.saveAndFlush(duplicateGenre))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectGenreWhenNameIsMissing() {
        Genre genre = createGenre(null);

        assertThatThrownBy(() -> genreRepository.saveAndFlush(genre))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Genre createGenre(String name) {
        Genre genre = new Genre();
        genre.setName(name);
        return genre;
    }
}

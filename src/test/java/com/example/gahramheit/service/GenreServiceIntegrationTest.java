package com.example.gahramheit.service;

import com.example.gahramheit.dto.GenreDTO;
import com.example.gahramheit.exception.DuplicateResourceException;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.repository.GenreRepository;
import com.example.gahramheit.support.AbstractPostgresContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class GenreServiceIntegrationTest extends AbstractPostgresContainerTest {

    @Autowired
    private GenreService genreService;

    @Autowired
    private GenreRepository genreRepository;

    @Test
    void shouldPersistGenreWhenGenreIsCreated() {
        GenreDTO created = genreService.createGenre(new GenreDTO(null, "Mystery Integration"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Mystery Integration");
        assertThat(genreRepository.findByName("Mystery Integration")).isPresent();
    }

    @Test
    void shouldThrowDuplicateResourceExceptionWhenGenreNameAlreadyExists() {
        genreService.createGenre(new GenreDTO(null, "Drama Integration"));

        assertThatThrownBy(() -> genreService.createGenre(new GenreDTO(null, "Drama Integration")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Drama Integration");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenGenreDoesNotExist() {
        assertThatThrownBy(() -> genreService.getGenreById(999_999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999999");
    }
}

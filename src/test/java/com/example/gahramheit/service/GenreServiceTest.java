package com.example.gahramheit.service;

import com.example.gahramheit.dto.GenreDTO;
import com.example.gahramheit.entity.Genre;
import com.example.gahramheit.exception.DuplicateResourceException;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.repository.GenreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenreServiceTest {

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private GenreService genreService;

    @Test
    void shouldReturnAllGenresWhenGenresExist() {
        Genre action = createGenre(1L, "Action");
        Genre drama = createGenre(2L, "Drama");
        when(genreRepository.findAll()).thenReturn(List.of(action, drama));
        when(modelMapper.map(action, GenreDTO.class)).thenReturn(new GenreDTO(1L, "Action"));
        when(modelMapper.map(drama, GenreDTO.class)).thenReturn(new GenreDTO(2L, "Drama"));

        List<GenreDTO> genres = genreService.getAllGenres();

        assertThat(genres)
                .extracting(GenreDTO::getName)
                .containsExactly("Action", "Drama");
    }

    @Test
    void shouldReturnGenreWhenGenreExists() {
        Genre genre = createGenre(1L, "Seinen");
        when(genreRepository.findById(1L)).thenReturn(Optional.of(genre));
        when(modelMapper.map(genre, GenreDTO.class)).thenReturn(new GenreDTO(1L, "Seinen"));

        GenreDTO result = genreService.getGenreById(1L);

        assertThat(result.getName()).isEqualTo("Seinen");
    }

    @Test
    void shouldThrowResourceNotFoundWhenGenreDoesNotExist() {
        when(genreRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> genreService.getGenreById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Genre not found with id: 99");
    }

    @Test
    void shouldCreateGenreWhenNameIsUnique() {
        GenreDTO request = new GenreDTO(null, "Comedy");
        when(genreRepository.findByName("Comedy")).thenReturn(Optional.empty());
        when(modelMapper.map(any(Genre.class), org.mockito.ArgumentMatchers.eq(GenreDTO.class)))
                .thenAnswer(invocation -> {
                    Genre genre = invocation.getArgument(0);
                    return new GenreDTO(genre.getId(), genre.getName());
                });

        GenreDTO result = genreService.createGenre(request);

        assertThat(result.getName()).isEqualTo("Comedy");
        verify(genreRepository).save(any(Genre.class));
    }

    @Test
    void shouldThrowDuplicateResourceWhenGenreNameAlreadyExists() {
        when(genreRepository.findByName("Drama")).thenReturn(Optional.of(createGenre(1L, "Drama")));

        assertThatThrownBy(() -> genreService.createGenre(new GenreDTO(null, "Drama")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Genre already exists: Drama");
        verify(genreRepository, never()).save(any(Genre.class));
    }

    @Test
    void shouldUpdateGenreWhenGenreExists() {
        Genre genre = createGenre(1L, "Old");
        when(genreRepository.findById(1L)).thenReturn(Optional.of(genre));
        when(modelMapper.map(genre, GenreDTO.class)).thenReturn(new GenreDTO(1L, "Updated"));

        GenreDTO result = genreService.updateGenre(1L, new GenreDTO(null, "Updated"));

        assertThat(result.getName()).isEqualTo("Updated");
        verify(genreRepository).save(genre);
    }

    @Test
    void shouldDeleteGenreWhenGenreExists() {
        Genre genre = createGenre(1L, "Delete");
        when(genreRepository.findById(1L)).thenReturn(Optional.of(genre));

        genreService.deleteGenre(1L);

        verify(genreRepository).delete(genre);
    }

    private Genre createGenre(Long id, String name) {
        Genre genre = new Genre();
        genre.setId(id);
        genre.setName(name);
        return genre;
    }
}

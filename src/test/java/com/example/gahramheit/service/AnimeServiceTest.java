package com.example.gahramheit.service;

import com.example.gahramheit.dto.AnimeDTO;
import com.example.gahramheit.dto.AnimeDetailResDTO;
import com.example.gahramheit.entity.Anime;
import com.example.gahramheit.entity.Genre;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.repository.AnimeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimeServiceTest {

    @Mock
    private AnimeRepository animeRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AnimeService animeService;

    @Test
    void shouldReturnAnimeCatalogWhenPageExists() {
        Anime anime = createAnime(1L, "Frieren", "Adventure");
        when(animeRepository.findAll(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(anime), PageRequest.of(0, 10), 1));
        when(modelMapper.map(anime, AnimeDTO.class)).thenReturn(AnimeDTO.builder()
                .id(1L)
                .title("Frieren")
                .build());

        Page<AnimeDTO> catalog = animeService.getAnimeCatalog(0, 10);

        assertThat(catalog.getContent()).hasSize(1);
        assertThat(catalog.getContent().get(0).getGenreNames()).containsExactly("Adventure");
    }

    @Test
    void shouldSearchAnimesWhenKeywordMatchesTitle() {
        Anime anime = createAnime(1L, "Mob Psycho 100", "Action");
        when(animeRepository.findByTitleContainingIgnoreCase("mob")).thenReturn(List.of(anime));
        when(modelMapper.map(anime, AnimeDTO.class)).thenReturn(AnimeDTO.builder()
                .id(1L)
                .title("Mob Psycho 100")
                .build());

        List<AnimeDTO> animes = animeService.searchAnimesByTitle("mob");

        assertThat(animes).hasSize(1);
        assertThat(animes.get(0).getGenreNames()).containsExactly("Action");
    }

    @Test
    void shouldReturnAnimeDetailsWhenAnimeExists() {
        Anime anime = createAnime(1L, "Violet Evergarden", "Drama");
        anime.setActoresVoz(new ArrayList<>(List.of("Yui Ishikawa")));
        when(animeRepository.findById(1L)).thenReturn(Optional.of(anime));
        when(modelMapper.map(anime, AnimeDetailResDTO.class)).thenReturn(AnimeDetailResDTO.builder()
                .id(1L)
                .title("Violet Evergarden")
                .build());

        AnimeDetailResDTO result = animeService.getAnimeDetails(1L);

        assertThat(result.getGenres()).containsExactly("Drama");
        assertThat(result.getActoresVoz()).containsExactly("Yui Ishikawa");
    }

    @Test
    void shouldReturnEmptyVoiceActorsWhenAnimeHasNoVoiceActors() {
        Anime anime = createAnime(1L, "No Actors Anime", "Mystery");
        anime.setActoresVoz(null);
        when(animeRepository.findById(1L)).thenReturn(Optional.of(anime));
        when(modelMapper.map(anime, AnimeDetailResDTO.class)).thenReturn(AnimeDetailResDTO.builder()
                .id(1L)
                .title("No Actors Anime")
                .build());

        AnimeDetailResDTO result = animeService.getAnimeDetails(1L);

        assertThat(result.getActoresVoz()).isEmpty();
    }

    @Test
    void shouldThrowResourceNotFoundWhenAnimeDoesNotExist() {
        when(animeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> animeService.getAnimeDetails(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Anime not found with id: 99");
    }

    private Anime createAnime(Long id, String title, String genreName) {
        Anime anime = new Anime();
        anime.setId(id);
        anime.setTitle(title);
        Genre genre = new Genre();
        genre.setId(id);
        genre.setName(genreName);
        anime.setGenres(new HashSet<>(List.of(genre)));
        return anime;
    }
}

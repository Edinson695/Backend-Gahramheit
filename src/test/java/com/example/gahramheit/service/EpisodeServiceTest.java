package com.example.gahramheit.service;

import com.example.gahramheit.dto.EpisodeDTO;
import com.example.gahramheit.entity.Anime;
import com.example.gahramheit.entity.Episode;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.repository.AnimeRepository;
import com.example.gahramheit.repository.EpisodeRepository;
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
class EpisodeServiceTest {

    @Mock
    private EpisodeRepository episodeRepository;

    @Mock
    private AnimeRepository animeRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private EpisodeService episodeService;

    @Test
    void shouldReturnEpisodesWhenAnimeExists() {
        Anime anime = createAnime(1L, "Frieren");
        Episode firstEpisode = createEpisode(10L, anime, 1, "The Journey's End");
        Episode secondEpisode = createEpisode(11L, anime, 2, "It Didn't Have to Be Magic");
        when(animeRepository.existsById(1L)).thenReturn(true);
        when(episodeRepository.findByAnimeIdOrderByEpisodeNumberAsc(1L))
                .thenReturn(List.of(firstEpisode, secondEpisode));
        mockEpisodeMapping(firstEpisode);
        mockEpisodeMapping(secondEpisode);

        List<EpisodeDTO> episodes = episodeService.getEpisodesByAnime(1L);

        assertThat(episodes)
                .extracting(EpisodeDTO::getEpisodeNumber)
                .containsExactly(1, 2);
    }

    @Test
    void shouldThrowResourceNotFoundWhenAnimeDoesNotExist() {
        when(animeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> episodeService.getEpisodesByAnime(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Anime not found with id: 99");
        verify(episodeRepository, never()).findByAnimeIdOrderByEpisodeNumberAsc(99L);
    }

    @Test
    void shouldReturnEpisodeWhenEpisodeExists() {
        Anime anime = createAnime(1L, "Frieren");
        Episode episode = createEpisode(10L, anime, 1, "The Journey's End");
        when(episodeRepository.findById(10L)).thenReturn(Optional.of(episode));
        mockEpisodeMapping(episode);

        EpisodeDTO result = episodeService.getEpisodeById(10L);

        assertThat(result.getAnimeId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("The Journey's End");
    }

    @Test
    void shouldCreateEpisodeWhenAnimeExists() {
        Anime anime = createAnime(1L, "Frieren");
        EpisodeDTO request = EpisodeDTO.builder()
                .episodeNumber(3)
                .title("Killing Magic")
                .build();
        when(animeRepository.findById(1L)).thenReturn(Optional.of(anime));
        when(modelMapper.map(any(Episode.class), org.mockito.ArgumentMatchers.eq(EpisodeDTO.class)))
                .thenAnswer(invocation -> {
                    Episode episode = invocation.getArgument(0);
                    return EpisodeDTO.builder()
                            .id(episode.getId())
                            .episodeNumber(episode.getEpisodeNumber())
                            .title(episode.getTitle())
                            .build();
                });

        EpisodeDTO result = episodeService.createEpisode(1L, request);

        assertThat(result.getAnimeId()).isEqualTo(1L);
        assertThat(result.getEpisodeNumber()).isEqualTo(3);
        verify(episodeRepository).save(any(Episode.class));
    }

    @Test
    void shouldThrowResourceNotFoundWhenCreatingEpisodeForMissingAnime() {
        when(animeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> episodeService.createEpisode(99L, new EpisodeDTO()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Anime not found with id: 99");
        verify(episodeRepository, never()).save(any(Episode.class));
    }

    private void mockEpisodeMapping(Episode episode) {
        when(modelMapper.map(episode, EpisodeDTO.class)).thenReturn(EpisodeDTO.builder()
                .id(episode.getId())
                .episodeNumber(episode.getEpisodeNumber())
                .title(episode.getTitle())
                .build());
    }

    private Anime createAnime(Long id, String title) {
        Anime anime = new Anime();
        anime.setId(id);
        anime.setTitle(title);
        return anime;
    }

    private Episode createEpisode(Long id, Anime anime, Integer episodeNumber, String title) {
        Episode episode = new Episode();
        episode.setId(id);
        episode.setAnime(anime);
        episode.setEpisodeNumber(episodeNumber);
        episode.setTitle(title);
        return episode;
    }
}

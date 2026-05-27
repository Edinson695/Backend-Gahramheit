package com.example.gahramheit.service;

import com.example.gahramheit.dto.EpisodeDTO;
import com.example.gahramheit.entity.Anime;
import com.example.gahramheit.entity.Episode;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.repository.AnimeRepository;
import com.example.gahramheit.repository.EpisodeRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EpisodeService {

    private final EpisodeRepository episodeRepository;
    private final AnimeRepository animeRepository;
    private final ModelMapper modelMapper;

    public List<EpisodeDTO> getEpisodesByAnime(Long animeId) {
        if (!animeRepository.existsById(animeId)) {
            throw new ResourceNotFoundException("Anime not found with id: " + animeId);
        }

        return episodeRepository.findByAnime_IdOrderByEpisodeNumberAsc(animeId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public EpisodeDTO getEpisodeById(Long id) {
        Episode episode = episodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Episode not found with id: " + id));
        return toDto(episode);
    }

    public EpisodeDTO createEpisode(Long animeId, EpisodeDTO request) {
        Anime anime = animeRepository.findById(animeId)
                .orElseThrow(() -> new ResourceNotFoundException("Anime not found with id: " + animeId));

        Episode episode = Episode.builder()
                .anime(anime)
                .episodeNumber(request.getEpisodeNumber())
                .title(request.getTitle())
                .build();

        episodeRepository.save(episode);
        return toDto(episode);
    }

    private EpisodeDTO toDto(Episode episode) {
        EpisodeDTO dto = modelMapper.map(episode, EpisodeDTO.class);
        dto.setAnimeId(episode.getAnime().getId());
        return dto;
    }
}

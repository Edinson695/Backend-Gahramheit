package com.example.gahramheit.service;

import com.example.gahramheit.dto.AnimeStatus;
import com.example.gahramheit.dto.UpdateUserAnimeListReqDTO;
import com.example.gahramheit.dto.UserAnimeListResDTO;
import com.example.gahramheit.entity.*;
import com.example.gahramheit.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.example.gahramheit.repository.AnimeRepository;
import com.example.gahramheit.repository.UserAnimeListRepository;
import com.example.gahramheit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAnimeListService {

    private final UserAnimeListRepository userAnimeListRepository;
    private final UserRepository userRepository;
    private final AnimeRepository animeRepository;

    public List<UserAnimeListResDTO> getUserList(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        return userAnimeListRepository.findByUser_Id(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserAnimeListResDTO updateAnimeInList(Long userId, UpdateUserAnimeListReqDTO request) {
        verifyOwnership(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Anime anime = animeRepository.findById(request.getAnimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Anime not found with id: " + request.getAnimeId()));

        UserAnimeListId id = new UserAnimeListId(userId, request.getAnimeId());

        UserAnimeList userAnimeList = userAnimeListRepository.findById(id)
                .orElseGet(() -> {
                    UserAnimeList newEntry = new UserAnimeList();
                    newEntry.setId(id);
                    newEntry.setUser(user);
                    newEntry.setAnime(anime);
                    return newEntry;
                });

        userAnimeList.setStatus(toEntityStatus(request.getStatus()));

        if (request.getCurrentEpisode() != null) {
            userAnimeList.setCurrentEpisode(request.getCurrentEpisode());
        }

        userAnimeListRepository.save(userAnimeList);
        return toDto(userAnimeList);
    }

    @Transactional
    public void removeFromList(Long userId, Long animeId) {
        verifyOwnership(userId);

        UserAnimeListId id = new UserAnimeListId(userId, animeId);

        UserAnimeList entry = userAnimeListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Entrada no encontrada para la usuaria" + userId + " y el anime " + animeId));

        userAnimeListRepository.delete(entry);
    }

    private UserAnimeListResDTO toDto(UserAnimeList ual) {
        UserAnimeListResDTO dto = new UserAnimeListResDTO();
        dto.setAnimeId(ual.getAnime().getId());
        dto.setTitle(ual.getAnime().getTitle());
        dto.setImageUrl(ual.getAnime().getImageUrl());
        dto.setStatus(toAnimeStatus(ual.getStatus()));
        dto.setCurrentEpisode(ual.getCurrentEpisode());
        dto.setEpisodesCount(ual.getAnime().getEpisodesCount());
        return dto;
    }

    private AnimeStatus toAnimeStatus(Status status) {
        return switch (status) {
            case WATCHING -> AnimeStatus.Watching;
            case COMPLETED -> AnimeStatus.Completed;
            case DROPPED -> AnimeStatus.Dropped;
            default -> throw new IllegalArgumentException("Estatus desconocido: " + status);
        };
    }

    private void verifyOwnership(Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Acceso denegado. Debe estar autenticado.");
        }

        String currentUsername = auth.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la autenticación del usuario"));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!currentUser.getId().equals(userId) && !isAdmin) {
            throw new AccessDeniedException("Solo puedes gestionar tu propia lista de anime.");
        }
    }

    private Status toEntityStatus(AnimeStatus status) {
        return switch (status) {
            case Watching -> Status.WATCHING;
            case Completed -> Status.COMPLETED;
            case Dropped -> Status.DROPPED;
            default -> throw new IllegalArgumentException("Estatus desconocido: " + status);
        };
    }
}

package com.example.gahramheit.service;

import com.example.gahramheit.dto.AnimeStatus;
import com.example.gahramheit.dto.UpdateUserAnimeListReqDTO;
import com.example.gahramheit.dto.UserAnimeListResDTO;
import com.example.gahramheit.entity.*;
import com.example.gahramheit.exception.AccessDeniedException;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.repository.AnimeRepository;
import com.example.gahramheit.repository.UserAnimeListRepository;
import com.example.gahramheit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAnimeListService {

    private final UserAnimeListRepository userAnimeListRepository;
    private final UserRepository userRepository;
    private final AnimeRepository animeRepository;

    public List<UserAnimeListResDTO> getUserList(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        return userAnimeListRepository.findByUserId(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

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

    public void removeFromList(Long userId, Long animeId) {
        verifyOwnership(userId);

        UserAnimeListId id = new UserAnimeListId(userId, animeId);

        UserAnimeList entry = userAnimeListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Entry not found for user " + userId + " and anime " + animeId));

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
        switch (status) {
            case WATCHING:  return AnimeStatus.Watching;
            case COMPLETED: return AnimeStatus.Completed;
            case DROPPED:   return AnimeStatus.Dropped;
            default: throw new IllegalArgumentException("Unknown status: " + status);
        }
    }

    private void verifyOwnership(Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!currentUser.getId().equals(userId) && !isAdmin) {
            throw new AccessDeniedException("You can only manage your own anime list");
        }
    }

    private Status toEntityStatus(AnimeStatus status) {
        switch (status) {
            case Watching:  return Status.WATCHING;
            case Completed: return Status.COMPLETED;
            case Dropped:   return Status.DROPPED;
            default: throw new IllegalArgumentException("Unknown status: " + status);
        }
    }
}

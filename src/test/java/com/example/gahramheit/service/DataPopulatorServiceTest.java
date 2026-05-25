package com.example.gahramheit.service;

import com.example.gahramheit.entity.Role;
import com.example.gahramheit.entity.User;
import com.example.gahramheit.repository.AnimeRepository;
import com.example.gahramheit.repository.EpisodeRepository;
import com.example.gahramheit.repository.ReviewRepository;
import com.example.gahramheit.repository.UserAnimeListRepository;
import com.example.gahramheit.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataPopulatorServiceTest {

    @Mock
    private AnimeRepository animeRepository;

    @Mock
    private EpisodeRepository episodeRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAnimeListRepository userAnimeListRepository;

    @InjectMocks
    private DataPopulatorService dataPopulatorService;

    @Test
    void shouldCreateSeedUsersWhenUsersDoNotExistAndThereAreNoAnimes() {
        AtomicLong idSequence = new AtomicLong(1L);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(idSequence.getAndIncrement());
            return user;
        });
        when(animeRepository.findAll()).thenReturn(List.of());

        dataPopulatorService.populateRemainingTables();

        verify(userRepository, times(5)).save(any(User.class));
        verify(animeRepository).findAll();
        verify(episodeRepository, never()).save(any());
        verify(reviewRepository, never()).save(any());
        verify(userAnimeListRepository, never()).save(any());
    }

    @Test
    void shouldReuseSeedUsersWhenUsersAlreadyExistAndThereAreNoAnimes() {
        when(userRepository.findByUsername(anyString())).thenAnswer(invocation -> {
            String username = invocation.getArgument(0);
            User user = new User();
            user.setId(1L);
            user.setUsername(username);
            user.setEmail(username + "@gahramheit.com");
            user.setPassword("password123");
            user.setRole(Role.USER);
            return Optional.of(user);
        });
        when(animeRepository.findAll()).thenReturn(List.of());

        dataPopulatorService.populateRemainingTables();

        verify(userRepository, never()).save(any(User.class));
        verify(animeRepository).findAll();
    }
}

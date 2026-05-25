package com.example.gahramheit.service;

import com.example.gahramheit.dto.AnimeStatus;
import com.example.gahramheit.dto.UpdateUserAnimeListReqDTO;
import com.example.gahramheit.dto.UserAnimeListResDTO;
import com.example.gahramheit.entity.Anime;
import com.example.gahramheit.entity.Role;
import com.example.gahramheit.entity.Status;
import com.example.gahramheit.entity.User;
import com.example.gahramheit.entity.UserAnimeList;
import com.example.gahramheit.entity.UserAnimeListId;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.repository.AnimeRepository;
import com.example.gahramheit.repository.UserAnimeListRepository;
import com.example.gahramheit.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAnimeListServiceTest {

    @Mock
    private UserAnimeListRepository userAnimeListRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AnimeRepository animeRepository;

    @InjectMocks
    private UserAnimeListService userAnimeListService;

    @Test
    void shouldReturnUserListWhenUserExists() {
        User user = createUser(1L, "watcher");
        Anime anime = createAnime(10L, "Frieren");
        UserAnimeList entry = createUserAnimeList(user, anime, Status.WATCHING, 3);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userAnimeListRepository.findByUserId(1L)).thenReturn(List.of(entry));

        List<UserAnimeListResDTO> result = userAnimeListService.getUserList(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(AnimeStatus.Watching);
        assertThat(result.get(0).getCurrentEpisode()).isEqualTo(3);
    }

    @Test
    void shouldThrowResourceNotFoundWhenGettingListForMissingUser() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userAnimeListService.getUserList(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 99");
        verify(userAnimeListRepository, never()).findByUserId(99L);
    }

    @Test
    void shouldCreateAnimeInListWhenEntryDoesNotExist() {
        User user = createUser(1L, "watcher");
        setUpSecurityContext("watcher", user);
        Anime anime = createAnime(10L, "Frieren");
        UpdateUserAnimeListReqDTO request = new UpdateUserAnimeListReqDTO(10L, AnimeStatus.Completed, 28);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(animeRepository.findById(10L)).thenReturn(Optional.of(anime));
        when(userAnimeListRepository.findById(new UserAnimeListId(1L, 10L))).thenReturn(Optional.empty());

        UserAnimeListResDTO result = userAnimeListService.updateAnimeInList(1L, request);

        ArgumentCaptor<UserAnimeList> captor = ArgumentCaptor.forClass(UserAnimeList.class);
        verify(userAnimeListRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Status.COMPLETED);
        assertThat(result.getStatus()).isEqualTo(AnimeStatus.Completed);
        assertThat(result.getCurrentEpisode()).isEqualTo(28);
    }

    @Test
    void shouldUpdateAnimeInListWhenEntryExists() {
        User user = createUser(1L, "watcher");
        setUpSecurityContext("watcher", user);
        Anime anime = createAnime(10L, "Frieren");
        UserAnimeList entry = createUserAnimeList(user, anime, Status.WATCHING, 3);
        UpdateUserAnimeListReqDTO request = new UpdateUserAnimeListReqDTO(10L, AnimeStatus.Dropped, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(animeRepository.findById(10L)).thenReturn(Optional.of(anime));
        when(userAnimeListRepository.findById(new UserAnimeListId(1L, 10L))).thenReturn(Optional.of(entry));

        UserAnimeListResDTO result = userAnimeListService.updateAnimeInList(1L, request);

        assertThat(entry.getStatus()).isEqualTo(Status.DROPPED);
        assertThat(entry.getCurrentEpisode()).isEqualTo(3);
        assertThat(result.getStatus()).isEqualTo(AnimeStatus.Dropped);
    }

    @Test
    void shouldThrowResourceNotFoundWhenUpdatingListForMissingAnime() {
        User user = createUser(1L, "watcher");
        setUpSecurityContext("watcher", user);
        UpdateUserAnimeListReqDTO request = new UpdateUserAnimeListReqDTO(99L, AnimeStatus.Watching, 1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(animeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAnimeListService.updateAnimeInList(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Anime not found with id: 99");
        verify(userAnimeListRepository, never()).save(any(UserAnimeList.class));
    }

    @Test
    void shouldRemoveFromListWhenEntryExists() {
        User user = createUser(1L, "watcher");
        setUpSecurityContext("watcher", user);
        UserAnimeList entry = createUserAnimeList(user, createAnime(10L, "Frieren"), Status.WATCHING, 3);
        when(userAnimeListRepository.findById(new UserAnimeListId(1L, 10L))).thenReturn(Optional.of(entry));

        userAnimeListService.removeFromList(1L, 10L);

        verify(userAnimeListRepository).delete(entry);
    }

    @Test
    void shouldThrowResourceNotFoundWhenRemovingMissingEntry() {
        User user = createUser(1L, "watcher");
        setUpSecurityContext("watcher", user);
        when(userAnimeListRepository.findById(new UserAnimeListId(1L, 10L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAnimeListService.removeFromList(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Entry not found for user 1 and anime 10");
    }

    private void setUpSecurityContext(String username, User user) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );
        SecurityContextHolder.setContext(context);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    }

    private User createUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(Role.USER);
        return user;
    }

    private Anime createAnime(Long id, String title) {
        Anime anime = new Anime();
        anime.setId(id);
        anime.setTitle(title);
        anime.setImageUrl("https://example.com/anime.jpg");
        anime.setEpisodesCount(28);
        return anime;
    }

    private UserAnimeList createUserAnimeList(User user, Anime anime, Status status, Integer currentEpisode) {
        UserAnimeList entry = new UserAnimeList();
        entry.setId(new UserAnimeListId(user.getId(), anime.getId()));
        entry.setUser(user);
        entry.setAnime(anime);
        entry.setStatus(status);
        entry.setCurrentEpisode(currentEpisode);
        return entry;
    }
}

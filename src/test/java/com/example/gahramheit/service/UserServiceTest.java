package com.example.gahramheit.service;

import com.example.gahramheit.dto.UserProfileResDTO;
import com.example.gahramheit.dto.UserRecapResDTO;
import com.example.gahramheit.dto.UserResponseDTO;
import com.example.gahramheit.dto.UserUpdateDTO;
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
import com.example.gahramheit.repository.UserAnimeListRepository;
import com.example.gahramheit.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAnimeListRepository userAnimeListRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldReturnUserProfileWhenUserExists() {
        User user = createUser(1L, "john", "john@gahramheit.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userAnimeListRepository.findByUserId(1L)).thenReturn(List.of(
                createUserAnimeList(user, createAnime(10L, "Completed 1"), Status.COMPLETED, 12),
                createUserAnimeList(user, createAnime(11L, "Watching"), Status.WATCHING, 5)
        ));

        UserProfileResDTO profile = userService.getUserProfile(1L);

        assertThat(profile.getUsername()).isEqualTo("john");
        assertThat(profile.getEpisodiosVistos()).isEqualTo(17);
        assertThat(profile.getAnimesCompletados()).isEqualTo(1);
        assertThat(profile.getRango()).isEqualTo("Nuevo en el Mundo Anime");
    }

    @Test
    void shouldReturnExperiencedRankWhenUserCompletedAtLeastFifteenAnime() {
        User user = createUser(1L, "senpai", "senpai@gahramheit.com");
        List<UserAnimeList> completedEntries = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            completedEntries.add(createUserAnimeList(user, createAnime((long) i, "Anime " + i), Status.COMPLETED, 12));
        }
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userAnimeListRepository.findByUserId(1L)).thenReturn(completedEntries);

        UserProfileResDTO profile = userService.getUserById(1L);

        assertThat(profile.getRango()).isEqualTo("Otaku Experimentado");
        assertThat(profile.getAnimesCompletados()).isEqualTo(15);
    }

    @Test
    void shouldThrowResourceNotFoundWhenUserProfileDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 99");
    }

    @Test
    void shouldReturnUserWhenUsernameExists() {
        User user = createUser(1L, "john", "john@gahramheit.com");
        UserResponseDTO dto = UserResponseDTO.builder().id(1L).username("john").email("john@gahramheit.com").build();
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserResponseDTO.class)).thenReturn(dto);

        UserResponseDTO result = userService.getUserByUsername("john");

        assertThat(result.getUsername()).isEqualTo("john");
    }

    @Test
    void shouldReturnUserRecapWhenUserExists() {
        User user = createUser(1L, "john", "john@gahramheit.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userAnimeListRepository.findByUserId(1L)).thenReturn(List.of(
                createUserAnimeList(user, createAnime(10L, "Completed 1"), Status.COMPLETED, 12),
                createUserAnimeList(user, createAnime(11L, "Completed 2"), Status.COMPLETED, 10)
        ));

        UserRecapResDTO recap = userService.getUserRecap(1L, 2026);

        assertThat(recap.getAnio()).isEqualTo(2026);
        assertThat(recap.getTotalEpisodiosVistos()).isEqualTo(22);
        assertThat(recap.getTiempoTotalMinutos()).isEqualTo(528L);
        assertThat(recap.getInsigniaDestacadaAnual()).isEqualTo("Principiante");
    }

    @Test
    void shouldUpdateOnlyProvidedFieldsWhenUserExists() {
        setUpSecurityContext("old");
        User user = createUser(1L, "old", "old@gahramheit.com");
        UserUpdateDTO request = UserUpdateDTO.builder().username("new").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserUpdateDTO.class)).thenReturn(UserUpdateDTO.builder()
                .username("new")
                .email("old@gahramheit.com")
                .build());

        UserUpdateDTO result = userService.updateUser(1L, request);

        assertThat(user.getUsername()).isEqualTo("new");
        assertThat(user.getEmail()).isEqualTo("old@gahramheit.com");
        assertThat(result.getUsername()).isEqualTo("new");
        verify(userRepository).save(user);
    }

    @Test
    void shouldDeleteUserWhenUserExists() {
        User user = createUser(1L, "john", "john@gahramheit.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository).delete(user);
    }

    private void setUpSecurityContext(String username) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );
        SecurityContextHolder.setContext(context);
    }

    private User createUser(Long id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        user.setRole(Role.USER);
        return user;
    }

    private Anime createAnime(Long id, String title) {
        Anime anime = new Anime();
        anime.setId(id);
        anime.setTitle(title);
        anime.setEpisodesCount(12);
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

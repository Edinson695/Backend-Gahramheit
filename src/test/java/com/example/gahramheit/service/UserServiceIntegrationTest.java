package com.example.gahramheit.service;

import com.example.gahramheit.dto.UserDTO;
import com.example.gahramheit.dto.UserProfileResDTO;
import com.example.gahramheit.dto.UserRecapResDTO;
import com.example.gahramheit.entity.Anime;
import com.example.gahramheit.entity.Status;
import com.example.gahramheit.entity.User;
import com.example.gahramheit.entity.UserAnimeList;
import com.example.gahramheit.entity.UserAnimeListId;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.repository.AnimeRepository;
import com.example.gahramheit.repository.UserAnimeListRepository;
import com.example.gahramheit.repository.UserRepository;
import com.example.gahramheit.support.AbstractPostgresContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServiceIntegrationTest extends AbstractPostgresContainerTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnimeRepository animeRepository;

    @Autowired
    private UserAnimeListRepository userAnimeListRepository;

    @Test
    void shouldBuildProfileWhenUserHasPersistedAnimeListEntries() {
        User user = userRepository.save(createUser("profile_user"));
        Anime anime = animeRepository.save(createAnime("Profile Anime"));
        saveListEntry(user, anime, Status.COMPLETED, 12);

        UserProfileResDTO profile = userService.getUserProfile(user.getId());

        assertThat(profile.getUsername()).isEqualTo("profile_user");
        assertThat(profile.getEpisodiosVistos()).isEqualTo(12);
        assertThat(profile.getAnimesCompletados()).isEqualTo(1);
        assertThat(profile.getRango()).isEqualTo("Nuevo en el Mundo Anime");
    }

    @Test
    void shouldUpdatePersistedUserWhenRequestContainsNewValues() {
        User user = userRepository.save(createUser("update_user"));
        UserDTO request = UserDTO.builder()
                .username("updated_user")
                .email("updated_user@gahramheit.com")
                .build();

        UserDTO updated = userService.updateUser(user.getId(), request);

        assertThat(updated.getUsername()).isEqualTo("updated_user");
        assertThat(userRepository.findById(user.getId()))
                .get()
                .extracting(User::getEmail)
                .isEqualTo("updated_user@gahramheit.com");
    }

    @Test
    void shouldBuildRecapWhenUserHasPersistedAnimeListEntries() {
        User user = userRepository.save(createUser("recap_user"));
        Anime anime = animeRepository.save(createAnime("Recap Anime"));
        saveListEntry(user, anime, Status.WATCHING, 8);

        UserRecapResDTO recap = userService.getUserRecap(user.getId(), 2026);

        assertThat(recap.getAnio()).isEqualTo(2026);
        assertThat(recap.getTotalEpisodiosVistos()).isEqualTo(8);
        assertThat(recap.getTiempoTotalMinutos()).isEqualTo(192L);
        assertThat(recap.getInsigniaDestacadaAnual()).isEqualTo("Principiante");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserProfileUserDoesNotExist() {
        assertThatThrownBy(() -> userService.getUserProfile(999_999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999999");
    }

    private void saveListEntry(User user, Anime anime, Status status, Integer currentEpisode) {
        UserAnimeList entry = new UserAnimeList();
        entry.setId(new UserAnimeListId(user.getId(), anime.getId()));
        entry.setUser(user);
        entry.setAnime(anime);
        entry.setStatus(status);
        entry.setCurrentEpisode(currentEpisode);
        userAnimeListRepository.save(entry);
    }

    private User createUser(String username) {
        return User.builder()
                .username(username)
                .email(username + "@gahramheit.com")
                .password("password123")
                .build();
    }

    private Anime createAnime(String title) {
        return Anime.builder()
                .title(title)
                .episodesCount(12)
                .imageUrl("https://example.com/user.jpg")
                .build();
    }
}

package com.example.gahramheit.service;

import com.example.gahramheit.dto.AnimeStatus;
import com.example.gahramheit.dto.UpdateUserAnimeListReqDTO;
import com.example.gahramheit.dto.UserAnimeListResDTO;
import com.example.gahramheit.entity.Anime;
import com.example.gahramheit.entity.User;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.repository.AnimeRepository;
import com.example.gahramheit.repository.UserAnimeListRepository;
import com.example.gahramheit.repository.UserRepository;
import com.example.gahramheit.support.AbstractPostgresContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserAnimeListServiceIntegrationTest extends AbstractPostgresContainerTest {

    @Autowired
    private UserAnimeListService userAnimeListService;

    @Autowired
    private UserAnimeListRepository userAnimeListRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnimeRepository animeRepository;

    @Test
    void shouldPersistAnimeInUserListWhenRequestIsValid() {
        User user = userRepository.save(createUser("list_user"));
        Anime anime = animeRepository.save(createAnime("List Anime"));
        UpdateUserAnimeListReqDTO request = new UpdateUserAnimeListReqDTO(anime.getId(), AnimeStatus.Watching, 3);

        UserAnimeListResDTO saved = userAnimeListService.updateAnimeInList(user.getId(), request);

        assertThat(saved.getAnimeId()).isEqualTo(anime.getId());
        assertThat(saved.getStatus()).isEqualTo(AnimeStatus.Watching);
        assertThat(saved.getCurrentEpisode()).isEqualTo(3);
        assertThat(userAnimeListRepository.findByUserId(user.getId())).hasSize(1);
    }

    @Test
    void shouldUpdateExistingAnimeInUserListWhenEntryAlreadyExists() {
        User user = userRepository.save(createUser("list_update_user"));
        Anime anime = animeRepository.save(createAnime("List Update Anime"));
        userAnimeListService.updateAnimeInList(user.getId(),
                new UpdateUserAnimeListReqDTO(anime.getId(), AnimeStatus.Watching, 2));

        UserAnimeListResDTO updated = userAnimeListService.updateAnimeInList(user.getId(),
                new UpdateUserAnimeListReqDTO(anime.getId(), AnimeStatus.Completed, 12));

        assertThat(updated.getStatus()).isEqualTo(AnimeStatus.Completed);
        assertThat(updated.getCurrentEpisode()).isEqualTo(12);
        assertThat(userAnimeListRepository.findByUserId(user.getId())).hasSize(1);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserListUserDoesNotExist() {
        assertThatThrownBy(() -> userAnimeListService.getUserList(999_999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999999");
    }

    @Test
    void shouldReturnUserListWhenUserHasPersistedEntries() {
        User user = userRepository.save(createUser("list_read_user"));
        Anime anime = animeRepository.save(createAnime("Readable List Anime"));
        userAnimeListService.updateAnimeInList(user.getId(),
                new UpdateUserAnimeListReqDTO(anime.getId(), AnimeStatus.Completed, 10));

        List<UserAnimeListResDTO> list = userAnimeListService.getUserList(user.getId());

        assertThat(list)
                .hasSize(1)
                .first()
                .extracting(UserAnimeListResDTO::getTitle)
                .isEqualTo("Readable List Anime");
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
                .imageUrl("https://example.com/list.jpg")
                .build();
    }
}

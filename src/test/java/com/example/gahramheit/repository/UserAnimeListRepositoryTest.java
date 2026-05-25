package com.example.gahramheit.repository;

import com.example.gahramheit.entity.Anime;
import com.example.gahramheit.entity.Role;
import com.example.gahramheit.entity.Status;
import com.example.gahramheit.entity.User;
import com.example.gahramheit.entity.UserAnimeList;
import com.example.gahramheit.entity.UserAnimeListId;
import com.example.gahramheit.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAnimeListRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private UserAnimeListRepository userAnimeListRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnimeRepository animeRepository;

    @Test
    void shouldSaveUserAnimeListWhenCompositeIdIsValid() {
        User user = userRepository.saveAndFlush(createUser("watcher", "watcher@gahramheit.com"));
        Anime anime = animeRepository.saveAndFlush(createAnime("Spy x Family"));
        UserAnimeList userAnimeList = createUserAnimeList(user, anime, Status.WATCHING, 3);

        UserAnimeList savedUserAnimeList = userAnimeListRepository.saveAndFlush(userAnimeList);

        assertThat(savedUserAnimeList.getId()).isEqualTo(new UserAnimeListId(user.getId(), anime.getId()));
        assertThat(savedUserAnimeList.getStatus()).isEqualTo(Status.WATCHING);
        assertThat(savedUserAnimeList.getCurrentEpisode()).isEqualTo(3);
    }

    @Test
    void shouldFindUserAnimeListWhenCompositeIdExists() {
        User user = userRepository.saveAndFlush(createUser("completed", "completed@gahramheit.com"));
        Anime anime = animeRepository.saveAndFlush(createAnime("Fullmetal Alchemist"));
        UserAnimeList savedUserAnimeList = userAnimeListRepository.saveAndFlush(
                createUserAnimeList(user, anime, Status.COMPLETED, 64)
        );

        Optional<UserAnimeList> foundUserAnimeList = userAnimeListRepository.findById(savedUserAnimeList.getId());

        assertThat(foundUserAnimeList).isPresent();
        assertThat(foundUserAnimeList.get().getStatus()).isEqualTo(Status.COMPLETED);
    }

    @Test
    void shouldFindUserAnimeListsWhenUserExists() {
        User user = userRepository.saveAndFlush(createUser("list-owner", "list-owner@gahramheit.com"));
        Anime firstAnime = animeRepository.saveAndFlush(createAnime("First User Anime"));
        Anime secondAnime = animeRepository.saveAndFlush(createAnime("Second User Anime"));
        Anime otherAnime = animeRepository.saveAndFlush(createAnime("Other User Anime"));
        userAnimeListRepository.saveAndFlush(createUserAnimeList(user, firstAnime, Status.WATCHING, 1));
        userAnimeListRepository.saveAndFlush(createUserAnimeList(user, secondAnime, Status.COMPLETED, 12));
        User otherUser = userRepository.saveAndFlush(createUser("other-list-owner", "other-list-owner@gahramheit.com"));
        userAnimeListRepository.saveAndFlush(createUserAnimeList(otherUser, otherAnime, Status.DROPPED, 2));

        List<UserAnimeList> animeLists = userAnimeListRepository.findByUserId(user.getId());

        assertThat(animeLists)
                .extracting(userAnimeList -> userAnimeList.getAnime().getTitle())
                .containsExactlyInAnyOrder("First User Anime", "Second User Anime");
    }

    @Test
    void shouldUpdateUserAnimeListWhenCompositeIdExists() {
        User user = userRepository.saveAndFlush(createUser("updater", "updater@gahramheit.com"));
        Anime anime = animeRepository.saveAndFlush(createAnime("Attack on Titan"));
        UserAnimeList savedUserAnimeList = userAnimeListRepository.saveAndFlush(
                createUserAnimeList(user, anime, Status.WATCHING, 5)
        );

        savedUserAnimeList.setStatus(Status.COMPLETED);
        savedUserAnimeList.setCurrentEpisode(12);
        UserAnimeList updatedUserAnimeList = userAnimeListRepository.saveAndFlush(savedUserAnimeList);

        assertThat(updatedUserAnimeList.getStatus()).isEqualTo(Status.COMPLETED);
        assertThat(updatedUserAnimeList.getCurrentEpisode()).isEqualTo(12);
    }

    @Test
    void shouldDeleteUserAnimeListWhenCompositeIdExists() {
        User user = userRepository.saveAndFlush(createUser("dropper", "dropper@gahramheit.com"));
        Anime anime = animeRepository.saveAndFlush(createAnime("Dropped Anime"));
        UserAnimeList savedUserAnimeList = userAnimeListRepository.saveAndFlush(
                createUserAnimeList(user, anime, Status.DROPPED, 2)
        );

        userAnimeListRepository.delete(savedUserAnimeList);
        userAnimeListRepository.flush();

        assertThat(userAnimeListRepository.findById(savedUserAnimeList.getId())).isEmpty();
    }

    @Test
    void shouldRejectUserAnimeListWhenStatusIsMissing() {
        User user = userRepository.saveAndFlush(createUser("missing-status", "missing-status@gahramheit.com"));
        Anime anime = animeRepository.saveAndFlush(createAnime("No Status Anime"));
        UserAnimeList userAnimeList = createUserAnimeList(user, anime, null, 1);

        assertThatThrownBy(() -> userAnimeListRepository.saveAndFlush(userAnimeList))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        user.setRole(Role.USER);
        return user;
    }

    private Anime createAnime(String title) {
        Anime anime = new Anime();
        anime.setTitle(title);
        anime.setMalId(300);
        anime.setEpisodesCount(12);
        anime.setImageUrl("https://example.com/list-anime.jpg");
        return anime;
    }

    private UserAnimeList createUserAnimeList(User user, Anime anime, Status status, Integer currentEpisode) {
        UserAnimeList userAnimeList = new UserAnimeList();
        userAnimeList.setId(new UserAnimeListId(user.getId(), anime.getId()));
        userAnimeList.setUser(user);
        userAnimeList.setAnime(anime);
        userAnimeList.setStatus(status);
        userAnimeList.setCurrentEpisode(currentEpisode);
        return userAnimeList;
    }
}

package com.example.gahramheit.repository;

import com.example.gahramheit.entity.Anime;
import com.example.gahramheit.entity.Genre;
import com.example.gahramheit.entity.Role;
import com.example.gahramheit.entity.Status;
import com.example.gahramheit.entity.User;
import com.example.gahramheit.entity.UserAnimeList;
import com.example.gahramheit.entity.UserAnimeListId;
import com.example.gahramheit.support.AbstractRepositoryTest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnimeRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private AnimeRepository animeRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAnimeListRepository userAnimeListRepository;

    @Test
    void shouldSaveAnimeWhenAnimeIsValid() {
        Anime anime = createAnime("Frieren", 52991, 28);

        Anime savedAnime = animeRepository.saveAndFlush(anime);

        assertThat(savedAnime.getId()).isNotNull();
        assertThat(savedAnime.getTitle()).isEqualTo("Frieren");
        assertThat(savedAnime.getMalId()).isEqualTo(52991);
    }

    @Test
    void shouldUpdateAnimeWhenAnimeExists() {
        Anime savedAnime = animeRepository.saveAndFlush(createAnime("Old Title", 1, 12));

        savedAnime.setTitle("Updated Title");
        savedAnime.setEpisodesCount(24);
        Anime updatedAnime = animeRepository.saveAndFlush(savedAnime);

        assertThat(updatedAnime.getTitle()).isEqualTo("Updated Title");
        assertThat(updatedAnime.getEpisodesCount()).isEqualTo(24);
    }

    @Test
    void shouldDeleteAnimeWhenAnimeExists() {
        Anime savedAnime = animeRepository.saveAndFlush(createAnime("Delete Me", 2, 10));

        animeRepository.delete(savedAnime);
        animeRepository.flush();

        assertThat(animeRepository.findById(savedAnime.getId())).isEmpty();
    }

    @Test
    void shouldFindAnimesWhenTitleContainsTextIgnoringCase() {
        animeRepository.saveAndFlush(createAnime("Sousou no Frieren", 52991, 28));
        animeRepository.saveAndFlush(createAnime("Frieren: Beyond Journey's End", 52992, 28));
        animeRepository.saveAndFlush(createAnime("Mob Psycho 100", 32182, 12));

        List<Anime> foundAnimes = animeRepository.findByTitleContainingIgnoreCase("FRIEREN");

        assertThat(foundAnimes)
                .extracting(Anime::getTitle)
                .containsExactlyInAnyOrder("Sousou no Frieren", "Frieren: Beyond Journey's End");
    }

    @Test
    void shouldRejectAnimeWhenTitleIsBlank() {
        Anime anime = createAnime("", 3, 12);

        assertThatThrownBy(() -> animeRepository.saveAndFlush(anime))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldPersistAnimeGenreRelationWhenAnimeHasGenres() {
        Genre genre = genreRepository.saveAndFlush(createGenre("Adventure"));
        Anime anime = createAnime("Dungeon Meshi", 52701, 24);
        anime.getGenres().add(genre);

        Anime savedAnime = animeRepository.saveAndFlush(anime);
        Optional<Anime> foundAnime = animeRepository.findById(savedAnime.getId());

        assertThat(foundAnime).isPresent();
        assertThat(foundAnime.get().getGenres())
                .extracting(Genre::getName)
                .containsExactly("Adventure");
    }

    @Test
    void shouldReturnMostWatchedGenreWhenUserHasAnimeList() {
        Genre action = genreRepository.saveAndFlush(createGenre("Action"));
        Genre drama = genreRepository.saveAndFlush(createGenre("Drama"));
        Anime firstActionAnime = createAnime("Attack on Titan", 16498, 25);
        firstActionAnime.getGenres().add(action);
        Anime secondActionAnime = createAnime("Jujutsu Kaisen", 40748, 24);
        secondActionAnime.getGenres().add(action);
        Anime dramaAnime = createAnime("Violet Evergarden", 33352, 13);
        dramaAnime.getGenres().add(drama);
        firstActionAnime = animeRepository.saveAndFlush(firstActionAnime);
        secondActionAnime = animeRepository.saveAndFlush(secondActionAnime);
        dramaAnime = animeRepository.saveAndFlush(dramaAnime);
        User user = userRepository.saveAndFlush(createUser("genre-user", "genre-user@gahramheit.com"));
        userAnimeListRepository.saveAndFlush(createUserAnimeList(user, firstActionAnime));
        userAnimeListRepository.saveAndFlush(createUserAnimeList(user, secondActionAnime));
        userAnimeListRepository.saveAndFlush(createUserAnimeList(user, dramaAnime));

        String mostWatchedGenre = animeRepository.getMostWatchedGenreByUser(user.getId());

        assertThat(mostWatchedGenre).isEqualTo("Action");
    }

    private Anime createAnime(String title, Integer malId, Integer episodesCount) {
        Anime anime = new Anime();
        anime.setTitle(title);
        anime.setMalId(malId);
        anime.setEpisodesCount(episodesCount);
        anime.setImageUrl("https://example.com/" + malId + ".jpg");
        return anime;
    }

    private Genre createGenre(String name) {
        Genre genre = new Genre();
        genre.setName(name);
        return genre;
    }

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        user.setRole(Role.USER);
        return user;
    }

    private UserAnimeList createUserAnimeList(
            User user,
            Anime anime
    ) {
        UserAnimeList userAnimeList = new UserAnimeList();
        userAnimeList.setId(new UserAnimeListId(user.getId(), anime.getId()));
        userAnimeList.setUser(user);
        userAnimeList.setAnime(anime);
        userAnimeList.setStatus(Status.WATCHING);
        userAnimeList.setCurrentEpisode(1);
        return userAnimeList;
    }
}

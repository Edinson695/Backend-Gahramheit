package com.example.gahramheit.repository;

import com.example.gahramheit.entity.Role;
import com.example.gahramheit.entity.User;
import com.example.gahramheit.entity.UserRecap;
import com.example.gahramheit.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRecapRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private UserRecapRepository userRecapRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveUserRecapWhenUserExists() {
        User user = userRepository.saveAndFlush(createUser("recap-user", "recap-user@gahramheit.com"));
        UserRecap userRecap = createUserRecap(user, 2026);

        UserRecap savedUserRecap = userRecapRepository.saveAndFlush(userRecap);

        assertThat(savedUserRecap.getId()).isNotNull();
        assertThat(savedUserRecap.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedUserRecap.getYear()).isEqualTo(2026);
    }

    @Test
    void shouldFindUserRecapWhenUserAndYearExist() {
        User user = userRepository.saveAndFlush(createUser("year-user", "year-user@gahramheit.com"));
        UserRecap savedUserRecap = userRecapRepository.saveAndFlush(createUserRecap(user, 2025));

        Optional<UserRecap> foundUserRecap = userRecapRepository.findByUserIdAndYear(user.getId(), 2025);

        assertThat(foundUserRecap).isPresent();
        assertThat(foundUserRecap.get().getId()).isEqualTo(savedUserRecap.getId());
    }

    @Test
    void shouldReturnEmptyWhenUserRecapDoesNotExist() {
        User user = userRepository.saveAndFlush(createUser("missing-recap", "missing-recap@gahramheit.com"));

        Optional<UserRecap> foundUserRecap = userRecapRepository.findByUserIdAndYear(user.getId(), 2026);

        assertThat(foundUserRecap).isEmpty();
    }

    @Test
    void shouldReturnTrueWhenUserRecapExistsForYear() {
        User user = userRepository.saveAndFlush(createUser("exists-recap", "exists-recap@gahramheit.com"));
        userRecapRepository.saveAndFlush(createUserRecap(user, 2026));

        boolean exists = userRecapRepository.existsByUserIdAndYear(user.getId(), 2026);

        assertThat(exists).isTrue();
    }

    @Test
    void shouldRejectUserRecapWhenUserIsMissing() {
        UserRecap userRecap = createUserRecap(null, 2026);

        assertThatThrownBy(() -> userRecapRepository.saveAndFlush(userRecap))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectUserRecapWhenYearIsMissing() {
        User user = userRepository.saveAndFlush(createUser("missing-year", "missing-year@gahramheit.com"));
        UserRecap userRecap = createUserRecap(user, null);

        assertThatThrownBy(() -> userRecapRepository.saveAndFlush(userRecap))
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

    private UserRecap createUserRecap(User user, Integer year) {
        UserRecap userRecap = new UserRecap();
        userRecap.setUser(user);
        userRecap.setYear(year);
        userRecap.setTotalGenresRated(3);
        userRecap.setTopGenre("Action");
        userRecap.setTop5Animes("[\"Frieren\", \"Jujutsu Kaisen\"]");
        userRecap.setAverageScore(8.5);
        userRecap.setAiPersonalizedMessage("You watched a lot of action anime.");
        return userRecap;
    }
}

package com.example.gahramheit.repository;

import com.example.gahramheit.entity.Role;
import com.example.gahramheit.entity.User;
import com.example.gahramheit.support.AbstractRepositoryTest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveUserWhenUserIsValid() {
        User user = createUser("john", "john@gahramheit.com");

        User savedUser = userRepository.saveAndFlush(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("john");
        assertThat(savedUser.getEmail()).isEqualTo("john@gahramheit.com");
    }

    @Test
    void shouldFindUserWhenUsernameExists() {
        User savedUser = userRepository.saveAndFlush(createUser("hinata", "hinata@gahramheit.com"));

        Optional<User> foundUser = userRepository.findByUsername("hinata");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    void shouldFindUserWhenEmailExists() {
        User savedUser = userRepository.saveAndFlush(createUser("email-user", "email-user@gahramheit.com"));

        Optional<User> foundUser = userRepository.findByEmail("email-user@gahramheit.com");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    void shouldReturnEmptyWhenUsernameDoesNotExist() {
        Optional<User> foundUser = userRepository.findByUsername("missing-user");

        assertThat(foundUser).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenEmailDoesNotExist() {
        Optional<User> foundUser = userRepository.findByEmail("missing@gahramheit.com");

        assertThat(foundUser).isEmpty();
    }

    @Test
    void shouldRejectDuplicateUserWhenUsernameAlreadyExists() {
        userRepository.saveAndFlush(createUser("sakura", "sakura@gahramheit.com"));

        User duplicateUser = createUser("sakura", "other-sakura@gahramheit.com");

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicateUser))
                .isInstanceOfAny(DataIntegrityViolationException.class, ConstraintViolationException.class);
    }

    @Test
    void shouldRejectDuplicateUserWhenEmailAlreadyExists() {
        userRepository.saveAndFlush(createUser("naruto", "naruto@gahramheit.com"));

        User duplicateUser = createUser("other-naruto", "naruto@gahramheit.com");

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicateUser))
                .isInstanceOfAny(DataIntegrityViolationException.class, ConstraintViolationException.class);
    }

    @Test
    void shouldRejectUserWhenEmailIsInvalid() {
        User user = createUser("invalid-email", "not-an-email");

        assertThatThrownBy(() -> userRepository.saveAndFlush(user))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldRejectUserWhenPasswordIsTooShort() {
        User user = createUser("short-password", "short-password@gahramheit.com");
        user.setPassword("short");

        assertThatThrownBy(() -> userRepository.saveAndFlush(user))
                .isInstanceOf(ConstraintViolationException.class);
    }

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        user.setRole(Role.USER);
        return user;
    }
}

package com.example.gahramheit.service;

import com.example.gahramheit.dto.AuthResDTO;
import com.example.gahramheit.dto.UserLoginReqDTO;
import com.example.gahramheit.dto.UserRegisterReqDTO;
import com.example.gahramheit.entity.User;
import com.example.gahramheit.event.UserRegisteredEvent;
import com.example.gahramheit.exception.DuplicateResourceException;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.repository.UserRepository;
import com.example.gahramheit.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldReturnTokenAndUserWhenLoginCredentialsAreValid() {
        UserLoginReqDTO request = new UserLoginReqDTO("john", "password123");
        Authentication authentication = new UsernamePasswordAuthenticationToken("john", null);
        User user = createUser(1L, "john", "john@gahramheit.com");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtils.generateToken("john")).thenReturn("jwt-token");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        AuthResDTO result = authService.login(request);

        assertThat(result.getToken()).isEqualTo("jwt-token");
        assertThat(result.getUser().getUsername()).isEqualTo("john");
        assertThat(result.getUser().getEmail()).isEqualTo("john@gahramheit.com");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void shouldThrowResourceNotFoundWhenAuthenticatedUserDoesNotExist() {
        UserLoginReqDTO request = new UserLoginReqDTO("ghost", "password123");
        Authentication authentication = new UsernamePasswordAuthenticationToken("ghost", null);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtils.generateToken("ghost")).thenReturn("jwt-token");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found: ghost");
    }

    @Test
    void shouldRegisterUserAndPublishEventWhenRequestIsValid() {
        UserRegisterReqDTO request = new UserRegisterReqDTO("john", "john@gahramheit.com", "password123");
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("john@gahramheit.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(jwtUtils.generateToken("john")).thenReturn("jwt-token");

        AuthResDTO result = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("john");
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(result.getToken()).isEqualTo("jwt-token");
        assertThat(result.getUser().getEmail()).isEqualTo("john@gahramheit.com");

        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEmail()).isEqualTo("john@gahramheit.com");
        assertThat(eventCaptor.getValue().getUsername()).isEqualTo("john");
    }

    @Test
    void shouldThrowDuplicateResourceWhenUsernameAlreadyExists() {
        UserRegisterReqDTO request = new UserRegisterReqDTO("john", "new@gahramheit.com", "password123");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(createUser(1L, "john", "old@gahramheit.com")));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username already in use: john");
        verify(userRepository, never()).save(any(User.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowDuplicateResourceWhenEmailAlreadyExists() {
        UserRegisterReqDTO request = new UserRegisterReqDTO("john", "john@gahramheit.com", "password123");
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("john@gahramheit.com"))
                .thenReturn(Optional.of(createUser(1L, "other", "john@gahramheit.com")));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already in use: john@gahramheit.com");
        verify(userRepository, never()).save(any(User.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    private User createUser(Long id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encoded-password");
        return user;
    }
}

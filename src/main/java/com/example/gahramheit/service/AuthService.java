package com.example.gahramheit.service;

import com.example.gahramheit.dto.AuthResDTO;
import com.example.gahramheit.dto.UserLoginReqDTO;
import com.example.gahramheit.dto.UserRegisterReqDTO;
import com.example.gahramheit.entity.Role;
import com.example.gahramheit.entity.User;
import com.example.gahramheit.event.UserRegisteredEvent;
import com.example.gahramheit.exception.DuplicateResourceException;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.repository.UserRepository;
import com.example.gahramheit.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final ApplicationEventPublisher eventPublisher;

    public AuthResDTO login(UserLoginReqDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
        );

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        AuthResDTO.UserBasicInfo userInfo = AuthResDTO.UserBasicInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();

        return AuthResDTO.builder().token(token).user(userInfo).build();
    }


    public AuthResDTO register(UserRegisterReqDTO request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DuplicateResourceException("Username already in use: " + request.getUsername());
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(user.getEmail(), user.getUsername()));

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        AuthResDTO.UserBasicInfo userInfo = AuthResDTO.UserBasicInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();

        return AuthResDTO.builder().token(token).user(userInfo).build();
    }
}

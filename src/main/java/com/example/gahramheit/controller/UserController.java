package com.example.gahramheit.controller;

import com.example.gahramheit.dto.UserResponseDTO;
import com.example.gahramheit.dto.UserUpdateDTO;
import com.example.gahramheit.dto.UserProfileResDTO;
import com.example.gahramheit.entity.Role;
import com.example.gahramheit.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponseDTO> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    @GetMapping("/{id}/profile")
    public ResponseEntity<UserProfileResDTO> getUserProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserProfile(id));
    }

    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UserUpdateDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateDTO request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/role")
    public ResponseEntity<UserResponseDTO> updateUserRole(
            @PathVariable Long id,
            @RequestParam Role role) {
        return ResponseEntity.ok(userService.updateUserRole(id, role));
    }
}

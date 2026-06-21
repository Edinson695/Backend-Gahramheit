package com.example.gahramheit.controller;

import com.example.gahramheit.dto.UpdateUserAnimeListReqDTO;
import com.example.gahramheit.dto.UserAnimeListResDTO;
import com.example.gahramheit.service.UserAnimeListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/anime-list")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserAnimeListController {

    private final UserAnimeListService userAnimeListService;

    @GetMapping
    public ResponseEntity<List<UserAnimeListResDTO>> getUserList(@PathVariable Long userId) {
        return ResponseEntity.ok(userAnimeListService.getUserList(userId));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PutMapping
    public ResponseEntity<UserAnimeListResDTO> updateAnimeInList(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserAnimeListReqDTO request) {
        return ResponseEntity.ok(userAnimeListService.updateAnimeInList(userId, request));
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{animeId}")
    public ResponseEntity<Void> removeFromList(
            @PathVariable Long userId,
            @PathVariable Long animeId) {
        userAnimeListService.removeFromList(userId, animeId);
        return ResponseEntity.noContent().build();
    }
}

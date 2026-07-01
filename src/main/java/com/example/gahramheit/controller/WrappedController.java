package com.example.gahramheit.controller;

import com.example.gahramheit.dto.UserRecapResDTO;
import com.example.gahramheit.service.WrappedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wrapped")
@RequiredArgsConstructor
public class WrappedController {

    private final WrappedService wrappedService;

    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    @GetMapping("/{userId}")
    public ResponseEntity<UserRecapResDTO> getRecap(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "2026") Integer year) {
        return ResponseEntity.ok(wrappedService.getRecap(userId, year));
    }

    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    @PostMapping("/{userId}")
    public ResponseEntity<UserRecapResDTO> forceGenerateRecap(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "2026") Integer year) {
        return ResponseEntity.ok(wrappedService.generateRecap(userId, year));
    }
}

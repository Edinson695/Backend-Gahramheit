package com.example.gahramheit.controller;

import com.example.gahramheit.dto.ReviewCreateReqDTO;
import com.example.gahramheit.dto.ReviewResDTO;
import com.example.gahramheit.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins="*")
@RequestMapping
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping("/reviews")
    public ResponseEntity<ReviewResDTO> createReview(
            @RequestParam Long userId,
            @Valid @RequestBody ReviewCreateReqDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(userId, request));
    }

    @GetMapping("/anime/{animeId}/reviews")
    public ResponseEntity<List<ReviewResDTO>> getReviewsByAnime(@PathVariable Long animeId) {
        return ResponseEntity.ok(reviewService.getReviewsByAnime(animeId));
    }

    @GetMapping("/reviews/{id}")
    public ResponseEntity<ReviewResDTO> getReviewById(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getReviewById(id));
    }

    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}

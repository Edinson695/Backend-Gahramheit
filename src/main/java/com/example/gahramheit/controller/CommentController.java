package com.example.gahramheit.controller;

import com.example.gahramheit.dto.CommentCreateReqDTO;
import com.example.gahramheit.dto.CommentResDTO;
import com.example.gahramheit.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/api/anime/{animeId}/comments")
    public ResponseEntity<Page<CommentResDTO>> getRootComments(
            @PathVariable Long animeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(commentService.getRootComments(animeId, page, size));
    }

    @GetMapping("/api/comments/{commentId}/replies")
    public ResponseEntity<List<CommentResDTO>> getReplies(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.getReplies(commentId));
    }

    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    @PostMapping("/api/anime/{animeId}/comments")
    public ResponseEntity<CommentResDTO> createComment(
            @PathVariable Long animeId,
            @RequestParam Long userId,
            @Valid @RequestBody CommentCreateReqDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.createComment(userId, animeId, request));
    }

    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    @PutMapping("/api/comments/{commentId}/like")
    public ResponseEntity<Void> likeComment(@PathVariable Long commentId) {
        commentService.likeComment(commentId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    @PutMapping("/api/comments/{commentId}/dislike")
    public ResponseEntity<Void> dislikeComment(@PathVariable Long commentId) {
        commentService.dislikeComment(commentId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    @PutMapping("/api/comments/{commentId}")
    public ResponseEntity<CommentResDTO> editComment(
            @PathVariable Long commentId,
            @RequestParam Long userId,
            @Valid @RequestBody CommentCreateReqDTO request) {
        return ResponseEntity.ok(commentService.editComment(commentId, userId, request));
    }

}

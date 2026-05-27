package com.example.gahramheit.service;

import com.example.gahramheit.dto.ReviewCreateReqDTO;
import com.example.gahramheit.dto.ReviewResDTO;
import com.example.gahramheit.entity.Anime;
import com.example.gahramheit.entity.Review;
import com.example.gahramheit.entity.User;
import com.example.gahramheit.event.AnimeReviewedEvent;
import org.springframework.security.access.AccessDeniedException;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.repository.AnimeRepository;
import com.example.gahramheit.repository.ReviewRepository;
import com.example.gahramheit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final AnimeRepository animeRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReviewResDTO createReview(Long userId, ReviewCreateReqDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Anime anime = animeRepository.findById(request.getAnimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Anime not found with id: " + request.getAnimeId()));

        Review review = Review.builder()
                .user(user)
                .anime(anime)
                .score(request.getScore())
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .build();

        reviewRepository.save(review);
        eventPublisher.publishEvent(new AnimeReviewedEvent(review.getAnime().getId(), review.getScore()));
        return toDto(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResDTO> getReviewsByAnime(Long animeId) {
        if (!animeRepository.existsById(animeId)) {
            throw new ResourceNotFoundException("Anime no encontrado con el id: " + animeId);
        }

        return reviewRepository.findByAnime_Id(animeId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReviewResDTO getReviewById(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        return toDto(review);
    }

    @Transactional
    public void deleteReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review no encontrada con el id: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Acceso negado");
        }

        String currentUsername = auth.getName();

        boolean isOwner = review.getUser() != null && Objects.equals(review.getUser().getUsername(), currentUsername);
        boolean isModeratorOrAdmin = auth.getAuthorities().stream()
                .anyMatch(a ->
                        "ROLE_MODERATOR".equals(a.getAuthority()) ||
                                "ROLE_ADMIN".equals(a.getAuthority())
                );

        if (!isOwner && !isModeratorOrAdmin) {
            throw new AccessDeniedException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
    }

    private ReviewResDTO toDto(Review review) {
        ReviewResDTO dto = modelMapper.map(review, ReviewResDTO.class);
        if (review.getUser() != null) {
            dto.setUsername(review.getUser().getUsername());
        }
        return dto;
    }
}

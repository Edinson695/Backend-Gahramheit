package com.example.gahramheit.service;

import com.example.gahramheit.dto.ReviewCreateReqDTO;
import com.example.gahramheit.dto.ReviewResDTO;
import com.example.gahramheit.entity.Anime;
import com.example.gahramheit.entity.Review;
import com.example.gahramheit.entity.User;
import com.example.gahramheit.event.AnimeReviewedEvent;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.repository.AnimeRepository;
import com.example.gahramheit.repository.ReviewRepository;
import com.example.gahramheit.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private AnimeRepository animeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void shouldCreateReviewWhenUserAndAnimeExist() {
        User user = createUser(1L, "reviewer");
        Anime anime = createAnime(10L, "Frieren");
        ReviewCreateReqDTO request = new ReviewCreateReqDTO(10L, 5, "Excellent");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(animeRepository.findById(10L)).thenReturn(Optional.of(anime));
        when(modelMapper.map(any(Review.class), org.mockito.ArgumentMatchers.eq(ReviewResDTO.class)))
                .thenReturn(ReviewResDTO.builder().score(5).comment("Excellent").build());

        ReviewResDTO result = reviewService.createReview(1L, request);

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(reviewCaptor.capture());
        Review savedReview = reviewCaptor.getValue();
        assertThat(savedReview.getUser()).isEqualTo(user);
        assertThat(savedReview.getAnime()).isEqualTo(anime);
        assertThat(savedReview.getCreatedAt()).isNotNull();
        assertThat(result.getUsername()).isEqualTo("reviewer");

        ArgumentCaptor<AnimeReviewedEvent> eventCaptor = ArgumentCaptor.forClass(AnimeReviewedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getAnimeId()).isEqualTo(10L);
        assertThat(eventCaptor.getValue().getScore()).isEqualTo(5);
    }

    @Test
    void shouldThrowResourceNotFoundWhenCreatingReviewForMissingUser() {
        ReviewCreateReqDTO request = new ReviewCreateReqDTO(10L, 5, "Excellent");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 99");
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void shouldThrowResourceNotFoundWhenCreatingReviewForMissingAnime() {
        User user = createUser(1L, "reviewer");
        ReviewCreateReqDTO request = new ReviewCreateReqDTO(99L, 5, "Excellent");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(animeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Anime not found with id: 99");
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void shouldReturnReviewsWhenAnimeExists() {
        User user = createUser(1L, "reader");
        Anime anime = createAnime(10L, "Frieren");
        Review review = createReview(100L, user, anime, 5);
        when(animeRepository.existsById(10L)).thenReturn(true);
        when(reviewRepository.findByAnimeId(10L)).thenReturn(List.of(review));
        when(modelMapper.map(review, ReviewResDTO.class)).thenReturn(ReviewResDTO.builder().score(5).build());

        List<ReviewResDTO> reviews = reviewService.getReviewsByAnime(10L);

        assertThat(reviews).hasSize(1);
        assertThat(reviews.get(0).getUsername()).isEqualTo("reader");
    }

    @Test
    void shouldThrowResourceNotFoundWhenGettingReviewsForMissingAnime() {
        when(animeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.getReviewsByAnime(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Anime not found with id: 99");
        verify(reviewRepository, never()).findByAnimeId(99L);
    }

    @Test
    void shouldDeleteReviewWhenReviewExists() {
        Review review = createReview(100L, createUser(1L, "reader"), createAnime(10L, "Frieren"), 5);
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        reviewService.deleteReview(100L);

        verify(reviewRepository).delete(review);
    }

    @Test
    void shouldThrowResourceNotFoundWhenReviewDoesNotExist() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviewById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Review not found with id: 99");
    }

    private User createUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private Anime createAnime(Long id, String title) {
        Anime anime = new Anime();
        anime.setId(id);
        anime.setTitle(title);
        return anime;
    }

    private Review createReview(Long id, User user, Anime anime, Integer score) {
        Review review = new Review();
        review.setId(id);
        review.setUser(user);
        review.setAnime(anime);
        review.setScore(score);
        review.setComment("Great");
        review.setCreatedAt(LocalDateTime.now());
        return review;
    }
}

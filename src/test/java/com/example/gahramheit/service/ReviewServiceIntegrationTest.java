package com.example.gahramheit.service;

import com.example.gahramheit.dto.ReviewCreateReqDTO;
import com.example.gahramheit.dto.ReviewResDTO;
import com.example.gahramheit.entity.Anime;
import com.example.gahramheit.entity.Role;
import com.example.gahramheit.entity.User;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.repository.AnimeRepository;
import com.example.gahramheit.repository.ReviewRepository;
import com.example.gahramheit.repository.UserRepository;
import com.example.gahramheit.support.AbstractPostgresContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ReviewServiceIntegrationTest extends AbstractPostgresContainerTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnimeRepository animeRepository;

    @Test
    void shouldPersistReviewWhenUserAndAnimeExist() {
        User user = userRepository.save(createUser("review_user"));
        Anime anime = animeRepository.save(createAnime("Review Anime"));
        ReviewCreateReqDTO request = new ReviewCreateReqDTO(anime.getId(), 5, "Excellent integration review");

        ReviewResDTO created = reviewService.createReview(user.getId(), request);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getUsername()).isEqualTo(user.getUsername());
        assertThat(created.getScore()).isEqualTo(5);
        assertThat(reviewRepository.findById(created.getId())).isPresent();
    }

    @Test
    void shouldReturnReviewsWhenAnimeHasPersistedReviews() {
        User user = userRepository.save(createUser("anime_reviews_user"));
        Anime anime = animeRepository.save(createAnime("Reviewed Anime"));
        reviewService.createReview(user.getId(), new ReviewCreateReqDTO(anime.getId(), 4, "Solid"));

        List<ReviewResDTO> reviews = reviewService.getReviewsByAnime(anime.getId());

        assertThat(reviews)
                .hasSize(1)
                .first()
                .extracting(ReviewResDTO::getComment)
                .isEqualTo("Solid");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenReviewAnimeDoesNotExist() {
        User user = userRepository.save(createUser("missing_anime_user"));
        ReviewCreateReqDTO request = new ReviewCreateReqDTO(999_999L, 3, "No anime");

        assertThatThrownBy(() -> reviewService.createReview(user.getId(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Anime not found");
    }

    private User createUser(String username) {
        return User.builder()
                .username(username)
                .email(username + "@gahramheit.com")
                .password("password123")
                .role(Role.USER)
                .build();
    }

    private Anime createAnime(String title) {
        return Anime.builder()
                .title(title)
                .episodesCount(12)
                .imageUrl("https://example.com/anime.jpg")
                .build();
    }
}

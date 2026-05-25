package com.example.gahramheit.repository;

import com.example.gahramheit.entity.Anime;
import com.example.gahramheit.entity.Review;
import com.example.gahramheit.entity.User;
import com.example.gahramheit.support.AbstractRepositoryTest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnimeRepository animeRepository;

    @Test
    void shouldSaveReviewWhenUserAndAnimeExist() {
        User user = userRepository.saveAndFlush(createUser("reviewer", "reviewer@gahramheit.com"));
        Anime anime = animeRepository.saveAndFlush(createAnime("Jujutsu Kaisen"));
        Review review = createReview(user, anime, 9);

        Review savedReview = reviewRepository.saveAndFlush(review);

        assertThat(savedReview.getId()).isNotNull();
        assertThat(savedReview.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedReview.getAnime().getId()).isEqualTo(anime.getId());
        assertThat(savedReview.getScore()).isEqualTo(9);
    }

    @Test
    void shouldFindReviewWhenReviewExists() {
        User user = userRepository.saveAndFlush(createUser("reader", "reader@gahramheit.com"));
        Anime anime = animeRepository.saveAndFlush(createAnime("Haikyuu"));
        Review savedReview = reviewRepository.saveAndFlush(createReview(user, anime, 10));

        Optional<Review> foundReview = reviewRepository.findById(savedReview.getId());

        assertThat(foundReview).isPresent();
        assertThat(foundReview.get().getComment()).isEqualTo("Great anime");
    }

    @Test
    void shouldFindReviewsWhenAnimeExists() {
        User firstUser = userRepository.saveAndFlush(createUser("anime-reviewer-1", "anime-reviewer-1@gahramheit.com"));
        User secondUser = userRepository.saveAndFlush(createUser("anime-reviewer-2", "anime-reviewer-2@gahramheit.com"));
        Anime anime = animeRepository.saveAndFlush(createAnime("Hunter x Hunter"));
        Anime otherAnime = animeRepository.saveAndFlush(createAnime("Other Anime"));
        reviewRepository.saveAndFlush(createReview(firstUser, anime, 9));
        reviewRepository.saveAndFlush(createReview(secondUser, anime, 10));
        reviewRepository.saveAndFlush(createReview(firstUser, otherAnime, 7));

        List<Review> reviews = reviewRepository.findByAnimeId(anime.getId());

        assertThat(reviews)
                .extracting(Review::getScore)
                .containsExactlyInAnyOrder(9, 10);
    }

    @Test
    void shouldReturnAverageScoreWhenUserHasReviews() {
        User user = userRepository.saveAndFlush(createUser("average-user", "average-user@gahramheit.com"));
        Anime firstAnime = animeRepository.saveAndFlush(createAnime("First Average Anime"));
        Anime secondAnime = animeRepository.saveAndFlush(createAnime("Second Average Anime"));
        reviewRepository.saveAndFlush(createReview(user, firstAnime, 8));
        reviewRepository.saveAndFlush(createReview(user, secondAnime, 9));

        Double averageScore = reviewRepository.getAverageScoreByUser(user.getId());

        assertThat(averageScore).isEqualTo(8.5);
    }

    @Test
    void shouldReturnNullAverageScoreWhenUserHasNoReviews() {
        User user = userRepository.saveAndFlush(createUser("no-reviews", "no-reviews@gahramheit.com"));

        Double averageScore = reviewRepository.getAverageScoreByUser(user.getId());

        assertThat(averageScore).isNull();
    }

    @Test
    void shouldRejectReviewWhenScoreIsGreaterThanTen() {
        User user = userRepository.saveAndFlush(createUser("invalid-score", "invalid-score@gahramheit.com"));
        Anime anime = animeRepository.saveAndFlush(createAnime("Chainsaw Man"));
        Review review = createReview(user, anime, 11);

        assertThatThrownBy(() -> reviewRepository.saveAndFlush(review))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldRejectReviewWhenScoreIsLessThanOne() {
        User user = userRepository.saveAndFlush(createUser("low-score", "low-score@gahramheit.com"));
        Anime anime = animeRepository.saveAndFlush(createAnime("Vinland Saga"));
        Review review = createReview(user, anime, 0);

        assertThatThrownBy(() -> reviewRepository.saveAndFlush(review))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldRejectReviewWhenUserIsMissing() {
        Anime anime = animeRepository.saveAndFlush(createAnime("Bleach"));
        Review review = createReview(null, anime, 8);

        assertThatThrownBy(() -> reviewRepository.saveAndFlush(review))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectReviewWhenCreatedAtIsMissing() {
        User user = userRepository.saveAndFlush(createUser("missing-date", "missing-date@gahramheit.com"));
        Anime anime = animeRepository.saveAndFlush(createAnime("Demon Slayer"));
        Review review = createReview(user, anime, 8);
        review.setCreatedAt(null);

        assertThatThrownBy(() -> reviewRepository.saveAndFlush(review))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        return user;
    }

    private Anime createAnime(String title) {
        Anime anime = new Anime();
        anime.setTitle(title);
        anime.setMalId(200);
        anime.setEpisodesCount(24);
        anime.setImageUrl("https://example.com/review-anime.jpg");
        return anime;
    }

    private Review createReview(User user, Anime anime, Integer score) {
        Review review = new Review();
        review.setUser(user);
        review.setAnime(anime);
        review.setScore(score);
        review.setComment("Great anime");
        review.setCreatedAt(LocalDateTime.now());
        return review;
    }
}

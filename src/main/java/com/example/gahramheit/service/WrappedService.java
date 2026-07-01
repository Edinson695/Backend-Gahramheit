package com.example.gahramheit.service;

import com.example.gahramheit.dto.UserRecapResDTO;
import com.example.gahramheit.entity.*;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WrappedService {

    private static final String SEPARATOR = "||";

    private final UserRecapRepository userRecapRepository;
    private final UserRepository userRepository;
    private final AnimeRepository animeRepository;
    private final ReviewRepository reviewRepository;
    private final UserAnimeListRepository userAnimeListRepository;
    private final AIService aiService;

    @Transactional(readOnly = true)
    public UserRecapResDTO getRecap(Long userId, Integer year) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con id: " + userId);
        }

        Optional<UserRecap> existing = userRecapRepository.findByUserIdAndYear(userId, year);
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        UserRecap recap = generateRecapEntity(userId, year);
        return toDto(recap);
    }

    @Transactional
    public UserRecapResDTO generateRecap(Long userId, Integer year) {
        UserRecap recap = generateRecapEntity(userId, year);
        return toDto(recap);
    }

    private UserRecap generateRecapEntity(Long userId, Integer year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + userId));

        List<UserAnimeList> userList = userAnimeListRepository.findByUser_Id(userId);

        int totalEpisodes = userList.stream()
                .filter(ual -> ual.getCurrentEpisode() != null)
                .mapToInt(UserAnimeList::getCurrentEpisode)
                .sum();

        long completedCount = userList.stream()
                .filter(ual -> ual.getStatus() == Status.COMPLETED)
                .count();

        String topGenre = animeRepository.getMostWatchedGenreByUser(userId);
        if (topGenre == null) topGenre = "Sin datos";

        Double avgScore = reviewRepository.getAverageScoreByUser(userId);

        List<Object[]> topAnimeData = reviewRepository.findTopAnimeByUser(userId);
        UserRecapResDTO.TopAnime favorite = new UserRecapResDTO.TopAnime(0L, "Sin datos", 0);
        List<String> top5AnimeTitles = new ArrayList<>();
        if (topAnimeData != null && !topAnimeData.isEmpty()) {
            for (int i = 0; i < Math.min(topAnimeData.size(), 5); i++) {
                Object[] row = topAnimeData.get(i);
                Long animeId = ((Number) row[0]).longValue();
                String title = (String) row[1];
                Integer score = ((Number) row[2]).intValue();
                if (i == 0) {
                    favorite = new UserRecapResDTO.TopAnime(animeId, title, score);
                }
                top5AnimeTitles.add(title);
            }
        }

        String badge = completedCount >= 10 ? "Completador Serial" : "Principiante";

        String aiMessage = aiService.generateOtakuProfile(
                user.getUsername(),
                topGenre,
                avgScore != null ? avgScore : 0.0
        );

        String top5Joined = String.join(SEPARATOR, top5AnimeTitles);

        UserRecap recap = UserRecap.builder()
                .user(user)
                .year(year)
                .totalEpisodesWatched(totalEpisodes)
                .totalTimeMinutes(totalEpisodes * 24L)
                .topGenre(topGenre)
                .top5Animes(top5Joined)
                .averageScore(avgScore)
                .favoriteAnimeId(favorite.getId())
                .favoriteAnimeTitle(favorite.getTitle())
                .favoriteAnimeScore(favorite.getScore())
                .badgeEarned(badge)
                .aiPersonalizedMessage(aiMessage)
                .createdAt(LocalDateTime.now())
                .build();

        userRecapRepository.save(recap);
        log.info("Recap generado para usuario {} año {}", userId, year);

        return recap;
    }

    private UserRecapResDTO toDto(UserRecap recap) {
        List<String> top5List = new ArrayList<>();
        if (recap.getTop5Animes() != null && !recap.getTop5Animes().isEmpty()) {
            top5List = Arrays.asList(recap.getTop5Animes().split("\\|\\|"));
        }

        UserRecapResDTO.TopAnime top = UserRecapResDTO.TopAnime.builder()
                .id(recap.getFavoriteAnimeId() != null ? recap.getFavoriteAnimeId() : 0L)
                .title(recap.getFavoriteAnimeTitle() != null ? recap.getFavoriteAnimeTitle() : "Sin datos")
                .score(recap.getFavoriteAnimeScore() != null ? recap.getFavoriteAnimeScore() : 0)
                .build();

        return UserRecapResDTO.builder()
                .anio(recap.getYear())
                .totalEpisodiosVistos(recap.getTotalEpisodesWatched())
                .tiempoTotalMinutos(recap.getTotalTimeMinutes())
                .generoFavorito(recap.getTopGenre())
                .top5Animes(top5List)
                .animeMejorCalificado(top)
                .insigniaDestacadaAnual(recap.getBadgeEarned())
                .promedioPuntaje(recap.getAverageScore())
                .mensajePersonalizadoIA(recap.getAiPersonalizedMessage())
                .build();
    }
}

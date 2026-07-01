package com.example.gahramheit.service;

import com.example.gahramheit.dto.AchievementResDTO;
import com.example.gahramheit.entity.*;
import com.example.gahramheit.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserAnimeListRepository userAnimeListRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @PostConstruct
    @Transactional
    public void seedAchievements() {
        if (achievementRepository.count() > 0) return;

        List<Achievement> defaults = List.of(
                Achievement.builder().name("Primeros Pasos").description("Registrarse en Gahramheit").conditionKey("REGISTER").imageUrl("/badges/register.png").tier(1).build(),
                Achievement.builder().name("Completador Novato").description("Completar 1 anime").conditionKey("COMPLETED_ANIME >= 1").imageUrl("/badges/novato.png").tier(1).build(),
                Achievement.builder().name("Completador Intermedio").description("Completar 5 animes").conditionKey("COMPLETED_ANIME >= 5").imageUrl("/badges/intermedio.png").tier(2).build(),
                Achievement.builder().name("Completador Serial").description("Completar 10 animes").conditionKey("COMPLETED_ANIME >= 10").imageUrl("/badges/serial.png").tier(3).build(),
                Achievement.builder().name("Maratonista").description("Ver 100 episodios").conditionKey("TOTAL_EPISODES >= 100").imageUrl("/badges/maratonista.png").tier(2).build(),
                Achievement.builder().name("Crítico").description("Escribir 5 reseñas").conditionKey("REVIEW_COUNT >= 5").imageUrl("/badges/critico.png").tier(2).build()
        );

        achievementRepository.saveAll(defaults);
        log.info("Logros por defecto insertados: {} registros", defaults.size());
    }

    @Transactional
    public void checkAndUnlock(Long userId) {
        if (!userRepository.existsById(userId)) return;

        List<Achievement> allAchievements = achievementRepository.findAll();
        Set<String> unlockedKeys = userAchievementRepository.findByUser_Id(userId).stream()
                .map(ua -> ua.getAchievement().getConditionKey())
                .collect(Collectors.toSet());

        long completedCount = userAnimeListRepository.findByUser_Id(userId).stream()
                .filter(ual -> ual.getStatus() == Status.COMPLETED)
                .count();

        int totalEpisodes = userAnimeListRepository.findByUser_Id(userId).stream()
                .filter(ual -> ual.getCurrentEpisode() != null)
                .mapToInt(UserAnimeList::getCurrentEpisode)
                .sum();

        long reviewCount = reviewRepository.countByUser_Id(userId);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        for (Achievement achievement : allAchievements) {
            if (unlockedKeys.contains(achievement.getConditionKey())) continue;

            boolean meetsCondition = evaluateCondition(achievement.getConditionKey(),
                    completedCount, totalEpisodes, reviewCount);

            if (meetsCondition) {
                UserAchievement ua = UserAchievement.builder()
                        .user(user)
                        .achievement(achievement)
                        .unlockedAt(LocalDateTime.now())
                        .build();
                userAchievementRepository.save(ua);
                log.info("Logro desbloqueado: {} para usuario {}", achievement.getName(), userId);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<AchievementResDTO> getUserAchievements(Long userId) {
        List<Achievement> all = achievementRepository.findAll();
        List<UserAchievement> unlocked = userAchievementRepository.findByUser_Id(userId);
        Set<Long> unlockedIds = unlocked.stream()
                .map(ua -> ua.getAchievement().getId())
                .collect(Collectors.toSet());

        Map<Long, LocalDateTime> unlockedAtMap = unlocked.stream()
                .collect(Collectors.toMap(
                        ua -> ua.getAchievement().getId(),
                        UserAchievement::getUnlockedAt
                ));

        return all.stream().map(a -> AchievementResDTO.builder()
                .id(a.getId())
                .name(a.getName())
                .description(a.getDescription())
                .isUnlocked(unlockedIds.contains(a.getId()))
                .unlockedAt(unlockedAtMap.get(a.getId()))
                .build()
        ).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnlockedCount(Long userId) {
        return userAchievementRepository.countByUser_Id(userId);
    }

    private boolean evaluateCondition(String conditionKey, long completedCount,
                                       int totalEpisodes, long reviewCount) {
        return switch (conditionKey) {
            case "REGISTER" -> true;
            case "COMPLETED_ANIME >= 1" -> completedCount >= 1;
            case "COMPLETED_ANIME >= 5" -> completedCount >= 5;
            case "COMPLETED_ANIME >= 10" -> completedCount >= 10;
            case "TOTAL_EPISODES >= 100" -> totalEpisodes >= 100;
            case "REVIEW_COUNT >= 5" -> reviewCount >= 5;
            default -> false;
        };
    }
}

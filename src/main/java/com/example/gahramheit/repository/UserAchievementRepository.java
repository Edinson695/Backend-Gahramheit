package com.example.gahramheit.repository;

import com.example.gahramheit.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    List<UserAchievement> findByUser_Id(Long userId);
    Optional<UserAchievement> findByUser_IdAndAchievement_Id(Long userId, Long achievementId);
    long countByUser_Id(Long userId);
}

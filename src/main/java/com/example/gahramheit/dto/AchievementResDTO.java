package com.example.gahramheit.dto;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AchievementResDTO {
    private Long id;
    private String name;        
    private String description; 
    private Boolean isUnlocked;
    private LocalDateTime unlockedAt;
}
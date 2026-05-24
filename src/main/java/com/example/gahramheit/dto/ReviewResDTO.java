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
public class ReviewResDTO {
    private Long id;
    private String username;
    private Integer score;
    private String comment;
    private LocalDateTime createdAt;
}
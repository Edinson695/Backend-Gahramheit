package com.example.gahramheit.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "achievements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(name = "condition_key", nullable = false)
    private String conditionKey;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false)
    private Integer tier;
}

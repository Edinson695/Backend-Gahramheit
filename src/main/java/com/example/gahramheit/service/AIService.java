package com.example.gahramheit.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AIService {

    public String generateOtakuProfile(String username, String topGenre, Double avgScore) {
        log.info("Llamando a la IA para generar el perfil de {}", username);

        String personality = "";
        if (avgScore >= 9.0) {
            personality = "Eres sumamente generoso y amas todo lo que ves.";
        } else if (avgScore >= 7.0) {
            personality = "Tienes un gusto equilibrado y sabes apreciar una buena historia.";
        } else {
            personality = "Eres un crítico implacable y muy difícil de complacer (nivel Dios).";
        }

        return String.format(
                "¡Saludos %s! Este 2026 has demostrado ser un verdadero fanático del género %s. " +
                        "Tu calificación promedio fue de %.1f/10, lo que nos dice algo muy claro: %s " +
                        "¡Sigue explorando nuevos mundos el próximo año!",
                username,
                topGenre != null ? topGenre : "Misterio",
                avgScore,
                personality
        );
    }
}
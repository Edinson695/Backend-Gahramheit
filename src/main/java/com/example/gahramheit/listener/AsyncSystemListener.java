package com.example.gahramheit.listener;

import com.example.gahramheit.event.AnimeReviewedEvent;
import com.example.gahramheit.event.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AsyncSystemListener {

    // =========================================================
    // TRABAJADOR 1: ENVÍO DE CORREOS
    // =========================================================
    @Async
    @EventListener
    public void handleUserRegistration(UserRegisteredEvent event) {
        log.info("⚡ [ASYNC-THREAD: {}] Iniciando simulación de envío de correo a: {}",
                Thread.currentThread().getName(), event.getEmail());
        try {
            // Simulamos que conectarse a un servidor SMTP real toma 3 segundos
            Thread.sleep(3000);
            log.info("📧 [ASYNC] Correo de bienvenida enviado exitosamente al otaku: {}", event.getUsername());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Fallo al enviar el correo asíncrono", e);
        }
    }

    // =========================================================
    // TRABAJADOR 2: RECÁLCULO DE ESTADÍSTICAS
    // =========================================================
    @Async
    @EventListener
    public void handleAnimeReview(AnimeReviewedEvent event) {
        log.info("⚡ [ASYNC-THREAD: {}] Recalculando estadísticas para el Anime ID: {}",
                Thread.currentThread().getName(), event.getAnimeId());
        try {
            // Aquí en el futuro puedes inyectar el AnimeRepository y actualizar su puntaje global
            Thread.sleep(1500);
            log.info("📊 [ASYNC] Score promedio actualizado para el Anime ID: {}", event.getAnimeId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
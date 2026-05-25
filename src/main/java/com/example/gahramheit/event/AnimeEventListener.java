package com.example.gahramheit.event;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AnimeEventListener {

    @Async // <-- Le dice a Spring: "Ejecuta esto en un hilo secundario de fondo"
    @EventListener // <-- Le dice a Spring: "Quédate atento a cuando alguien dispare un AnimeReviewedEvent"
    public void handleAnimeReviewed(AnimeReviewedEvent event) {
        try {
            System.out.println("[HILO: " + Thread.currentThread().getName() + "] ⚡ Evento recibido de forma asíncrona!");
            System.out.println("Procesando lógicas pesadas de fondo para el anime ID: " + event.getAnimeId());

            // Simula una tarea pesada de I/O (ej. recalcular ranking global de los 1000 animes)
            Thread.sleep(4000);

            System.out.println("[HILO: " + Thread.currentThread().getName() + "] ✅ Procesamiento de fondo COMPLETO.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
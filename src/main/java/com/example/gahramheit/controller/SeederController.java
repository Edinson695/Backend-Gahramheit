package com.example.gahramheit.controller;

import com.example.gahramheit.service.DataPopulatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class SeederController {

    @Autowired
    private DataPopulatorService dataPopulatorService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/seed")
    public ResponseEntity<String> runSeeder() {
        System.out.println(">>> 1. POSTMAN TOCÓ LA PUERTA DEL CONTROLADOR <<<");
        dataPopulatorService.populateRemainingTables();
        return ResponseEntity.ok("Seeder iniciado en segundo plano. Mira la consola de tu IDE.");
    }
}
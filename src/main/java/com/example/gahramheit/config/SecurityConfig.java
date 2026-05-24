package com.example.gahramheit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Desactivamos CSRF (Obligatorio para que Postman pueda hacer peticiones POST)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Configuramos las reglas de las URLs
                .authorizeHttpRequests(auth -> auth
                        // Permitir acceso libre a tu seeder
                        .requestMatchers("/api/admin/seed").permitAll()

                        // Permitir acceso libre a todo por ahora mientras desarrollas (luego lo cambiaremos)
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
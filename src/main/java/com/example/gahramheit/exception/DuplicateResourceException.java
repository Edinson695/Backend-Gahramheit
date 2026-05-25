package com.example.gahramheit.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 1. Aquí usas la anotación para fijar el código HTTP (409) y el mensaje por defecto
@ResponseStatus(value = HttpStatus.CONFLICT, reason = "El recurso ya existe en el sistema")
public class DuplicateResourceException extends RuntimeException {

    // 2. Permites que Guillermo o Daniel pasen un mensaje más específico si quieren
    public DuplicateResourceException(String message) {
        super(message);
    }
}
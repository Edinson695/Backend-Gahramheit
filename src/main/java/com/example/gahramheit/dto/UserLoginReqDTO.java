package com.example.gahramheit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginReqDTO {
    @NotBlank(message = "El usuario o correo electrónico es requerido.")
    private String usernameOrEmail;

    @NotBlank(message = "La contraseña es requerida.")
    private String password;
}
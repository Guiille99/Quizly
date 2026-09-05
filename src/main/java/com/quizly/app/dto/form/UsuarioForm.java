package com.quizly.app.dto.form;

import com.quizly.app.model.enums.Rol;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data 
public class UsuarioForm {
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
    private String nombre;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 255, message = "Los apellidos no pueden superar los 255 caracteres")
    private String apellidos;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 50, message = "El nombre de usuario no puede superar los 50 caracteres")
    private String username;

    @NotBlank(message =  "El correo electrónico es obligatorio")
    @Email(message = "El formato de correo es inválido")
    private String email;

    private String password;

    @NotNull(message = "El rol es obligatorio")
    private Rol rol;

    private Long generoId;
}

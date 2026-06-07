package com.quizly.app.dto;

import org.apache.commons.lang3.StringUtils;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterFormDto {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255, message = "El nombre debe tener 255 caracteres máximo")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 255, message = "El apellido debe tener 255 caracteres máximo")
    private String apellidos;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^\\S+$", message = "El nombre de usuario no puede contener espacios")
    private String username;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    @Size(max = 100)
    private String email;

    @NotNull(message = "El género es obligatorio")
    private Long idGenero;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "Debes confirmar la contraseña")
    private String passwordConfirm;

    @AssertTrue(message = "Las contraseñas no coinciden")
    public boolean isPasswordConfirmValid() {
        if (StringUtils.isEmpty(password) || StringUtils.isEmpty(passwordConfirm)) {
            return true;
        }
        return password.equals(passwordConfirm);
    }
}

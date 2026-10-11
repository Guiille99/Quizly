package com.quizly.app.dto.form;

import org.apache.commons.lang3.StringUtils;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambioPasswordForm {
    @NotBlank(message = "Introduce tu contraseña actual")
    private String passwordActual;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
    private String passwordNueva;

    @NotBlank(message = "Debes confirmar la nueva contraseña")
    private String passwordConfirmacion;

    @AssertTrue(message = "Las contraseñas no coinciden")
    public boolean isPasswordMatch() {
        if (StringUtils.isEmpty(passwordNueva) || StringUtils.isEmpty(passwordConfirmacion)) {
            return true;
        }
        return passwordNueva.equals(passwordConfirmacion);
    }
}
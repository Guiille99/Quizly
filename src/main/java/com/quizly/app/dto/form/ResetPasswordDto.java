package com.quizly.app.dto.form;

import org.apache.commons.lang3.StringUtils;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordDto {
    
    @NotBlank
    private String token;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "Debes confirmar la contraseña")
    private String passwordConfirm;

    @AssertTrue(message = "Las contraseñas no coinciden")
    public boolean isPasswordMatch() {
        if (StringUtils.isEmpty(password) || StringUtils.isEmpty(passwordConfirm)) {
            return true;
        }
        return password.equals(passwordConfirm);
    }
}

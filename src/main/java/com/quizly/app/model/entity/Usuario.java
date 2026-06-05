package com.quizly.app.model.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.quizly.app.model.enums.Rol;

import io.micrometer.common.util.StringUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Column(name = "apellidos", nullable = false, length = 255)
    private String apellidos;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Rol rol;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "usuario")
    private List<IntentoCuestionario> intentos;

    /**
     * Devuelve las iniciales del usuario basadas en su nombre y primer apellido.
     */
    @Transient
    public String getIniciales() {
        StringBuilder sb = new StringBuilder();
        
        if (StringUtils.isNotBlank(nombre)) {
            sb.append(nombre.trim().charAt(0));
        }
        
        if (StringUtils.isNotBlank(apellidos)) {
            sb.append(apellidos.trim().charAt(0));
        }
        
        return sb.toString().toUpperCase();
    }

}

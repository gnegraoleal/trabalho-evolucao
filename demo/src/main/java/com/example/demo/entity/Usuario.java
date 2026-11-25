package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
    @UniqueConstraint(name = "uk_usuario_username_normalizado", columnNames = "username_normalizado"),
    @UniqueConstraint(name = "uk_usuario_token", columnNames = "token")
})
@Getter @Setter
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 16)
    private String username;
    @Column(name = "username_normalizado", nullable = false, length = 16)
    private String usernameNormalizado;
    @Column(nullable = false, length = 255)
    private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private PapelUsuario papel = PapelUsuario.USER;
    @Column(nullable = false, length = 64)
    private String token;
    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();
    @Column(nullable = false)
    private long loginCount;
    private Instant loginAtual;
    private Instant ultimoLogin;
    @Column(length = 64)
    private String ipLoginAtual;
    @Column(length = 64)
    private String ipUltimoLogin;
    @Column(nullable = false)
    private long versaoCredenciais;
    @Version
    private long versaoRegistro;
    @PrePersist @PreUpdate
    void normalizarNome() { if (username != null) usernameNormalizado = username.toLowerCase(Locale.ROOT); }
}

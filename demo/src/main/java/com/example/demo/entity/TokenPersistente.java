package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "persistent_logins", indexes = @Index(name = "idx_persistent_usuario", columnList = "usuario_id"))
@Getter @Setter
public class TokenPersistente {
    @Id @Column(length = 64)
    private String selector;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @Column(nullable = false, length = 64)
    private String digest;
    @Column(nullable = false)
    private long versaoCredenciais;
    @Column(nullable = false)
    private Instant expiraEm;
}

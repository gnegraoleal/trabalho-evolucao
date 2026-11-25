package com.example.demo.marcador;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarcadorRepository extends JpaRepository<Marcador, Long> {
    List<Marcador> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

    /** Filtra pelo dono: ponto de outro usuário simplesmente "não existe" (404). */
    Optional<Marcador> findByIdAndUsuarioId(Long id, Long usuarioId);
}

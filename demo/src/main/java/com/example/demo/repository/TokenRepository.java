package com.example.demo.repository;

import com.example.demo.entity.TokenPersistente;
import java.time.Instant;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface TokenRepository extends JpaRepository<TokenPersistente, String> {
    @Modifying
    @Query("delete from TokenPersistente t where t.usuario.id = :id")
    int revogarUsuario(@Param("id") Long id);
    @Modifying
    @Query("delete from TokenPersistente t where t.expiraEm <= :agora")
    int removerExpirados(@Param("agora") Instant agora);
}

package com.example.demo.repository;

import com.example.demo.entity.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsernameNormalizado(String username);
    boolean existsByUsernameNormalizado(String username);
    long countByPapel(PapelUsuario papel);
    List<Usuario> findByPapelOrderByUsernameNormalizadoAsc(PapelUsuario papel);
    List<Usuario> findTop10ByOrderByCriadoEmDescIdDesc();
    List<Usuario> findTop10ByUltimoLoginIsNotNullOrderByUltimoLoginDescIdDesc();
    @Query("select u from Usuario u order by case when u.ultimoLogin is null then 1 else 0 end, u.ultimoLogin asc, u.id asc")
    Page<Usuario> ordenarUltimoLoginAsc(Pageable pageable);
    @Query("select u from Usuario u order by case when u.ultimoLogin is null then 1 else 0 end, u.ultimoLogin desc, u.id desc")
    Page<Usuario> ordenarUltimoLoginDesc(Pageable pageable);
    Page<Usuario> findByUsernameNormalizadoContaining(String busca, Pageable pageable);
}

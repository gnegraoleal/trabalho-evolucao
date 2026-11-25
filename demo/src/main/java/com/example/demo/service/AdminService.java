package com.example.demo.service;

import com.example.demo.dto.ContaDTO.*;
import com.example.demo.entity.*;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.security.ContaPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor @PreAuthorize("hasRole('ADMIN')")
public class AdminService {
    private final UsuarioRepository usuarios;
    private final UsuarioService contas;
    private final ContaLock lock;
    private static final Map<String, String> CAMPOS = Map.of(
        "id", "id", "username", "usernameNormalizado", "loginCount", "loginCount", "ultimoLogin", "ultimoLogin");
    private void verificarAdmin(ContaPrincipal principal) {
        if (contas.validarSessao(principal).getPapel() != PapelUsuario.ADMIN)
            throw new RegraContaException(HttpStatus.FORBIDDEN, "Acesso negado.");
    }
    @Transactional(readOnly = true)
    public Dashboard dashboard(ContaPrincipal principal) {
        verificarAdmin(principal);
        long comuns = usuarios.countByPapel(PapelUsuario.USER);
        List<Resumo> admins = usuarios.findByPapelOrderByUsernameNormalizadoAsc(PapelUsuario.ADMIN).stream().map(Resumo::de).toList();
        return new Dashboard(comuns, admins.size(), comuns + admins.size(), admins,
            usuarios.findTop10ByOrderByCriadoEmDescIdDesc().stream().map(Resumo::de).toList(),
            usuarios.findTop10ByUltimoLoginIsNotNullOrderByUltimoLoginDescIdDesc().stream().map(Resumo::de).toList());
    }
    @Transactional(readOnly = true)
    public Pagina listar(ContaPrincipal principal, String busca, int page, String sort, String direction) {
        verificarAdmin(principal);
        if (page < 0 || page > Integer.MAX_VALUE / 100) throw UsuarioPolicy.invalido("Página inválida.");
        String campo = CAMPOS.get(sort);
        if (campo == null) throw UsuarioPolicy.invalido("Campo de ordenação inválido.");
        Sort.Direction ordem;
        try { ordem = Sort.Direction.fromString(direction); }
        catch (IllegalArgumentException e) { throw UsuarioPolicy.invalido("Direção de ordenação inválida."); }
        String termo = UsuarioPolicy.normalizar(busca == null ? "" : busca.trim());
        if (termo.length() > 100) throw UsuarioPolicy.invalido("Pesquisa muito longa.");
        // Papel é armazenado como texto: ADMIN vem antes de USER em ordem ascendente.
        Sort s = termo.isEmpty() ? Sort.by(ordem, campo) : Sort.by("papel", "usernameNormalizado");
        if (!campo.equals("id") || !termo.isEmpty()) s = s.and(Sort.by("id"));
        Pageable pageable = PageRequest.of(page, 100, s);
        Page<Usuario> resultado;
        if (!termo.isEmpty()) resultado = usuarios.findByUsernameNormalizadoContaining(termo, pageable);
        else if (campo.equals("ultimoLogin")) {
            Pageable semSort = PageRequest.of(page, 100);
            resultado = ordem.isAscending() ? usuarios.ordenarUltimoLoginAsc(semSort) : usuarios.ordenarUltimoLoginDesc(semSort);
        } else resultado = usuarios.findAll(pageable);
        return new Pagina(resultado.getContent().stream().map(Resumo::de).toList(), page, 100, resultado.getTotalElements(), resultado.getTotalPages());
    }
    @Transactional(readOnly = true)
    public Resumo obter(ContaPrincipal principal, Long id) { verificarAdmin(principal); return Resumo.de(contas.obter(id)); }
    @Transactional
    public Resumo atualizar(ContaPrincipal principal, Long id, EdicaoAdmin dto) {
        lock.adquirir(); verificarAdmin(principal);
        Usuario u = contas.obter(id);
        contas.aplicarAlteracoes(u, dto.username(), dto.papel(), dto.novaSenha(), dto.confirmacaoSenha());
        return Resumo.de(u);
    }
    @Transactional
    public void excluir(ContaPrincipal principal, Long id) {
        lock.adquirir(); verificarAdmin(principal);
        if (Objects.equals(principal.id(), id)) throw new RegraContaException(HttpStatus.FORBIDDEN,
            "Não é permitido excluir sua própria conta pelo painel administrativo.");
        contas.excluirConta(contas.obter(id));
    }
}

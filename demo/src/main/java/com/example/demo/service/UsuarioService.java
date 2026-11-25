package com.example.demo.service;

import com.example.demo.dto.ContaDTO.*;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.security.ContaPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.time.Instant;
import java.util.Objects;

@Service @RequiredArgsConstructor
public class UsuarioService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UsuarioRepository usuarios;
    private final TokenRepository tokens;
    private final PasswordEncoder encoder;
    private final ContaLock lock;
    private final JdbcTemplate jdbc;

    public static String aleatorio() {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes); return HexFormat.of().formatHex(bytes);
    }
    @Transactional
    public Resumo cadastrar(Cadastro dto) {
        lock.adquirir();
        return Resumo.de(criar(dto.username(), dto.senha(), dto.confirmacaoSenha(), PapelUsuario.USER));
    }
    @Transactional
    public void inicializarAdmin(String nome, String senha) {
        lock.adquirir();
        if (usuarios.countByPapel(PapelUsuario.ADMIN) != 0) return;
        criar(nome, senha, senha, PapelUsuario.ADMIN);
    }
    private Usuario criar(String nome, String senha, String confirmacao, PapelUsuario papel) {
        String normalizado = UsuarioPolicy.validarNome(nome);
        UsuarioPolicy.validarSenha(senha, confirmacao);
        if (usuarios.existsByUsernameNormalizado(normalizado)) throw duplicado();
        Usuario u = new Usuario();
        u.setUsername(nome); u.setUsernameNormalizado(normalizado);
        u.setPasswordHash(encoder.encode(senha)); u.setPapel(papel); u.setToken(aleatorio());
        return usuarios.saveAndFlush(u);
    }
    @Transactional(readOnly = true)
    public Usuario obter(Long id) { return usuarios.findById(id).orElseThrow(UsuarioService::naoEncontrado); }
    @Transactional(readOnly = true)
    public Resumo atual(ContaPrincipal principal) { return Resumo.de(validarSessao(principal)); }
    public Usuario validarSessao(ContaPrincipal principal) {
        if (principal == null) throw sessaoInvalida();
        Usuario u = usuarios.findById(principal.id()).orElseThrow(UsuarioService::sessaoInvalida);
        if (u.getVersaoCredenciais() != principal.versaoCredenciais() || !u.getPapel().name().equals(principal.papel()))
            throw sessaoInvalida();
        return u;
    }
    @Transactional(readOnly = true)
    public boolean sessaoValida(ContaPrincipal principal) {
        try { validarSessao(principal); return true; }
        catch (RegraContaException e) { return false; }
    }
    void registrarLogin(Usuario u, String ip) {
        Instant agora = Instant.now();
        // Devise Trackable: no primeiro login, anterior = atual; depois, desloca o atual.
        u.setUltimoLogin(u.getLoginAtual() == null ? agora : u.getLoginAtual());
        u.setIpUltimoLogin(u.getLoginAtual() == null ? ip : u.getIpLoginAtual());
        u.setLoginAtual(agora); u.setIpLoginAtual(ip); u.setLoginCount(u.getLoginCount() + 1);
    }
    @Transactional
    public Resumo alterarPerfil(ContaPrincipal principal, Perfil dto) {
        lock.adquirir(); Usuario u = validarSessao(principal);
        conferirSenhaAtual(u, dto.senhaAtual());
        // O próprio perfil exige nova autenticação após qualquer envio válido.
        if (!aplicarAlteracoes(u, dto.username(), u.getPapel(), dto.novaSenha(), dto.confirmacaoSenha()))
            revogarCredenciais(u);
        return Resumo.de(u);
    }
    @Transactional
    public void cancelar(ContaPrincipal principal, String senhaAtual) {
        lock.adquirir(); Usuario u = validarSessao(principal);
        conferirSenhaAtual(u, senhaAtual); excluirConta(u);
    }
    void conferirSenhaAtual(Usuario u, String senha) {
        if (senha == null || senha.codePointCount(0, senha.length()) > 128 || !encoder.matches(senha, u.getPasswordHash()))
            throw new RegraContaException(HttpStatus.BAD_REQUEST, "Senha atual incorreta.");
    }
    boolean aplicarAlteracoes(Usuario u, String nome, PapelUsuario papel, String senha, String confirmacao) {
        String normalizado = UsuarioPolicy.validarNome(nome);
        if (papel == null) throw UsuarioPolicy.invalido("Papel obrigatório.");
        if (!normalizado.equals(u.getUsernameNormalizado()) && usuarios.existsByUsernameNormalizado(normalizado)) throw duplicado();
        boolean trocarSenha = UsuarioPolicy.temNovaSenha(senha, confirmacao);
        if (trocarSenha) UsuarioPolicy.validarSenha(senha, confirmacao);
        if (u.getPapel() == PapelUsuario.ADMIN && papel != PapelUsuario.ADMIN) protegerUltimoAdmin(u);
        boolean alterado = !Objects.equals(nome, u.getUsername()) || papel != u.getPapel() || trocarSenha;
        u.setUsername(nome); u.setUsernameNormalizado(normalizado); u.setPapel(papel);
        if (trocarSenha) u.setPasswordHash(encoder.encode(senha));
        if (alterado) revogarCredenciais(u);
        usuarios.saveAndFlush(u);
        return alterado;
    }
    private void revogarCredenciais(Usuario u) {
        u.setVersaoCredenciais(u.getVersaoCredenciais() + 1);
        tokens.revogarUsuario(u.getId());
    }
    void excluirConta(Usuario u) {
        protegerUltimoAdmin(u);
        tokens.revogarUsuario(u.getId());
        // Contrato com Pessoa 3: marcadores(usuario_id), no schema corrente.
        // Suporta execução antes da entrega de marcadores e remoção atômica depois dela.
        Boolean existe = jdbc.queryForObject("select to_regclass('marcadores') is not null", Boolean.class);
        if (Boolean.TRUE.equals(existe)) jdbc.update("delete from marcadores where usuario_id = ?", u.getId());
        usuarios.delete(u); usuarios.flush();
    }
    void protegerUltimoAdmin(Usuario u) {
        if (u.getPapel() == PapelUsuario.ADMIN && usuarios.countByPapel(PapelUsuario.ADMIN) <= 1)
            throw new RegraContaException(HttpStatus.CONFLICT, "O sistema precisa possuir um administrador.");
    }
    static RegraContaException duplicado() { return new RegraContaException(HttpStatus.CONFLICT, "Nome de usuário já cadastrado."); }
    static RegraContaException naoEncontrado() { return new RegraContaException(HttpStatus.NOT_FOUND, "Usuário não encontrado."); }
    static RegraContaException sessaoInvalida() { return new RegraContaException(HttpStatus.UNAUTHORIZED, "Autenticação necessária."); }
}

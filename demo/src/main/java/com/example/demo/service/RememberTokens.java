package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.TokenRepository;
import com.example.demo.security.ContaPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Service @RequiredArgsConstructor
public class RememberTokens {
    public static final int DURACAO = 14 * 24 * 60 * 60;
    private final TokenRepository tokens;
    private final UsuarioService contas;
    private final ContaLock lock;
    public record Restaurado(ContaPrincipal principal, String cookie) {}
    private static String digest(String segredo) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(segredo.getBytes(StandardCharsets.US_ASCII))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static boolean valido(TokenPersistente t, String segredo) {
        return MessageDigest.isEqual(t.getDigest().getBytes(StandardCharsets.US_ASCII), digest(segredo).getBytes(StandardCharsets.US_ASCII));
    }
    private static String[] partes(String cookie) {
        if (cookie == null || !cookie.matches("[a-f0-9]{64}\\.[a-f0-9]{64}")) return null;
        return cookie.split("\\.");
    }
    /** Registra o login e emite (ou revoga) o remember token na mesma transação. */
    @Transactional
    public String concluirLogin(ContaPrincipal principal, String ip, boolean lembrar, String cookieAnterior) {
        lock.adquirir(); Usuario u = contas.validarSessao(principal);
        revogarInterno(cookieAnterior);
        contas.registrarLogin(u, ip);
        if (!lembrar) return null;
        tokens.removerExpirados(Instant.now());
        TokenPersistente t = new TokenPersistente();
        t.setSelector(UsuarioService.aleatorio()); t.setUsuario(u); t.setVersaoCredenciais(u.getVersaoCredenciais());
        String segredo = UsuarioService.aleatorio(); t.setDigest(digest(segredo));
        t.setExpiraEm(Instant.now().plus(14, ChronoUnit.DAYS)); tokens.saveAndFlush(t);
        return t.getSelector() + "." + segredo;
    }
    @Transactional
    public Restaurado consumir(String cookie, String ip) {
        String[] partes = partes(cookie); if (partes == null) return null;
        lock.adquirir(); TokenPersistente t = tokens.findById(partes[0]).orElse(null);
        if (t == null || !valido(t, partes[1])) return null;
        Usuario u = t.getUsuario();
        if (!t.getExpiraEm().isAfter(Instant.now()) || t.getVersaoCredenciais() != u.getVersaoCredenciais()) {
            tokens.delete(t); return null;
        }
        String segredo = UsuarioService.aleatorio(); t.setDigest(digest(segredo));
        t.setExpiraEm(Instant.now().plus(14, ChronoUnit.DAYS));
        contas.registrarLogin(u, ip);
        ContaPrincipal principal = new ContaPrincipal(u); principal.eraseCredentials();
        return new Restaurado(principal, t.getSelector() + "." + segredo);
    }
    @Transactional
    public void revogar(String cookie) {
        if (partes(cookie) == null) return;
        lock.adquirir(); revogarInterno(cookie);
    }
    private void revogarInterno(String cookie) {
        String[] partes = partes(cookie); if (partes == null) return;
        tokens.findById(partes[0]).filter(t -> valido(t, partes[1])).ifPresent(tokens::delete);
    }
}

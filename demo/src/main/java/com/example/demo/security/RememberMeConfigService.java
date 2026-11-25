package com.example.demo.security;

import com.example.demo.service.RememberTokens;
import jakarta.servlet.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import java.util.UUID;

/** Remember Me integrado ao filtro nativo Spring Security, com segredo hasheado e rotação no banco. */
public class RememberMeConfigService implements RememberMeServices, LogoutHandler {
    private static final String COOKIE = "imarker-remember";
    private final RememberTokens tokens;
    private final String key = UUID.randomUUID().toString();
    private final boolean secure;
    public RememberMeConfigService(RememberTokens tokens, boolean secure) { this.tokens = tokens; this.secure = secure; }
    public String key() { return key; }
    private String ler(HttpServletRequest req) {
        if (req.getCookies() != null) for (Cookie c : req.getCookies()) if (COOKIE.equals(c.getName())) return c.getValue();
        return null;
    }
    private void cookie(HttpServletRequest req, HttpServletResponse res, String valor, int segundos) {
        res.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(COOKIE, valor).path("/")
            .httpOnly(true).secure(secure || req.isSecure()).sameSite("Lax").maxAge(segundos).build().toString());
    }
    public void limpar(HttpServletRequest req, HttpServletResponse res) { cookie(req, res, "", 0); }
    @Override public Authentication autoLogin(HttpServletRequest req, HttpServletResponse res) {
        String valor = ler(req); if (valor == null) return null;
        RememberTokens.Restaurado restaurado = tokens.consumir(valor, req.getRemoteAddr());
        if (restaurado == null) { limpar(req, res); return null; }
        cookie(req, res, restaurado.cookie(), RememberTokens.DURACAO);
        var p = restaurado.principal(); return new RememberMeAuthenticationToken(key, p, p.getAuthorities());
    }
    @Override public void loginFail(HttpServletRequest req, HttpServletResponse res) {
        tokens.revogar(ler(req)); limpar(req, res);
    }
    @Override public void loginSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth) {}
    public void concluirLogin(HttpServletRequest req, HttpServletResponse res, Authentication auth) {
        ContaPrincipal p = (ContaPrincipal) auth.getPrincipal();
        String valor = tokens.concluirLogin(p, req.getRemoteAddr(),
            "true".equalsIgnoreCase(req.getParameter("remember-me")), ler(req));
        if (valor == null) limpar(req, res);
        else cookie(req, res, valor, RememberTokens.DURACAO);
    }
    @Override public void logout(HttpServletRequest req, HttpServletResponse res, Authentication auth) {
        tokens.revogar(ler(req)); limpar(req, res);
    }
}

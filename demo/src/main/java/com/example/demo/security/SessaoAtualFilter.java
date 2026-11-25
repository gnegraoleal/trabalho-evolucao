package com.example.demo.security;

import com.example.demo.service.UsuarioService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class SessaoAtualFilter extends OncePerRequestFilter {
    private final UsuarioService contas;
    private final RememberMeConfigService remember;
    public SessaoAtualFilter(UsuarioService contas, RememberMeConfigService remember) { this.contas = contas; this.remember = remember; }
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof ContaPrincipal p && !contas.sessaoValida(p)) {
            new SecurityContextLogoutHandler().logout(req, res, auth); remember.limpar(req, res);
        }
        chain.doFilter(req, res);
    }
}

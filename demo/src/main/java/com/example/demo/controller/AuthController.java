package com.example.demo.controller;

import com.example.demo.dto.ContaDTO.*;
import com.example.demo.security.*;
import com.example.demo.service.UsuarioService;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class AuthController {
    private final UsuarioService contas;
    private final RememberMeConfigService remember;
    @PostMapping("/auth/register") @ResponseStatus(HttpStatus.CREATED)
    public Resumo cadastrar(@RequestBody Cadastro dto) { return contas.cadastrar(dto); }
    @GetMapping("/auth/me")
    public Resumo me(@AuthenticationPrincipal ContaPrincipal p) { return contas.atual(p); }
    @PutMapping("/users/me")
    public Resumo atualizar(@AuthenticationPrincipal ContaPrincipal p, @RequestBody Perfil dto,
        HttpServletRequest req, HttpServletResponse res, Authentication auth) {
        Resumo resumo = contas.alterarPerfil(p, dto);
        encerrar(req, res, auth); return resumo;
    }
    @PostMapping("/users/me/cancelar") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@AuthenticationPrincipal ContaPrincipal p, @RequestBody Cancelamento dto,
        HttpServletRequest req, HttpServletResponse res, Authentication auth) {
        contas.cancelar(p, dto.senhaAtual()); encerrar(req, res, auth);
    }
    private void encerrar(HttpServletRequest req, HttpServletResponse res, Authentication auth) {
        // As credenciais persistentes já foram revogadas pela transação de conta.
        remember.limpar(req, res); new SecurityContextLogoutHandler().logout(req, res, auth);
        CookieCsrfTokenRepository.withHttpOnlyFalse().saveToken(null, req, res);
    }
}

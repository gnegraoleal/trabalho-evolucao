package com.example.demo.controller;

import com.example.demo.dto.ContaDTO.*;
import com.example.demo.security.ContaPrincipal;
import com.example.demo.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin") @RequiredArgsConstructor @PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService admin;
    @GetMapping("/dashboard")
    public Dashboard dashboard(@AuthenticationPrincipal ContaPrincipal p) { return admin.dashboard(p); }
    @GetMapping("/usuarios")
    public Pagina listar(@AuthenticationPrincipal ContaPrincipal p,
        @RequestParam(defaultValue = "") String busca, @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "desc") String direction) {
        return admin.listar(p, busca, page, sort, direction);
    }
    @GetMapping("/usuarios/{id}")
    public Resumo obter(@AuthenticationPrincipal ContaPrincipal p, @PathVariable Long id) { return admin.obter(p, id); }
    @PutMapping("/usuarios/{id}")
    public Resumo editar(@AuthenticationPrincipal ContaPrincipal p, @PathVariable Long id, @RequestBody EdicaoAdmin dto) {
        return admin.atualizar(p, id, dto);
    }
    @DeleteMapping("/usuarios/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal ContaPrincipal p, @PathVariable Long id) { admin.excluir(p, id); }
}

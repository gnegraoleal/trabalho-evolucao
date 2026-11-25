package com.example.demo.dto;

import com.example.demo.entity.*;
import java.time.Instant;
import java.util.List;

public final class ContaDTO {
    private ContaDTO() {}
    public record Cadastro(String username, String senha, String confirmacaoSenha) {}
    public record Perfil(String username, String senhaAtual, String novaSenha, String confirmacaoSenha) {}
    public record Cancelamento(String senhaAtual) {}
    public record EdicaoAdmin(String username, PapelUsuario papel, String novaSenha, String confirmacaoSenha) {}
    public record Resumo(Long id, String username, PapelUsuario papel, long loginCount,
                         Instant criadoEm, Instant loginAtual, Instant ultimoLogin) {
        public static Resumo de(Usuario u) {
            return new Resumo(u.getId(), u.getUsername(), u.getPapel(), u.getLoginCount(),
                u.getCriadoEm(), u.getLoginAtual(), u.getUltimoLogin());
        }
    }
    public record Pagina(List<Resumo> content, int page, int size, long totalElements, int totalPages) {}
    public record Dashboard(long totalUsuarios, long totalAdministradores, long totalContas,
                            List<Resumo> administradores, List<Resumo> cadastrosRecentes,
                            List<Resumo> loginsRecentes) {}
}

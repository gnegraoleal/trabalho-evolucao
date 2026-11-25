package com.example.demo.service;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class UsuarioPolicy {
    private static final Pattern NOME = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private UsuarioPolicy() {}
    public static String normalizar(String nome) { return nome == null ? "" : nome.toLowerCase(Locale.ROOT); }
    public static String validarNome(String nome) {
        if (nome == null || !NOME.matcher(nome).matches())
            throw invalido("Nome deve ter de 3 a 16 letras ASCII, números ou sublinhados.");
        return normalizar(nome);
    }
    public static void validarSenha(String senha, String confirmacao) {
        int tamanho = senha == null ? 0 : senha.codePointCount(0, senha.length());
        if (tamanho < 6 || tamanho > 128)
            throw invalido("Senha deve ter de 6 a 128 caracteres.");
        if (!Objects.equals(senha, confirmacao)) throw invalido("As senhas não coincidem.");
    }
    public static boolean temNovaSenha(String senha, String confirmacao) {
        return (senha != null && !senha.isEmpty()) || (confirmacao != null && !confirmacao.isEmpty());
    }
    public static ValidacaoContaException invalido(String mensagem) {
        return new ValidacaoContaException(mensagem);
    }
}

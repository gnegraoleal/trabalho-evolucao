package com.example.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioPolicyTests {
    @Test void nomeValidoELimites() {
        assertEquals("abc_123", UsuarioPolicy.validarNome("ABC_123"));
        assertEquals("a".repeat(16), UsuarioPolicy.validarNome("A".repeat(16)));
        for (String nome : new String[]{"ab", "a".repeat(17), "a ç", "ab-c", "abc\n", " abc"})
            assertThrows(ValidacaoContaException.class, () -> UsuarioPolicy.validarNome(nome));
        assertThrows(ValidacaoContaException.class, () -> UsuarioPolicy.validarNome(null));
    }
    @Test void senhaLimitesEConfirmacao() {
        for (int n : new int[]{6,128}) {
            String senha = "🔐".repeat(n);
            assertDoesNotThrow(() -> UsuarioPolicy.validarSenha(senha, senha));
        }
        for (int n : new int[]{5,129}) {
            String senha = "a".repeat(n);
            assertThrows(ValidacaoContaException.class, () -> UsuarioPolicy.validarSenha(senha, senha));
        }
        assertThrows(ValidacaoContaException.class, () -> UsuarioPolicy.validarSenha("abcdef", "abcdeg"));
        assertFalse(UsuarioPolicy.temNovaSenha(null, null));
        assertFalse(UsuarioPolicy.temNovaSenha("", ""));
        assertTrue(UsuarioPolicy.temNovaSenha("", "abc"));
    }
    @Test void pbkdf2AceitaSenhaMultibyteLonga() {
        var encoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        String senha = "🔐".repeat(128);
        String hash = encoder.encode(senha);
        assertNotEquals(senha, hash);
        assertTrue(encoder.matches(senha, hash));
        assertFalse(encoder.matches("incorreta", hash));
        assertNotEquals(hash, encoder.encode(senha));
    }
}

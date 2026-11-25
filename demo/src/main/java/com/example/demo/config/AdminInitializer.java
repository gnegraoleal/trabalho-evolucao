package com.example.demo.config;

import com.example.demo.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminInitializer {
    @Bean
    ApplicationRunner criarAdminInicial(UsuarioService contas,
        @Value("${app.admin.enabled:false}") boolean enabled,
        @Value("${app.admin.username:}") String nome,
        @Value("${app.admin.password:}") String senha) {
        return args -> { if (enabled) contas.inicializarAdmin(nome, senha); };
    }
}

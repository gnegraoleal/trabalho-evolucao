package com.example.demo.config;

import com.example.demo.security.*;
import com.example.demo.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8(); }
    @Bean RememberMeConfigService rememberMe( RememberTokens tokens, @Value("${app.cookies.secure:false}") boolean secure) {
        return new RememberMeConfigService(tokens, secure);
    }
    public static void erro(HttpServletResponse res, int status, String mensagem) throws IOException {
        res.setStatus(status); res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"status\":" + status + ",\"message\":\"" + mensagem + "\"}");
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioService contas,
        RememberMeConfigService remember) throws Exception {
        return http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.spa())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/", "/home", "/about", "/api/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .requestCache(cache -> cache.disable())
            .formLogin(form -> form.loginProcessingUrl("/api/auth/login")
                .successHandler((req, res, auth) -> {
                    try {
                        remember.concluirLogin(req, res, auth);
                        res.setStatus(204);
                    } catch (RuntimeException e) {
                        remember.limpar(req, res);
                        new SecurityContextLogoutHandler().logout(req, res, auth);
                        LoggerFactory.getLogger(SecurityConfig.class).warn("Login não concluído: {}", e.getClass().getSimpleName());
                        if (e instanceof RegraContaException) erro(res, 401, "Credenciais desatualizadas.");
                        else erro(res, 500, "Não foi possível concluir o login.");
                    }
                })
                .failureHandler((req, res, e) -> erro(res, 401, "Credenciais inválidas.")))
            .rememberMe(r -> r.key(remember.key()).rememberMeServices(remember))
            .logout(logout -> logout.logoutUrl("/api/auth/logout")
                .deleteCookies("JSESSIONID").logoutSuccessHandler((req,res,auth) -> res.setStatus(204)))
            .addFilterAfter(new SessaoAtualFilter(contas, remember), RememberMeAuthenticationFilter.class)
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req,res,e) -> erro(res,401,"Autenticação necessária."))
                .accessDeniedHandler((req,res,e) -> erro(res,403,"Acesso negado ou token CSRF inválido.")))
            .build();
    }
}

package com.example.demo.security;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.UsuarioPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarios;
    @Override public UserDetails loadUserByUsername(String username) {
        return usuarios.findByUsernameNormalizado(UsuarioPolicy.normalizar(username))
            .map(ContaPrincipal::new)
            .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
    }
}

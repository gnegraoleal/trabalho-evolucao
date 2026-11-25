package com.example.demo.security;

import com.example.demo.entity.Usuario;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

public final class ContaPrincipal implements UserDetails, CredentialsContainer {
    private static final long serialVersionUID = 1L;
    private final Long id;
    private final String username;
    private final String papel;
    private final long versaoCredenciais;
    private String password;
    public ContaPrincipal(Usuario u) {
        id = u.getId(); username = u.getUsernameNormalizado(); papel = u.getPapel().name();
        versaoCredenciais = u.getVersaoCredenciais(); password = u.getPasswordHash();
    }
    public Long id() { return id; }
    public long versaoCredenciais() { return versaoCredenciais; }
    public String papel() { return papel; }
    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return password; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + papel));
    }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
    @Override public void eraseCredentials() { password = null; }
}

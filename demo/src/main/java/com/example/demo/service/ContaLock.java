package com.example.demo.service;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Serializa alterações de contas e de credenciais persistentes entre instâncias PostgreSQL. */
@Component @RequiredArgsConstructor
public class ContaLock {
    private final JdbcTemplate jdbc;
    @Transactional(propagation = Propagation.MANDATORY)
    public void adquirir() {
        jdbc.execute("SELECT pg_advisory_xact_lock(1749915831)");
    }
}

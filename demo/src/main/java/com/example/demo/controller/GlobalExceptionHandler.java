package com.example.demo.controller;

import com.example.demo.service.RegraContaException;
import com.example.demo.service.ValidacaoContaException;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    public record Erro(int status, String message) {}
    private ResponseEntity<Erro> erro(HttpStatus status, String msg) { return ResponseEntity.status(status).body(new Erro(status.value(), msg)); }
    @ExceptionHandler(RegraContaException.class)
    ResponseEntity<Erro> regra(RegraContaException ex) { return erro(ex.status(), ex.getMessage()); }
    @ExceptionHandler(ValidacaoContaException.class)
    ResponseEntity<Erro> validacao(ValidacaoContaException ex) { return erro(HttpStatus.BAD_REQUEST, ex.getMessage()); }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, MethodArgumentNotValidException.class})
    ResponseEntity<Erro> entrada(Exception ex) { return erro(HttpStatus.BAD_REQUEST, "Dados da requisição inválidos."); }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Erro> status(ResponseStatusException ex) {
        var status = ex.getStatusCode();
        return ResponseEntity.status(status).body(new Erro(status.value(), ex.getReason()));
    }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Erro> proibido(AccessDeniedException ex) { return erro(HttpStatus.FORBIDDEN, "Acesso negado."); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Erro> integridade(DataIntegrityViolationException ex) {
        LOG.warn("Operação recusada por restrição de integridade: {}", ex.getClass().getSimpleName());
        return erro(HttpStatus.CONFLICT, "A operação viola uma restrição de integridade.");
    }
    @ExceptionHandler({DataAccessException.class, TransactionException.class, PersistenceException.class})
    ResponseEntity<Erro> inesperado(Exception ex) {
        LOG.error("Falha ao executar operação: {}", ex.getClass().getSimpleName());
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível concluir a operação.");
    }
}

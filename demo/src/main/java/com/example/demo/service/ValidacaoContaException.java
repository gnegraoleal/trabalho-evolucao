package com.example.demo.service;
/** Erro de política de conta, independente de HTTP e do framework. */
public class ValidacaoContaException extends RuntimeException {
    public ValidacaoContaException(String message) { super(message); }
}

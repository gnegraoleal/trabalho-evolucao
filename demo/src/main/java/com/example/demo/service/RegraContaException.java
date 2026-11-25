package com.example.demo.service;
import org.springframework.http.HttpStatus;
public class RegraContaException extends RuntimeException {
    private final HttpStatus status;
    public RegraContaException(HttpStatus status, String message) { super(message); this.status = status; }
    public HttpStatus status() { return status; }
}

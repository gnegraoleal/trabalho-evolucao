package com.example.demo.config;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
class CsrfController {
    @GetMapping("/api/csrf") @ResponseStatus(HttpStatus.NO_CONTENT)
    void initializeCsrfCookie(CsrfToken token) { token.getToken(); }
}

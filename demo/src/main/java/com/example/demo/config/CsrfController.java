package com.example.demo.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CsrfController {
    @GetMapping("/api/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void initializeCsrfCookie() {}
}

package com.example.demo.marcador;

import com.example.demo.marcador.dto.MarcadorRequest;
import com.example.demo.marcador.dto.MarcadorResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/marcadores")
public class MarcadorController {

    private final MarcadorService service;

    public MarcadorController(MarcadorService service) { this.service = service; }

    @GetMapping
    public List<MarcadorResponse> listar(Authentication auth) { return service.listar(auth); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MarcadorResponse criar(@Valid @RequestBody MarcadorRequest req, Authentication auth) {
        return service.criar(req, auth);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id, Authentication auth) { service.excluir(id, auth); }
}

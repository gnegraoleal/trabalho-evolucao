package com.example.demo.marcador;

import com.example.demo.marcador.dto.LinkPublicoResponse;
import com.example.demo.marcador.dto.MarcadorPublicoResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MapaController {

    private final MarcadorService service;

    public MapaController(MarcadorService service) { this.service = service; }

    @GetMapping("/api/mapa/link")
    public LinkPublicoResponse link(Authentication auth) { return service.obterLink(auth); }

    @PostMapping("/api/mapa/link/regenerar")
    public LinkPublicoResponse regenerar(Authentication auth) { return service.regenerar(auth); }

    /** Público (somente leitura). Liberar em /api/public/** no Spring Security. */
    @GetMapping("/api/public/mapas/{nomeUsuario}/{token}")
    public List<MarcadorPublicoResponse> publico(@PathVariable String nomeUsuario, @PathVariable String token) {
        return service.mapaPublico(nomeUsuario, token);
    }
}

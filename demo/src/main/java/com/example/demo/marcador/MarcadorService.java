package com.example.demo.marcador;

import com.example.demo.marcador.dto.*;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.security.ContaPrincipal;
import com.example.demo.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

@Service
public class MarcadorService {

    private final MarcadorRepository marcadores;
    private final UsuarioRepository usuarios;
    private final UsuarioService contas;

    public MarcadorService(MarcadorRepository marcadores, UsuarioRepository usuarios, UsuarioService contas) {
        this.marcadores = marcadores;
        this.usuarios = usuarios;
        this.contas = contas;
    }

    // ---------- marcadores do titular ----------

    @Transactional(readOnly = true)
    public List<MarcadorResponse> listar(Authentication auth) {
        return marcadores.findByUsuarioIdOrderByCriadoEmDesc(usuarioAtual(auth).getId())
                .stream().map(MarcadorResponse::de).toList();
    }

    @Transactional
    public MarcadorResponse criar(MarcadorRequest req, Authentication auth) {
        Marcador m = new Marcador();
        m.setTitulo(req.titulo().trim());
        m.setDescricao(normalizar(req.descricao())); // RD-04: nula e vazia têm o mesmo tratamento
        m.setLatitude(req.latitude());
        m.setLongitude(req.longitude());
        m.setUsuario(usuarioAtual(auth));            // titular sempre da sessão
        return MarcadorResponse.de(marcadores.save(m));
    }

    @Transactional
    public void excluir(Long id, Authentication auth) {
        Marcador m = marcadores.findByIdAndUsuarioId(id, usuarioAtual(auth).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        marcadores.delete(m);
    }

    // ---------- link público ----------

    @Transactional(readOnly = true)
    public LinkPublicoResponse obterLink(Authentication auth) {
        return respostaLink(usuarioAtual(auth));
    }

    @Transactional
    public LinkPublicoResponse regenerar(Authentication auth) {
        return respostaLink(contas.regenerarTokenMapa(principal(auth)));
    }

    private LinkPublicoResponse respostaLink(Usuario usuario) {
        return new LinkPublicoResponse(usuario.getUsername(), usuario.getToken());
    }

    /** Usuário inexistente, token errado e link revogado produzem a mesma resposta (RF-11). */
    @Transactional(readOnly = true)
    public List<MarcadorPublicoResponse> mapaPublico(String nomeUsuario, String token) {
        Usuario u = usuarios.findByUsernameNormalizado(nomeUsuario.toLowerCase(Locale.ROOT))
                .orElseThrow(MarcadorService::naoEncontrado);

        boolean confere = MessageDigest.isEqual(
                u.getToken().getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
        if (!confere) throw naoEncontrado();

        return marcadores.findByUsuarioIdOrderByCriadoEmDesc(u.getId())
                .stream().map(MarcadorPublicoResponse::de).toList();
    }

    // ---------- auxiliares ----------

    private Usuario usuarioAtual(Authentication auth) {
        return contas.validarSessao(principal(auth));
    }

    private ContaPrincipal principal(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof ContaPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return principal;
    }

    private static String normalizar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    private static ResponseStatusException naoEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Link inválido ou revogado");
    }
}

package com.example.demo.marcador;

import com.example.demo.marcador.dto.*;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.security.ContaPrincipal;
import com.example.demo.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Testes unitários sem banco e sem rede (RNF-10).  */
class MarcadorServiceTest {

    MarcadorRepository marcadores = mock(MarcadorRepository.class);
    UsuarioService contas = mock(UsuarioService.class);
    UsuarioRepository usuarios = mock(UsuarioRepository.class);
    MarcadorService service = new MarcadorService(marcadores, usuarios, contas);

    Usuario ana = usuario(1L, "Ana");
    Usuario bia = usuario(2L, "bia");
    Authentication authAna = auth(ana);
    Authentication authBia = auth(bia);

    @BeforeEach
    void setup() {
        when(usuarios.findByUsernameNormalizado(anyString())).thenAnswer(inv -> {
            String nome = inv.getArgument(0);
            if (nome.equalsIgnoreCase("ana")) return Optional.of(ana);
            if (nome.equalsIgnoreCase("bia")) return Optional.of(bia);
            return Optional.empty();
        });
        when(marcadores.save(any(Marcador.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contas.validarSessao((ContaPrincipal) authAna.getPrincipal())).thenReturn(ana);
        when(contas.validarSessao((ContaPrincipal) authBia.getPrincipal())).thenReturn(bia);
    }

    // ---- CA-02 / RF-17: isolamento entre titulares ----

    @Test
    void criarUsaOTitularDaSessaoETrataDescricaoEmBranco() {
        service.criar(new MarcadorRequest("  Casa  ", "   ", -12.97, -38.5), authAna);

        ArgumentCaptor<Marcador> cap = ArgumentCaptor.forClass(Marcador.class);
        verify(marcadores).save(cap.capture());
        assertSame(ana, cap.getValue().getUsuario());
        assertEquals("Casa", cap.getValue().getTitulo());
        assertNull(cap.getValue().getDescricao()); // RD-04: vazia == nula
    }

    @Test
    void excluirPontoDeOutroUsuarioRetorna404ENaoApaga() {
        when(marcadores.findByIdAndUsuarioId(10L, 2L)).thenReturn(Optional.empty()); // ponto 10 é da Ana

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> { service.excluir(10L, authBia); });

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(marcadores, never()).delete(any());
    }

    @Test
    void excluirPontoProprioApaga() {
        Marcador m = new Marcador();
        when(marcadores.findByIdAndUsuarioId(10L, 1L)).thenReturn(Optional.of(m));

        service.excluir(10L, authAna);

        verify(marcadores).delete(m);
    }

    @Test
    void semSessaoRetorna401() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> { service.listar(null); });
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    // ---- CA-03 / RF-10..12: link público ----

    @Test
    void obterLinkRetornaTokenDaConta() {
        assertEquals(new LinkPublicoResponse("Ana", "tok"), service.obterLink(authAna));
    }

    @Test
    void regenerarUsaONovoTokenDaConta() {
        when(contas.regenerarTokenMapa((ContaPrincipal) authAna.getPrincipal())).thenAnswer(inv -> {
            ana.setToken("novo");
            return ana;
        });
        when(marcadores.findByUsuarioIdOrderByCriadoEmDesc(1L)).thenReturn(List.of());

        assertEquals("novo", service.regenerar(authAna).token());
        assertEquals(404, status(() -> service.mapaPublico("Ana", "tok")));
        assertDoesNotThrow(() -> service.mapaPublico("Ana", "novo"));
    }

    @Test
    void mapaPublicoRetornaPontosSemDadosDoDonoEIgnoraCaixaNoNome() {
        Marcador m = new Marcador();
        m.setTitulo("Praia");
        m.setLatitude(-13.0);
        m.setLongitude(-38.4);
        when(marcadores.findByUsuarioIdOrderByCriadoEmDesc(1L)).thenReturn(List.of(m));

        List<MarcadorPublicoResponse> r = service.mapaPublico("ANA", "tok");

        assertEquals(1, r.size());
        assertEquals("Praia", r.get(0).titulo());
    }

    @Test
    void usuarioInexistenteETokenInvalidoDaoMesmaResposta() {

        ResponseStatusException semUsuario = assertThrows(ResponseStatusException.class,
                () -> { service.mapaPublico("fantasma", "tok"); });
        ResponseStatusException tokenErrado = assertThrows(ResponseStatusException.class,
                () -> { service.mapaPublico("Ana", "errado"); });

        assertEquals(HttpStatus.NOT_FOUND, semUsuario.getStatusCode());
        assertEquals(semUsuario.getStatusCode(), tokenErrado.getStatusCode());
        assertEquals(semUsuario.getReason(), tokenErrado.getReason());
    }

    // ---- auxiliares ----

    private int status(Runnable r) {
        return assertThrows(ResponseStatusException.class, r::run).getStatusCode().value();
    }

    private static Usuario usuario(Long id, String nome) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername(nome);
        u.setUsernameNormalizado(nome.toLowerCase(java.util.Locale.ROOT));
        u.setToken("tok");
        return u;
    }

    private static Authentication auth(Usuario usuario) {
        Authentication a = mock(Authentication.class);
        when(a.getPrincipal()).thenReturn(new ContaPrincipal(usuario));
        return a;
    }
}

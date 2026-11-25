package com.example.demo.marcador;

import com.example.demo.marcador.dto.MarcadorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Valida RD-04 e RD-05 (limites inclusivos) na camada web, sem Spring Security nem banco. */
class MarcadorControllerTest {

    MarcadorService service = mock(MarcadorService.class);
    MockMvc mvc = MockMvcBuilders.standaloneSetup(new MarcadorController(service)).build();

    @BeforeEach
    void setup() {
        when(service.criar(any(), any())).thenReturn(new MarcadorResponse(1L, "t", null, 0, 0));
    }

    private int enviar(String titulo, Object lat, Object lng) throws Exception {
        String json = String.format(Locale.ROOT,
                "{\"titulo\":%s,\"latitude\":%s,\"longitude\":%s}",
                titulo == null ? "null" : "\"" + titulo + "\"", lat, lng);
        return mvc.perform(post("/api/marcadores")
                        .principal(new UsernamePasswordAuthenticationToken("ana", null))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn().getResponse().getStatus();
    }

    @ParameterizedTest
    @CsvSource({"0,0", "90,180", "-90,-180", "89.9999,179.9999"})
    void coordenadasNosLimitesSaoAceitas(String lat, String lng) throws Exception {
        org.junit.jupiter.api.Assertions.assertEquals(201, enviar("Ponto", lat, lng));
    }

    @ParameterizedTest
    @CsvSource({"90.0001,0", "-90.0001,0", "0,180.0001", "0,-180.0001"})
    void coordenadasForaDosLimitesSaoRecusadas(String lat, String lng) throws Exception {
        org.junit.jupiter.api.Assertions.assertEquals(400, enviar("Ponto", lat, lng));
        verify(service, never()).criar(any(), any());
    }

    @Test
    void tituloVazioOuBrancoEhRecusado() throws Exception {
        org.junit.jupiter.api.Assertions.assertEquals(400, enviar("", 0, 0));
        org.junit.jupiter.api.Assertions.assertEquals(400, enviar("   ", 0, 0));
        org.junit.jupiter.api.Assertions.assertEquals(400, enviar(null, 0, 0));
    }

    @Test
    void tituloComLimite255() throws Exception {
        org.junit.jupiter.api.Assertions.assertEquals(201, enviar("a".repeat(255), 0, 0));
        org.junit.jupiter.api.Assertions.assertEquals(400, enviar("a".repeat(256), 0, 0));
    }

    @Test
    void coordenadaAusenteEhRecusada() throws Exception {
        mvc.perform(post("/api/marcadores")
                        .principal(new UsernamePasswordAuthenticationToken("ana", null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }
}

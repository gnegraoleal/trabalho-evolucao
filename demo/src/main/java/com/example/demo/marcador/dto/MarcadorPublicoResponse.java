package com.example.demo.marcador.dto;

import com.example.demo.marcador.Marcador;

/** Visão pública: sem id, sem dono, sem IP (RNF-04). */
public record MarcadorPublicoResponse(String titulo, String descricao, double latitude, double longitude) {
    public static MarcadorPublicoResponse de(Marcador m) {
        return new MarcadorPublicoResponse(m.getTitulo(), m.getDescricao(), m.getLatitude(), m.getLongitude());
    }
}

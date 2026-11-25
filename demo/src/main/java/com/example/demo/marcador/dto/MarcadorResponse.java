package com.example.demo.marcador.dto;

import com.example.demo.marcador.Marcador;

public record MarcadorResponse(Long id, String titulo, String descricao, double latitude, double longitude) {
    public static MarcadorResponse de(Marcador m) {
        return new MarcadorResponse(m.getId(), m.getTitulo(), m.getDescricao(), m.getLatitude(), m.getLongitude());
    }
}

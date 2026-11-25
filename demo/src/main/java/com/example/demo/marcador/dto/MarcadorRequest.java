package com.example.demo.marcador.dto;

import jakarta.validation.constraints.*;

/**
 * Sem campo de usuário: o titular é derivado da sessão (DT-08).
 * No legado Rails, :user_id estava na lista de parâmetros permitidos.
 */
public record MarcadorRequest(
        @NotBlank @Size(max = 255) String titulo,
        @Size(max = 2000) String descricao,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude
) {}

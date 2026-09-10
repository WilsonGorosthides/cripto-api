package br.com.wilson.criptoapi.alerta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Corpo de POST /api/v1/alertas. O usuario nao vem aqui: vem do token.
 * Condicao invalida nem chega a validacao - o Jackson recusa antes (400).
 */
public record NovoAlerta(
        @NotBlank String simbolo,
        @NotNull Condicao condicao,
        @NotNull @Positive BigDecimal valor) {
}

package br.com.wilson.criptoapi.alerta;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** O que sai sobre um alerta. Sem usuarioId: o dono e quem esta perguntando. */
public record AlertaResposta(
        Long id,
        String simbolo,
        Condicao condicao,
        BigDecimal valor,
        boolean ativo,
        OffsetDateTime criadoEm) {

    public static AlertaResposta de(Alerta a) {
        return new AlertaResposta(
                a.getId(), a.getSimbolo(), a.getCondicao(), a.getValor(), a.isAtivo(), a.getCriadoEm());
    }
}

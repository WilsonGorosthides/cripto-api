package br.com.wilson.criptoapi.alerta;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Um disparo: o preco e a coleta que fizeram a condicao valer. */
public record DisparoResposta(
        Long id,
        BigDecimal preco,
        OffsetDateTime coletadoEm,
        OffsetDateTime disparadoEm) {

    public static DisparoResposta de(AlertaDisparo d) {
        return new DisparoResposta(d.getId(), d.getPreco(), d.getColetadoEm(), d.getDisparadoEm());
    }
}

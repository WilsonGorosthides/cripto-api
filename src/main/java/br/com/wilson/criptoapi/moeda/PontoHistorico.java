package br.com.wilson.criptoapi.moeda;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Um ponto da serie temporal, no contrato da API.
 *
 * Deliberadamente enxuto. A entidade Preco tem 12 campos, mas repetir nome,
 * simbolo e moeda_fiat em cada um dos N pontos de uma serie seria carregar a
 * resposta com o mesmo valor N vezes - a moeda ja esta no caminho da URL.
 *
 * Ficou variacao24hPct alem do preco porque ela e a leitura daquele instante,
 * nao um dado da moeda: muda a cada coleta, como o preco.
 */
public record PontoHistorico(
        OffsetDateTime coletadoEm,
        BigDecimal preco,
        BigDecimal variacao24hPct
) {

    public static PontoHistorico de(Preco p) {
        return new PontoHistorico(p.getColetadoEm(), p.getPreco(), p.getVariacao24hPct());
    }
}

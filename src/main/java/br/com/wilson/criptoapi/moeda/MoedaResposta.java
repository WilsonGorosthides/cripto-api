package br.com.wilson.criptoapi.moeda;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * O CONTRATO da API para uma moeda. Nao confundir com a entidade MoedaAtual,
 * que descreve como ler a tabela.
 *
 * Sao duas classes de proposito. A entidade muda quando o schema do banco muda;
 * este record muda quando NOS decidimos mudar o que a API promete. Sem essa
 * separacao, renomear uma coluna no PostgreSQL quebraria todos os clientes.
 *
 * O que ficou de fora, e por que:
 *   - id: e a chave da LINHA na tabela, nao da moeda. Muda a cada coleta.
 *         Um cliente que o guardasse achando que identifica o Bitcoin
 *         quebraria em uma hora.
 *
 * O que ficou dentro, e por que:
 *   - moedaId: e o identificador estavel da CoinGecko ("bitcoin"). Esse sim
 *              serve para o cliente referenciar a moeda.
 *   - moedaFiat: sem ele, "preco" e ambiguo - 394041 do que?
 *
 * A ordem dos campos abaixo e a ordem de leitura humana: primeiro quem e a
 * moeda, depois quanto vale, depois os agregados, e por ultimo o carimbo
 * de tempo.
 */
public record MoedaResposta(
        Integer ranking,
        String moedaId,
        String simbolo,
        String nome,
        String moedaFiat,
        BigDecimal preco,
        BigDecimal variacao24hPct,
        BigDecimal marketCap,
        BigDecimal volume24h,
        BigDecimal ofertaCirculante,
        OffsetDateTime coletadoEm
) {

    /**
     * Traducao de entidade para contrato.
     *
     * Fica aqui, e nao na entidade, para que a direcao da dependencia seja
     * uma so: o contrato conhece a entidade, a entidade nao conhece o contrato.
     * Assim a MoedaAtual continua sendo apenas uma descricao da tabela.
     */
    public static MoedaResposta de(MoedaAtual m) {
        return new MoedaResposta(
                m.getRanking(),
                m.getMoedaId(),
                m.getSimbolo(),
                m.getNome(),
                m.getMoedaFiat(),
                m.getPreco(),
                m.getVariacao24hPct(),
                m.getMarketCap(),
                m.getVolume24h(),
                m.getOfertaCirculante(),
                m.getColetadoEm()
        );
    }
}

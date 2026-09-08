package br.com.wilson.criptoapi.moeda;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Uma leitura de preco, no instante em que foi coletada.
 *
 * Mapeia a TABELA precos_cripto - a serie historica inteira. A diferenca para
 * MoedaAtual, que mapeia a view: aquela tem uma linha por moeda, esta tem uma
 * linha por moeda POR COLETA. Sao granularidades diferentes sobre o mesmo dado,
 * e por isso duas entidades e nao uma.
 *
 * @Immutable pelo mesmo motivo de sempre: a tabela pertence ao cripto-pipeline.
 * Ver docs/adr/0002.
 */
@Entity
@Immutable
@Table(name = "precos_cripto")
public class Preco {

    /** Chave surrogate da linha, gerada pelo pipeline. Nunca sai no JSON. */
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "moeda_id")
    private String moedaId;

    @Column(name = "simbolo")
    private String simbolo;

    @Column(name = "nome")
    private String nome;

    @Column(name = "moeda_fiat")
    private String moedaFiat;

    @Column(name = "preco")
    private BigDecimal preco;

    @Column(name = "market_cap")
    private BigDecimal marketCap;

    @Column(name = "ranking")
    private Integer ranking;

    @Column(name = "volume_24h")
    private BigDecimal volume24h;

    @Column(name = "variacao_24h_pct")
    private BigDecimal variacao24hPct;

    @Column(name = "oferta_circulante")
    private BigDecimal ofertaCirculante;

    @Column(name = "coletado_em")
    private OffsetDateTime coletadoEm;

    protected Preco() {
    }

    public Long getId() {
        return id;
    }

    public String getMoedaId() {
        return moedaId;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public String getNome() {
        return nome;
    }

    public String getMoedaFiat() {
        return moedaFiat;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public BigDecimal getMarketCap() {
        return marketCap;
    }

    public Integer getRanking() {
        return ranking;
    }

    public BigDecimal getVolume24h() {
        return volume24h;
    }

    public BigDecimal getVariacao24hPct() {
        return variacao24hPct;
    }

    public BigDecimal getOfertaCirculante() {
        return ofertaCirculante;
    }

    public OffsetDateTime getColetadoEm() {
        return coletadoEm;
    }
}

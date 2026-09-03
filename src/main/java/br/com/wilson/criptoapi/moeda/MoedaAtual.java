package br.com.wilson.criptoapi.moeda;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Ultima coleta de cada moeda.
 *
 * Mapeia a VIEW vw_cripto_atual, e nao a tabela. A view ja resolve o
 * "qual e a linha mais recente" com DISTINCT ON, do lado do banco.
 *
 * @Immutable diz ao Hibernate que esta classe e somente leitura: ele nao
 * gera INSERT, UPDATE nem DELETE para ela, e pula a verificacao de alteracoes.
 * A tabela por tras pertence ao pipeline Python.
 */
@Entity
@Immutable
@Table(name = "vw_cripto_atual")
public class MoedaAtual {

    /**
     * A view nao tem chave primaria declarada - views nao tem. Mas JPA exige
     * um @Id em toda entidade, para saber identificar cada objeto no cache da
     * sessao. Usamos o id que a view carrega da tabela de origem, que e unico
     * na pratica. O Hibernate confia nessa afirmacao; ele nao consulta o banco
     * para verificar se existe constraint.
     */
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

    // BigDecimal, nao Double: o banco guarda numeric(24,8) e o valor e dinheiro.
    @Column(name = "preco")
    private BigDecimal preco;

    @Column(name = "market_cap")
    private BigDecimal marketCap;

    @Column(name = "ranking")
    private Integer ranking;

    // O nome explicito e obrigatorio aqui: a conversao automatica de
    // volume24h produziria "volume24h", e a coluna se chama "volume_24h".
    @Column(name = "volume_24h")
    private BigDecimal volume24h;

    @Column(name = "variacao_24h_pct")
    private BigDecimal variacao24hPct;

    @Column(name = "oferta_circulante")
    private BigDecimal ofertaCirculante;

    // OffsetDateTime preserva o fuso. A coluna e timestamptz, e trocar por
    // LocalDateTime jogaria fora a informacao de que o horario esta em UTC.
    @Column(name = "coletado_em")
    private OffsetDateTime coletadoEm;

    /** Exigido pela JPA: o Hibernate instancia a classe antes de preencher os campos. */
    protected MoedaAtual() {
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

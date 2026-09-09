package br.com.wilson.criptoapi.alerta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Registro de que um alerta disparou: com que preco e em qual coleta.
 * UNIQUE (alerta_id, coletado_em) no banco - um disparo por coleta.
 */
@Entity
@Table(name = "alerta_disparos")
public class AlertaDisparo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alerta_id", nullable = false)
    private Alerta alerta;

    @Column(name = "preco", nullable = false, precision = 24, scale = 8)
    private BigDecimal preco;

    /** A coleta que fez a condicao valer - nao o instante do job. */
    @Column(name = "coletado_em", nullable = false)
    private OffsetDateTime coletadoEm;

    @Column(name = "disparado_em", nullable = false)
    private OffsetDateTime disparadoEm;

    protected AlertaDisparo() {
        // exigido pelo JPA
    }

    public AlertaDisparo(Alerta alerta, BigDecimal preco, OffsetDateTime coletadoEm) {
        this.alerta = alerta;
        this.preco = preco;
        this.coletadoEm = coletadoEm;
        this.disparadoEm = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Alerta getAlerta() {
        return alerta;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public OffsetDateTime getColetadoEm() {
        return coletadoEm;
    }

    public OffsetDateTime getDisparadoEm() {
        return disparadoEm;
    }
}

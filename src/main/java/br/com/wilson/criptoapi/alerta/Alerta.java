package br.com.wilson.criptoapi.alerta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * "Avise quando {simbolo} ficar {condicao} de {valor}". Tabela da API (V1).
 *
 * usuarioId e um Long, nao um @ManyToOne Usuario: o alerta nunca precisa do
 * usuario carregado, so do id para filtrar. O FK no banco continua valendo.
 */
@Entity
@Table(name = "alertas")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "simbolo", nullable = false)
    private String simbolo;

    @Enumerated(EnumType.STRING)
    @Column(name = "condicao", nullable = false)
    private Condicao condicao;

    @Column(name = "valor", nullable = false, precision = 24, scale = 8)
    private BigDecimal valor;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    protected Alerta() {
        // exigido pelo JPA
    }

    public Alerta(Long usuarioId, String simbolo, Condicao condicao, BigDecimal valor) {
        this.usuarioId = usuarioId;
        this.simbolo = simbolo;
        this.condicao = condicao;
        this.valor = valor;
        this.ativo = true;
        this.criadoEm = OffsetDateTime.now();
    }

    /** Dispara uma vez e para (ADR 0013). Nao ha reativar. */
    public void desativar() {
        this.ativo = false;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public Condicao getCondicao() {
        return condicao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}

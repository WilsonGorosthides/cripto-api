package br.com.wilson.criptoapi.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Conta que pode ter alertas. Tabela da API, criada pelo Flyway (V1).
 *
 * Diferente de MoedaAtual e Preco, esta entidade e mutavel e a API escreve
 * nela - e a primeira tabela que e nossa (docs/adr/0011).
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // BIGSERIAL
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    protected Usuario() {
        // exigido pelo JPA
    }

    public Usuario(String email, String senhaHash) {
        this.email = email;
        this.senhaHash = senhaHash;
        this.criadoEm = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}

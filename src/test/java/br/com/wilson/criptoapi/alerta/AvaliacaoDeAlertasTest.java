package br.com.wilson.criptoapi.alerta;

import br.com.wilson.criptoapi.infra.ContainerDeTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unica regra de negocio do projeto (ADR 0013), testada sem relogio:
 * semeia precos e alertas, chama avaliar() e olha alerta_disparos.
 *
 * O agendamento esta desligado em teste (cripto.alertas.agendamento=false);
 * se estivesse ligado, o job poderia rodar no meio de um teste e mudar o
 * resultado de uma execucao para outra.
 */
@SpringBootTest
@Import(ContainerDeTeste.class)
class AvaliacaoDeAlertasTest {

    @Autowired
    private AvaliadorDeAlertas avaliador;

    @Autowired
    private JdbcTemplate jdbc;

    private long usuarioId;

    @BeforeEach
    void limpar() {
        jdbc.update("DELETE FROM usuarios");
        jdbc.update("DELETE FROM precos_cripto");
        usuarioId = jdbc.queryForObject(
                "INSERT INTO usuarios (email, senha_hash) VALUES ('ana@exemplo.com', 'x') RETURNING id",
                Long.class);
    }

    @Test
    void disparaQuandoOPrecoAtualEstaAcimaDoValor() {
        preco("BTC", "410000", "2026-09-01T11:00:00Z");
        long alerta = alerta("BTC", "ACIMA", "400000", true);

        int disparados = avaliador.avaliar();

        assertEquals(1, disparados);
        assertEquals(1, disparos(alerta));
        assertFalse(ativo(alerta), "alerta deveria ter sido desativado ao disparar");

        Map<String, Object> disparo = jdbc.queryForMap(
                "SELECT preco, coletado_em FROM alerta_disparos WHERE alerta_id = ?", alerta);
        assertEquals(0, new BigDecimal("410000").compareTo((BigDecimal) disparo.get("preco")));
        assertTrue(disparo.get("coletado_em").toString().startsWith("2026-09-01"));
    }

    @Test
    void disparaQuandoOPrecoAtualEstaAbaixoDoValor() {
        preco("BTC", "390000", "2026-09-01T11:00:00Z");
        long alerta = alerta("BTC", "ABAIXO", "400000", true);

        assertEquals(1, avaliador.avaliar());
        assertEquals(1, disparos(alerta));
    }

    @Test
    void naoDisparaQuandoACondicaoNaoVale() {
        preco("BTC", "390000", "2026-09-01T11:00:00Z");
        long alerta = alerta("BTC", "ACIMA", "400000", true);

        assertEquals(0, avaliador.avaliar());
        assertEquals(0, disparos(alerta));
        assertTrue(ativo(alerta));
    }

    @Test
    void soAColetaMaisRecenteConta() {
        // A coleta antiga dispararia; a mais recente, nao. Vale a mais recente.
        preco("BTC", "410000", "2026-09-01T10:00:00Z");
        preco("BTC", "390000", "2026-09-01T11:00:00Z");
        long alerta = alerta("BTC", "ACIMA", "400000", true);

        assertEquals(0, avaliador.avaliar());
        assertEquals(0, disparos(alerta));
    }

    @Test
    void disparaUmaVezEDesativa() {
        preco("BTC", "410000", "2026-09-01T11:00:00Z");
        long alerta = alerta("BTC", "ACIMA", "400000", true);

        avaliador.avaliar();
        avaliador.avaliar();                                   // mesma coleta
        preco("BTC", "420000", "2026-09-01T12:00:00Z");        // coleta nova, ainda acima
        avaliador.avaliar();

        assertEquals(1, disparos(alerta));
        assertFalse(ativo(alerta));
    }

    @Test
    void alertaInativoNaoEAvaliado() {
        preco("BTC", "410000", "2026-09-01T11:00:00Z");
        long alerta = alerta("BTC", "ACIMA", "400000", false);

        assertEquals(0, avaliador.avaliar());
        assertEquals(0, disparos(alerta));
    }

    @Test
    void moedaSemColetaNaoDisparaNemQuebra() {
        long alerta = alerta("XYZ", "ACIMA", "1", true);

        assertEquals(0, avaliador.avaliar());
        assertEquals(0, disparos(alerta));
        assertTrue(ativo(alerta));
    }

    // ------------------------------------------------------------------ apoio

    private void preco(String simbolo, String valor, String instante) {
        jdbc.update("""
                INSERT INTO precos_cripto
                    (moeda_id, simbolo, nome, moeda_fiat, preco, market_cap, ranking,
                     volume_24h, variacao_24h_pct, oferta_circulante, coletado_em)
                VALUES (lower(?), ?, ?, 'BRL', CAST(? AS NUMERIC), 1000, 1, 500, 1.5, 100,
                        CAST(? AS TIMESTAMPTZ))
                """, simbolo, simbolo, simbolo, valor, instante);
    }

    private long alerta(String simbolo, String condicao, String valor, boolean ativo) {
        return jdbc.queryForObject("""
                INSERT INTO alertas (usuario_id, simbolo, condicao, valor, ativo)
                VALUES (?, ?, ?, CAST(? AS NUMERIC), ?) RETURNING id
                """, Long.class, usuarioId, simbolo, condicao, valor, ativo);
    }

    private int disparos(long alertaId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM alerta_disparos WHERE alerta_id = ?", Integer.class, alertaId);
    }

    private boolean ativo(long alertaId) {
        return jdbc.queryForObject(
                "SELECT ativo FROM alertas WHERE id = ?", Boolean.class, alertaId);
    }
}

package br.com.wilson.criptoapi.infra;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prova que o Flyway criou as tabelas da API e NAO tocou nas do pipeline.
 *
 * O container nasce com o espelho do schema do pipeline (ADR 0010) e o Flyway
 * roda em cima dele no boot do contexto (ADR 0011). Este teste olha o
 * resultado dos dois juntos, direto no catalogo do PostgreSQL.
 */
@SpringBootTest
@Import(ContainerDeTeste.class)
class MigracoesTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void flywayCriaAsTresTabelasDaApi() {
        List<String> tabelas = jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """, String.class);

        assertTrue(tabelas.contains("usuarios"), "faltou usuarios em " + tabelas);
        assertTrue(tabelas.contains("alertas"), "faltou alertas em " + tabelas);
        assertTrue(tabelas.contains("alerta_disparos"), "faltou alerta_disparos em " + tabelas);
    }

    @Test
    void migracaoInicialFoiAplicadaUmaVezESemErro() {
        // Regra 3 do ADR 0011: baseline-version=0 faz a V1 rodar de verdade.
        // Sem ela, o Flyway marcaria o banco como "ja na versao 1" e pularia
        // a migration - e este teste e o unico lugar em que isso apareceria.
        Integer aplicadas = jdbc.queryForObject("""
                SELECT count(*) FROM flyway_schema_history
                WHERE version = '1' AND success = true
                """, Integer.class);

        assertEquals(1, aplicadas);
    }

    @Test
    void precosCriptoContinuaComAsColunasDoPipeline() {
        // Regra 1 do ADR 0011: nenhuma migration toca na tabela do pipeline.
        // O espelho tem 12 colunas; se aparecer uma 13a, alguem cruzou a fronteira.
        Integer colunas = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'precos_cripto'
                """, Integer.class);

        assertEquals(12, colunas);
    }
}

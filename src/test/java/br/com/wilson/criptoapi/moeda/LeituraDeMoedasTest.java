package br.com.wilson.criptoapi.moeda;

import br.com.wilson.criptoapi.comum.PaginaResposta;
import br.com.wilson.criptoapi.infra.ContainerDeTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de integracao da leitura de moedas, contra PostgreSQL de verdade.
 *
 * O container e o espelho do schema do pipeline vem de ContainerDeTeste; o
 * ddl-auto=validate vem de src/test/resources/application.properties. As duas
 * coisas sao a mitigacao 1 do ADR 0010.
 */
@SpringBootTest
@Import(ContainerDeTeste.class)
class LeituraDeMoedasTest {

    @Autowired
    private MoedaService service;

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * Duas moedas, duas coletas cada. A escrita e feita por JdbcTemplate porque
     * as entidades sao @Immutable - a API nao escreve nesta tabela, e o teste
     * respeita essa fronteira em vez de contorna-la.
     */
    @BeforeEach
    void semear() {
        jdbc.update("DELETE FROM precos_cripto");
        inserir("bitcoin", "BTC", "Bitcoin", 1, "400000.00000000", "2026-09-01T10:00:00Z");
        inserir("bitcoin", "BTC", "Bitcoin", 1, "410000.00000000", "2026-09-01T11:00:00Z");
        inserir("ethereum", "ETH", "Ethereum", 2, "12000.00000000", "2026-09-01T10:00:00Z");
        inserir("ethereum", "ETH", "Ethereum", 2, "12500.00000000", "2026-09-01T11:00:00Z");
    }

    private void inserir(String moedaId, String simbolo, String nome,
                         int ranking, String preco, String instante) {
        jdbc.update("""
                INSERT INTO precos_cripto
                    (moeda_id, simbolo, nome, moeda_fiat, preco, market_cap, ranking,
                     volume_24h, variacao_24h_pct, oferta_circulante, coletado_em)
                VALUES (?, ?, ?, 'BRL', CAST(? AS NUMERIC), 1000, ?, 500, 1.5, 100,
                        CAST(? AS TIMESTAMPTZ))
                """, moedaId, simbolo, nome, preco, ranking, instante);
    }

    @Test
    void listarAtuaisDevolveUmaLinhaPorMoeda() {
        List<MoedaResposta> atuais = service.listarAtuais();

        assertEquals(2, atuais.size(), "a view deveria deduplicar por moeda");
        assertEquals("BTC", atuais.get(0).simbolo(), "ordenado por ranking");
        assertEquals("ETH", atuais.get(1).simbolo());
    }

    @Test
    void listarAtuaisTrazAColetaMaisRecente() {
        MoedaResposta btc = service.listarAtuais().get(0);

        assertEquals(0, btc.preco().compareTo(new BigDecimal("410000.00000000")),
                "o DISTINCT ON deveria trazer a leitura das 11h, nao a das 10h");
    }

    @Test
    void historicoVemDoMaisRecenteParaOMaisAntigo() {
        PaginaResposta<PontoHistorico> pagina =
                service.buscarHistorico("BTC", PageRequest.of(0, 10));

        assertEquals(2, pagina.totalDeItens());
        assertEquals(1, pagina.totalDePaginas());
        assertTrue(pagina.primeira());
        assertTrue(pagina.ultima());

        assertEquals(0, pagina.conteudo().get(0).preco()
                .compareTo(new BigDecimal("410000.00000000")));
        assertEquals(0, pagina.conteudo().get(1).preco()
                .compareTo(new BigDecimal("400000.00000000")));
    }

    @Test
    void historicoPaginaDeVerdade() {
        PaginaResposta<PontoHistorico> pagina =
                service.buscarHistorico("BTC", PageRequest.of(0, 1));

        assertEquals(1, pagina.conteudo().size(), "size=1 devolve um item");
        assertEquals(2, pagina.totalDeItens(), "mas o total continua dois");
        assertEquals(2, pagina.totalDePaginas());
        assertTrue(pagina.primeira());
        assertFalse(pagina.ultima());
    }

    @Test
    void simboloEmMinusculoEncontraAMesmaSerie() {
        assertEquals(service.buscarHistorico("BTC", PageRequest.of(0, 10)).totalDeItens(),
                service.buscarHistorico("btc", PageRequest.of(0, 10)).totalDeItens(),
                "o IgnoreCase do repositorio deveria tornar isso indiferente");
    }

    /** Documenta o defeito conhecido: deveria ser 404. O bloco 7 resolve. */
    @Test
    void moedaInexistenteDevolvePaginaVaziaEmVezDeErro() {
        PaginaResposta<PontoHistorico> pagina =
                service.buscarHistorico("XPTO", PageRequest.of(0, 10));

        assertEquals(0, pagina.totalDeItens());
        assertTrue(pagina.conteudo().isEmpty());
    }
}

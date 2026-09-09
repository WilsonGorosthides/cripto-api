package br.com.wilson.criptoapi.infra;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de infraestrutura, nao de funcionalidade.
 *
 * Ele nao verifica nenhuma regra da aplicacao. Existe para responder uma
 * pergunta so: o Maven consegue subir um container? Se este teste falhar, o
 * problema e Docker, nao codigo - e saber disso de antemao evita horas
 * depurando a aplicacao errada.
 *
 * Deliberadamente NAO sobe o contexto do Spring. Quanto menos peca envolvida,
 * mais preciso o diagnostico quando quebrar.
 */
@Testcontainers
class DockerDisponivelTest {

    /**
     * static de proposito: com @Container em campo estatico, o container sobe
     * UMA vez para a classe inteira. Em campo de instancia, subiria um por
     * metodo de teste - correto, porem lento.
     *
     * A tag "17-alpine" fixa a versao. Usar "latest" faria o teste depender de
     * qual imagem estava publicada no dia, que e o oposto de reproduzivel.
     */
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Test
    void containerSobeEAceitaConexao() throws Exception {
        assertTrue(postgres.isRunning(), "o container deveria estar rodando");

        try (Connection conexao = DriverManager.getConnection(
                     postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement comando = conexao.createStatement();
             ResultSet resultado = comando.executeQuery("SELECT version()")) {

            assertTrue(resultado.next(), "SELECT version() deveria devolver uma linha");
            System.out.println("  [infra] versao no container : " + resultado.getString(1));
        }
    }

    /**
     * A prova de que nao ha conflito com o PostgreSQL do Windows: o container
     * escuta na 5432 DELE, mas o Testcontainers publica isso numa porta alta
     * sorteada pelo sistema operacional. Nunca a 5432 do hospedeiro.
     */
    @Test
    void portaPublicadaNaoColideComOPostgresLocal() {
        Integer portaNoHospedeiro = postgres.getMappedPort(5432);

        System.out.println("  [infra] porta publicada     : " + portaNoHospedeiro);
        System.out.println("  [infra] jdbcUrl             : " + postgres.getJdbcUrl());

        assertNotEquals(5432, portaNoHospedeiro,
                "se fosse 5432 colidiria com o PostgreSQL do Windows");
        assertTrue(portaNoHospedeiro > 1024, "deveria ser uma porta alta, nao privilegiada");
    }

    /**
     * O banco do container nasce VAZIO. A tabela precos_cripto pertence ao
     * pipeline Python e nao existe aqui - e esse e o proximo problema a
     * resolver, antes de qualquer teste de integracao de verdade.
     */
    @Test
    void bancoDoContainerNasceSemAsTabelasDoPipeline() throws Exception {
        try (Connection conexao = DriverManager.getConnection(
                     postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement comando = conexao.createStatement();
             ResultSet resultado = comando.executeQuery(
                     "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'")) {

            resultado.next();
            int tabelas = resultado.getInt(1);
            System.out.println("  [infra] tabelas no schema public : " + tabelas);

            assertEquals(0, tabelas, "o container deveria comecar sem tabela nenhuma");
        }
    }
}

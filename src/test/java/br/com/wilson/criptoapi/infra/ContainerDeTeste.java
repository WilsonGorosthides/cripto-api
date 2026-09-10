package br.com.wilson.criptoapi.infra;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * O PostgreSQL de teste, declarado como bean.
 *
 * Definir o container como @Bean, em vez de campo estatico com @Container, tem
 * duas consequencias praticas:
 *
 *   - o Spring gerencia o ciclo de vida, e reaproveita o MESMO container entre
 *     classes de teste que compartilham o contexto. Com campo estatico, cada
 *     classe subiria o seu;
 *   - @ServiceConnection injeta url, usuario e senha do container nas
 *     propriedades do datasource. Nenhum teste precisa saber a porta sorteada.
 *
 * O withInitScript aplica o espelho do schema do pipeline (docs/adr/0010) na
 * criacao do container, antes de qualquer teste rodar.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ContainerDeTeste {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17-alpine")
                .withInitScript("schema-pipeline.sql");
    }
}

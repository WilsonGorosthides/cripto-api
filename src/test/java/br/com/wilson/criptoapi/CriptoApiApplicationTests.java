package br.com.wilson.criptoapi;

import br.com.wilson.criptoapi.infra.ContainerDeTeste;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Teste de fumaca do contexto: todos os beans sao construidos e conectados.
 *
 * Parece pouco, mas pega uma classe de erro que nenhum outro teste pega -
 * dependencia ciclica, bean sem candidato, propriedade obrigatoria ausente.
 * Sao falhas de INICIALIZACAO, e so aparecem quando o contexto inteiro sobe.
 */
@SpringBootTest
@Import(ContainerDeTeste.class)
class CriptoApiApplicationTests {

    @Test
    void contextLoads() {
    }
}

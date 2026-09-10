package br.com.wilson.criptoapi.comum;

import br.com.wilson.criptoapi.infra.ContainerDeTeste;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Todo erro que passa pelo MVC sai como application/problem+json (ADR 0015),
 * com status, detail e instance - venha de excecao nossa ou do proprio Spring.
 *
 * O 401 do filtro de seguranca (sem token) NAO esta aqui: acontece antes do
 * MVC e sai sem corpo. Inconsistencia registrada no ADR, nao testada como
 * se fosse formato.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ContainerDeTeste.class)
class TratamentoDeErroTest {

    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    @Autowired
    private MockMvc http;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limpar() {
        jdbc.update("DELETE FROM usuarios");
        jdbc.update("DELETE FROM precos_cripto");
    }

    // ------------------------------------------------------- excecoes nossas

    @Test
    void historicoDeMoedaInexistenteDevolve404ProblemDetail() throws Exception {
        http.perform(get("/api/v1/moedas/XPTO/historico"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("XPTO")))
                .andExpect(jsonPath("$.instance").value("/api/v1/moedas/XPTO/historico"));
    }

    @Test
    void emailRepetidoDevolve409ProblemDetail() throws Exception {
        String credenciais = """
                {"email": "ana@exemplo.com", "senha": "senha-forte-123"}
                """;
        http.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON).content(credenciais))
                .andExpect(status().isCreated());

        http.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON).content(credenciais))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("ana@exemplo.com")))
                .andExpect(jsonPath("$.instance").value("/api/v1/auth/registrar"));
    }

    @Test
    void loginErradoDevolve401ProblemDetail() throws Exception {
        http.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ninguem@exemplo.com", "senha": "tanto-faz-123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").isString());
    }

    // ---------------------------------------------------- excecoes do Spring

    @Test
    void validacaoDevolve400ComListaDeCampos() throws Exception {
        http.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nao-e-email", "senha": "curta"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erros.length()").value(2))
                .andExpect(jsonPath("$.erros[*].campo").value(Matchers.containsInAnyOrder("email", "senha")))
                .andExpect(jsonPath("$.erros[*].mensagem").value(Matchers.everyItem(Matchers.not(Matchers.emptyString()))));
    }

    @Test
    void corpoIlegivelDevolve400ProblemDetail() throws Exception {
        http.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{isto nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rotaInexistenteDevolve404ProblemDetail() throws Exception {
        // Sob /api/v1/moedas/** para passar pelo filtro (publico) e chegar ao MVC.
        http.perform(get("/api/v1/moedas/BTC/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }
}

package br.com.wilson.criptoapi.alerta;

import br.com.wilson.criptoapi.infra.ContainerDeTeste;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP dos alertas: criar, listar, apagar e ver disparos.
 *
 * Toda rota exige Bearer (ADR 0012) e todo alerta pertence a quem o criou.
 * "De outro usuario" responde 404, nao 403: 403 confirmaria que o id existe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ContainerDeTeste.class)
class AlertasTest {

    private static final String SENHA = "senha-forte-123";

    @Autowired
    private MockMvc http;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limpar() {
        jdbc.update("DELETE FROM usuarios"); // cascata leva alertas e disparos
    }

    // ------------------------------------------------------------------ criar

    @Test
    void criarAlertaDevolve201ComOsCampos() throws Exception {
        String token = tokenDe("ana@exemplo.com");

        http.perform(post("/api/v1/alertas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alerta("BTC", "ACIMA", "400000")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.simbolo").value("BTC"))
                .andExpect(jsonPath("$.condicao").value("ACIMA"))
                .andExpect(jsonPath("$.valor").value(400000))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.criadoEm").isString());
    }

    @Test
    void simboloEmMinusculoEGuardadoEmMaiusculo() throws Exception {
        String token = tokenDe("ana@exemplo.com");

        http.perform(post("/api/v1/alertas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alerta("btc", "ABAIXO", "350000")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.simbolo").value("BTC"));
    }

    @Test
    void criarSemTokenDevolve401() throws Exception {
        http.perform(post("/api/v1/alertas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alerta("BTC", "ACIMA", "400000")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void condicaoInvalidaDevolve400() throws Exception {
        String token = tokenDe("ana@exemplo.com");

        http.perform(post("/api/v1/alertas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alerta("BTC", "IGUAL", "400000")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void valorNegativoDevolve400() throws Exception {
        String token = tokenDe("ana@exemplo.com");

        http.perform(post("/api/v1/alertas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alerta("BTC", "ACIMA", "-1")))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------------- listar

    @Test
    void listarDevolveSoOsAlertasDoProprioUsuario() throws Exception {
        String ana = tokenDe("ana@exemplo.com");
        String bia = tokenDe("bia@exemplo.com");
        criar(ana, "BTC", "ACIMA", "400000");
        criar(ana, "ETH", "ABAIXO", "10000");
        criar(bia, "BTC", "ABAIXO", "300000");

        http.perform(get("/api/v1/alertas")
                        .header("Authorization", "Bearer " + ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].simbolo").value(org.hamcrest.Matchers.containsInAnyOrder("BTC", "ETH")));
    }

    @Test
    void disparosDeAlertaNovoEListaVazia() throws Exception {
        String token = tokenDe("ana@exemplo.com");
        long id = criar(token, "BTC", "ACIMA", "400000");

        http.perform(get("/api/v1/alertas/{id}/disparos", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ----------------------------------------------------------------- apagar

    @Test
    void apagarDoProprioUsuarioDevolve204() throws Exception {
        String token = tokenDe("ana@exemplo.com");
        long id = criar(token, "BTC", "ACIMA", "400000");

        http.perform(delete("/api/v1/alertas/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        http.perform(get("/api/v1/alertas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void apagarDeOutroUsuarioDevolve404EPreservaOAlerta() throws Exception {
        String ana = tokenDe("ana@exemplo.com");
        String bia = tokenDe("bia@exemplo.com");
        long id = criar(ana, "BTC", "ACIMA", "400000");

        http.perform(delete("/api/v1/alertas/{id}", id)
                        .header("Authorization", "Bearer " + bia))
                .andExpect(status().isNotFound());

        http.perform(get("/api/v1/alertas")
                        .header("Authorization", "Bearer " + ana))
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ------------------------------------------------------------------ apoio

    private static String alerta(String simbolo, String condicao, String valor) {
        return """
                {"simbolo": "%s", "condicao": "%s", "valor": %s}
                """.formatted(simbolo, condicao, valor);
    }

    private long criar(String token, String simbolo, String condicao, String valor) throws Exception {
        MvcResult resultado = http.perform(post("/api/v1/alertas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alerta(simbolo, condicao, valor)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private String tokenDe(String email) throws Exception {
        String credenciais = """
                {"email": "%s", "senha": "%s"}
                """.formatted(email, SENHA);
        http.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais))
                .andExpect(status().isCreated());
        MvcResult login = http.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.token");
    }
}

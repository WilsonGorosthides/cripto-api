package br.com.wilson.criptoapi.usuario;

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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP de registro, login e acesso protegido (ADR 0012).
 *
 * Testa pela borda - requisicao entra, resposta sai - e nao pelas classes,
 * de proposito: o que o cliente ve e o contrato, e e isso que nao pode
 * mudar sem querer. O banco e o container real, com o Flyway ja aplicado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ContainerDeTeste.class)
class AutenticacaoTest {

    private static final String EMAIL = "ana@exemplo.com";
    private static final String SENHA = "senha-forte-123";

    @Autowired
    private MockMvc http;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limpar() {
        // ON DELETE CASCADE leva alertas e disparos junto.
        jdbc.update("DELETE FROM usuarios");
    }

    // ---------------------------------------------------------------- registro

    @Test
    void registrarDevolve201ComIdEEmailESemSenha() throws Exception {
        http.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(EMAIL, SENHA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void emailRepetidoDevolve409() throws Exception {
        registrar(EMAIL, SENHA);

        http.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(EMAIL, "outra-senha-456")))
                .andExpect(status().isConflict());
    }

    @Test
    void senhaCurtaDevolve400() throws Exception {
        http.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(EMAIL, "curta")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void senhaNaoFicaEmClaroNoBanco() throws Exception {
        registrar(EMAIL, SENHA);

        String guardado = jdbc.queryForObject(
                "SELECT senha_hash FROM usuarios WHERE email = ?", String.class, EMAIL);

        assertNotEquals(SENHA, guardado);
        // DelegatingPasswordEncoder prefixa o algoritmo; BCrypt comeca com $2.
        assertTrue(guardado.startsWith("{bcrypt}$2"), "hash inesperado: " + guardado);
    }

    // ------------------------------------------------------------------- login

    @Test
    void loginCorretoDevolveToken() throws Exception {
        registrar(EMAIL, SENHA);

        http.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(EMAIL, SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.expiraEm").isString());
    }

    @Test
    void loginComSenhaErradaDevolve401() throws Exception {
        registrar(EMAIL, SENHA);

        http.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(EMAIL, "errada-errada-1")))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------- rota protegida

    @Test
    void rotaProtegidaSemTokenDevolve401() throws Exception {
        http.perform(get("/api/v1/auth/eu"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotaProtegidaComTokenIdentificaOUsuario() throws Exception {
        registrar(EMAIL, SENHA);
        String token = login(EMAIL, SENHA);

        http.perform(get("/api/v1/auth/eu")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void leituraDeMoedasContinuaPublica() throws Exception {
        // O que ja funcionava nao pode passar a exigir token (ADR 0012).
        http.perform(get("/api/v1/moedas"))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- apoio

    private static String credenciais(String email, String senha) {
        return """
                {"email": "%s", "senha": "%s"}
                """.formatted(email, senha);
    }

    private void registrar(String email, String senha) throws Exception {
        http.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(email, senha)))
                .andExpect(status().isCreated());
    }

    private String login(String email, String senha) throws Exception {
        MvcResult resultado = http.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(email, senha)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
    }
}

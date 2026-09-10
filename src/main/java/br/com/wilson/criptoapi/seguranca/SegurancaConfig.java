package br.com.wilson.criptoapi.seguranca;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Regras de acesso e as pecas do JWT (docs/adr/0012).
 *
 * A API e resource server de si mesma: o mesmo Nimbus que valida token de
 * terceiros valida o que ela emite. O segredo HMAC e um so, entra por
 * variavel de ambiente e alimenta o decoder (validar) e o encoder (emitir).
 */
@Configuration
@EnableConfigurationProperties(PropriedadesJwt.class)
public class SegurancaConfig {

    /** HS256 exige chave de no minimo 256 bits. */
    private static final int MINIMO_BYTES_SEGREDO = 32;

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
                // Sem sessao e sem cookie, CSRF nao tem o que proteger: o token
                // vai no cabecalho, e cabecalho o navegador nao manda sozinho.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        // O que ja era publico continua publico.
                        .requestMatchers(HttpMethod.GET, "/api/v1/moedas/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health/**").permitAll()
                        // Sem isto ninguem conseguiria obter o primeiro token.
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/registrar", "/api/v1/auth/login").permitAll()
                        .anyRequest().authenticated())
                // Liga o filtro que le "Authorization: Bearer ..." e usa o
                // JwtDecoder abaixo. Sem token -> 401 pelo entry point padrao.
                .oauth2ResourceServer(servidor -> servidor.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /**
     * Delegating em vez de BCryptPasswordEncoder direto: o hash sai com o
     * prefixo {bcrypt}, e trocar de algoritmo depois e trocar o padrao aqui,
     * sem migrar dado - o Spring reconhece o antigo pelo prefixo.
     */
    @Bean
    PasswordEncoder codificadorDeSenha() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    JwtDecoder decodificadorJwt(PropriedadesJwt propriedades) {
        return NimbusJwtDecoder.withSecretKey(chave(propriedades))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    JwtEncoder codificadorJwt(PropriedadesJwt propriedades) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave(propriedades)));
    }

    private static SecretKey chave(PropriedadesJwt propriedades) {
        String segredo = propriedades.segredo();
        if (segredo == null || segredo.isBlank()) {
            throw new IllegalStateException(
                    "CRIPTO_JWT_SEGREDO nao definido. A API nao sobe sem segredo de assinatura.");
        }
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MINIMO_BYTES_SEGREDO) {
            throw new IllegalStateException(
                    "CRIPTO_JWT_SEGREDO precisa de ao menos " + MINIMO_BYTES_SEGREDO
                            + " bytes; tem " + bytes.length + ".");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}

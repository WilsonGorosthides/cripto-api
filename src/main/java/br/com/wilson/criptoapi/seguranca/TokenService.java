package br.com.wilson.criptoapi.seguranca;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Emite o JWT no login (docs/adr/0012).
 *
 * Recebe id e email, nao a entidade, para que o pacote seguranca nao dependa
 * do pacote usuario - a dependencia vai numa direcao so.
 */
@Service
public class TokenService {

    /** Quem emitiu e quando expira. O que o cliente precisa guardar. */
    public record Emitido(String token, Instant expiraEm) {
    }

    private final JwtEncoder codificador;
    private final PropriedadesJwt propriedades;

    public TokenService(JwtEncoder codificador, PropriedadesJwt propriedades) {
        this.codificador = codificador;
        this.propriedades = propriedades;
    }

    public Emitido emitir(Long usuarioId, String email) {
        Instant agora = Instant.now();
        Instant expira = agora.plus(propriedades.validade());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cripto-api")
                .issuedAt(agora)
                .expiresAt(expira)
                .subject(String.valueOf(usuarioId))
                .claim("email", email)
                .build();

        // O cabecalho HS256 e obrigatorio: sem ele o NimbusJwtEncoder assume
        // RS256, procura uma chave RSA que nao existe e falha ao assinar.
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = codificador.encode(JwtEncoderParameters.from(cabecalho, claims))
                .getTokenValue();
        return new Emitido(token, expira);
    }
}

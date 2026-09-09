package br.com.wilson.criptoapi.seguranca;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuracao do JWT, lida de cripto.jwt.* (docs/adr/0012).
 *
 * O segredo chega por CRIPTO_JWT_SEGREDO - relaxed binding, como a senha do
 * banco. A validacao de tamanho fica em SegurancaConfig, que e quem usa.
 */
@ConfigurationProperties("cripto.jwt")
public record PropriedadesJwt(String segredo, Duration validade) {
}

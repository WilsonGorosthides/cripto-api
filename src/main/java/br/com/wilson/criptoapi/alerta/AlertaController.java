package br.com.wilson.criptoapi.alerta;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Todas as rotas exigem Bearer (anyRequest().authenticated() em
 * SegurancaConfig). O usuario vem do subject do token, nunca da URL ou do
 * corpo - senao bastaria trocar o id na requisicao.
 */
@RestController
@RequestMapping("/api/v1/alertas")
public class AlertaController {

    private final AlertaService service;

    public AlertaController(AlertaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlertaResposta criar(@AuthenticationPrincipal Jwt jwt,
                                @Valid @RequestBody NovoAlerta novo) {
        return service.criar(usuarioDe(jwt), novo);
    }

    @GetMapping
    public List<AlertaResposta> listar(@AuthenticationPrincipal Jwt jwt) {
        return service.listar(usuarioDe(jwt));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.apagar(usuarioDe(jwt), id);
    }

    @GetMapping("/{id}/disparos")
    public List<DisparoResposta> disparos(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.disparos(usuarioDe(jwt), id);
    }

    private static Long usuarioDe(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}

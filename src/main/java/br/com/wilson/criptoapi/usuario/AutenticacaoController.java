package br.com.wilson.criptoapi.usuario;

import br.com.wilson.criptoapi.seguranca.TokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /registrar e POST /login sao publicos por configuracao
 * (SegurancaConfig); GET /eu exige Bearer e existe para provar que o token
 * emitido e aceito de volta - e para o cliente descobrir quem ele e.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AutenticacaoController {

    private final AutenticacaoService service;

    public AutenticacaoController(AutenticacaoService service) {
        this.service = service;
    }

    /** 201, nao 200: um recurso novo passou a existir. */
    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResposta registrar(@Valid @RequestBody Credenciais credenciais) {
        return service.registrar(credenciais);
    }

    @PostMapping("/login")
    public TokenService.Emitido login(@Valid @RequestBody Credenciais credenciais) {
        return service.login(credenciais);
    }

    /**
     * O Jwt ja chegou validado pelo filtro: assinatura conferida e prazo
     * dentro da validade. Aqui so se le o subject, que e o id do usuario.
     */
    @GetMapping("/eu")
    public UsuarioResposta eu(@AuthenticationPrincipal Jwt jwt) {
        return service.buscar(Long.valueOf(jwt.getSubject()));
    }
}

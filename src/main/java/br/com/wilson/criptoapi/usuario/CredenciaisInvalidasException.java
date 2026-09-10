package br.com.wilson.criptoapi.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * 401 no login. A mesma excecao para "email nao existe" e "senha errada",
 * de proposito: distinguir os dois diria a quem tenta adivinhar quais emails
 * estao cadastrados. Formato: ADR 0015.
 */
public class CredenciaisInvalidasException extends ErrorResponseException {

    public CredenciaisInvalidasException() {
        super(HttpStatus.UNAUTHORIZED,
                ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                        "Email ou senha invalidos"),
                null);
    }
}

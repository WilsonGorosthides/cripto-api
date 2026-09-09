package br.com.wilson.criptoapi.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 401 no login. A mesma excecao para "email nao existe" e "senha errada",
 * de proposito: distinguir os dois diria a quem tenta adivinhar quais emails
 * estao cadastrados.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("Email ou senha invalidos");
    }
}

package br.com.wilson.criptoapi.alerta;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 404 tanto para "nao existe" quanto para "existe, mas e de outro usuario".
 * Um 403 no segundo caso confirmaria que o id existe - informacao que o
 * outro usuario nao tem por que receber.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class AlertaNaoEncontradoException extends RuntimeException {

    public AlertaNaoEncontradoException(Long id) {
        super("Alerta " + id + " nao encontrado");
    }
}

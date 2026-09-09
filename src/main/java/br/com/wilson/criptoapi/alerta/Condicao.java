package br.com.wilson.criptoapi.alerta;

/**
 * Espelha o CHECK ck_alertas_condicao da V1. Um valor fora destes dois e
 * recusado duas vezes: pelo Jackson ao ler o JSON (400) e pelo banco.
 */
public enum Condicao {
    ACIMA,
    ABAIXO
}

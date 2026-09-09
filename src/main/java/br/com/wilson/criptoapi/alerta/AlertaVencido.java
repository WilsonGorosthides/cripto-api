package br.com.wilson.criptoapi.alerta;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Resultado da consulta que cruza alertas ativos com a ultima coleta:
 * o alerta cuja condicao vale, e o preco/coleta que fizeram valer.
 * Construido pela JPQL de AlertaRepository.encontrarVencidos.
 */
public record AlertaVencido(Alerta alerta, BigDecimal preco, OffsetDateTime coletadoEm) {
}

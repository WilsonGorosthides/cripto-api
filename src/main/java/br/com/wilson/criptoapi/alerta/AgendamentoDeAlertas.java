package br.com.wilson.criptoapi.alerta;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Liga o relogio ao avaliador (ADR 0013).
 *
 * Separado do AvaliadorDeAlertas para que o teste possa chamar avaliar()
 * sem que um agendador rode em segundo plano no meio dele. Em teste,
 * cripto.alertas.agendamento=false faz esta classe nem existir.
 *
 * fixedDelay, nao fixedRate: conta a partir do FIM da execucao anterior.
 * Se uma avaliacao demorar, a proxima espera - nunca ha duas ao mesmo tempo.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "cripto.alertas.agendamento", havingValue = "true", matchIfMissing = true)
public class AgendamentoDeAlertas {

    private final AvaliadorDeAlertas avaliador;

    public AgendamentoDeAlertas(AvaliadorDeAlertas avaliador) {
        this.avaliador = avaliador;
    }

    @Scheduled(fixedDelayString = "${cripto.alertas.intervalo:PT5M}")
    public void rodar() {
        avaliador.avaliar();
    }
}

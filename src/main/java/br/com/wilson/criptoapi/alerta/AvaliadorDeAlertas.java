package br.com.wilson.criptoapi.alerta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * A unica regra de negocio do projeto (ADR 0013).
 *
 * Uma consulta encontra os alertas cuja condicao vale na ultima coleta;
 * para cada um, grava o disparo e desativa o alerta - na mesma transacao,
 * para nunca existir disparo sem desativacao nem o contrario.
 *
 * Nao sabe nada de agendamento: quem chama e AgendamentoDeAlertas em
 * execucao, e o teste diretamente.
 */
@Service
public class AvaliadorDeAlertas {

    private static final Logger log = LoggerFactory.getLogger(AvaliadorDeAlertas.class);

    private final AlertaRepository alertas;
    private final AlertaDisparoRepository disparos;

    public AvaliadorDeAlertas(AlertaRepository alertas, AlertaDisparoRepository disparos) {
        this.alertas = alertas;
        this.disparos = disparos;
    }

    /** @return quantos alertas dispararam nesta avaliacao */
    @Transactional
    public int avaliar() {
        List<AlertaVencido> vencidos = alertas.encontrarVencidos();
        for (AlertaVencido vencido : vencidos) {
            disparos.save(new AlertaDisparo(vencido.alerta(), vencido.preco(), vencido.coletadoEm()));
            vencido.alerta().desativar(); // entidade gerenciada: o UPDATE sai no commit
        }
        if (!vencidos.isEmpty()) {
            log.info("{} alerta(s) disparado(s)", vencidos.size());
        }
        return vencidos.size();
    }
}

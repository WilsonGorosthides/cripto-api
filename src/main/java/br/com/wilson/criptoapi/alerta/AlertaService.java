package br.com.wilson.criptoapi.alerta;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * CRUD de alertas, sempre no escopo de um usuario. Nenhum metodo aceita um
 * alerta sem o usuarioId junto - e o que impede um usuario de alcancar o
 * alerta de outro por adivinhar o id.
 */
@Service
public class AlertaService {

    private final AlertaRepository alertas;
    private final AlertaDisparoRepository disparos;

    public AlertaService(AlertaRepository alertas, AlertaDisparoRepository disparos) {
        this.alertas = alertas;
        this.disparos = disparos;
    }

    /** Simbolo em maiusculo, como o pipeline grava - e o job compara. */
    @Transactional
    public AlertaResposta criar(Long usuarioId, NovoAlerta novo) {
        Alerta alerta = alertas.save(new Alerta(
                usuarioId,
                novo.simbolo().trim().toUpperCase(Locale.ROOT),
                novo.condicao(),
                novo.valor()));
        return AlertaResposta.de(alerta);
    }

    @Transactional(readOnly = true)
    public List<AlertaResposta> listar(Long usuarioId) {
        return alertas.findByUsuarioIdOrderByCriadoEmDesc(usuarioId)
                .stream()
                .map(AlertaResposta::de)
                .toList();
    }

    @Transactional
    public void apagar(Long usuarioId, Long id) {
        Alerta alerta = alertas.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new AlertaNaoEncontradoException(id));
        alertas.delete(alerta); // ON DELETE CASCADE leva os disparos
    }

    @Transactional(readOnly = true)
    public List<DisparoResposta> disparos(Long usuarioId, Long id) {
        Alerta alerta = alertas.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new AlertaNaoEncontradoException(id));
        return disparos.findByAlertaIdOrderByColetadoEmDesc(alerta.getId())
                .stream()
                .map(DisparoResposta::de)
                .toList();
    }
}

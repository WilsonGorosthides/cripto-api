package br.com.wilson.criptoapi.alerta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertaDisparoRepository extends JpaRepository<AlertaDisparo, Long> {

    /** "AlertaId" e resolvido pelo Spring Data como alerta.id. */
    List<AlertaDisparo> findByAlertaIdOrderByColetadoEmDesc(Long alertaId);
}

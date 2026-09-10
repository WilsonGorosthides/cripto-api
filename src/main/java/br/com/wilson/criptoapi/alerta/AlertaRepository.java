package br.com.wilson.criptoapi.alerta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    List<Alerta> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

    /** Id E dono, numa consulta so: alerta de outro usuario e "nao existe". */
    Optional<Alerta> findByIdAndUsuarioId(Long id, Long usuarioId);

    /**
     * A regra do ADR 0013 numa consulta: alertas ativos cruzados com a ultima
     * coleta de cada moeda (MoedaAtual e a view vw_cripto_atual - ADR 0006),
     * ficando so os cujo preco atual satisfaz a condicao.
     *
     * upper() no simbolo da view porque o alerta e guardado em maiusculo e o
     * pipeline nao promete caixa. Nao ha indice em simbolo (ADR 0009), entao
     * o upper() nao custa nada que ja nao se pagasse.
     */
    @Query("""
            SELECT new br.com.wilson.criptoapi.alerta.AlertaVencido(a, v.preco, v.coletadoEm)
            FROM Alerta a
            JOIN MoedaAtual v ON upper(v.simbolo) = a.simbolo
            WHERE a.ativo = true
              AND ((a.condicao = br.com.wilson.criptoapi.alerta.Condicao.ACIMA  AND v.preco > a.valor)
                OR (a.condicao = br.com.wilson.criptoapi.alerta.Condicao.ABAIXO AND v.preco < a.valor))
            """)
    List<AlertaVencido> encontrarVencidos();
}

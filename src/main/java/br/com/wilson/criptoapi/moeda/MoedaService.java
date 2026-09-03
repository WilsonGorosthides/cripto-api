package br.com.wilson.criptoapi.moeda;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regras de leitura sobre moedas.
 *
 * Existe por dois motivos, um imediato e um de investimento:
 *
 * 1. IMEDIATO - e aqui que a transacao comeca e termina. Sem esta camada, cada
 *    consulta ia ao banco em auto-commit, sem limite transacional em lugar
 *    nenhum. O readOnly=true tambem informa ao Hibernate que ele pode pular a
 *    verificacao de alteracoes, e ao PostgreSQL que a transacao nao escreve.
 *
 * 2. INVESTIMENTO - da um lugar para regra de negocio que nao seja dentro de um
 *    metodo de HTTP. Hoje nao ha regra nenhuma; o metodo abaixo so orquestra.
 *    Vale reconhecer isso em vez de fingir que a camada ja se paga.
 *
 * A traducao para DTO acontece aqui, e nao no controller, para que o controller
 * so trate de HTTP e o servico entregue o contrato ja pronto.
 */
@Service
public class MoedaService {

    private final MoedaAtualRepository repository;

    public MoedaService(MoedaAtualRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MoedaResposta> listarAtuais() {
        return repository.findAllByOrderByRankingAsc()
                .stream()
                .map(MoedaResposta::de)
                .toList();
    }
}

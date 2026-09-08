package br.com.wilson.criptoapi.moeda;

import br.com.wilson.criptoapi.comum.PaginaResposta;
import org.springframework.data.domain.Pageable;
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
 *    metodo de HTTP.
 *
 * A traducao para DTO acontece aqui, e nao no controller, para que o controller
 * so trate de HTTP e o servico entregue o contrato ja pronto.
 */
@Service
public class MoedaService {

    private final MoedaAtualRepository moedaAtualRepository;
    private final PrecoRepository precoRepository;

    public MoedaService(MoedaAtualRepository moedaAtualRepository,
                        PrecoRepository precoRepository) {
        this.moedaAtualRepository = moedaAtualRepository;
        this.precoRepository = precoRepository;
    }

    @Transactional(readOnly = true)
    public List<MoedaResposta> listarAtuais() {
        return moedaAtualRepository.findAllByOrderByRankingAsc()
                .stream()
                .map(MoedaResposta::de)
                .toList();
    }

    /**
     * Serie historica de uma moeda, do mais recente para o mais antigo.
     *
     * ATENCAO - comportamento provisorio: simbolo inexistente devolve pagina
     * vazia, e o controller responde 200. O correto e 404. Isso e tratado no
     * proximo bloco, junto com o formato padronizado de erro.
     */
    @Transactional(readOnly = true)
    public PaginaResposta<PontoHistorico> buscarHistorico(String simbolo, Pageable paginacao) {
        return PaginaResposta.de(
                precoRepository.findBySimboloIgnoreCaseOrderByColetadoEmDesc(simbolo, paginacao),
                PontoHistorico::de
        );
    }
}

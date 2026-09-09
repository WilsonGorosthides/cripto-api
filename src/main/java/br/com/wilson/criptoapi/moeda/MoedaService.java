package br.com.wilson.criptoapi.moeda;

import br.com.wilson.criptoapi.comum.PaginaResposta;
import org.springframework.data.domain.Page;
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
     * Pagina vazia tem dois significados, e o cliente precisa distingui-los
     * (ADR 0015): moeda que nunca foi coletada e 404; pagina alem do fim de
     * uma moeda real e 200 vazio. A verificacao de existencia so roda no caso
     * raro - pagina vazia - para nao custar uma consulta a mais no caminho comum.
     */
    @Transactional(readOnly = true)
    public PaginaResposta<PontoHistorico> buscarHistorico(String simbolo, Pageable paginacao) {
        Page<Preco> pagina =
                precoRepository.findBySimboloIgnoreCaseOrderByColetadoEmDesc(simbolo, paginacao);

        if (pagina.isEmpty() && !precoRepository.existsBySimboloIgnoreCase(simbolo)) {
            throw new MoedaNaoEncontradaException(simbolo);
        }
        return PaginaResposta.de(pagina, PontoHistorico::de);
    }
}

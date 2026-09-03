# 0004 — Separar o contrato HTTP da entidade com um DTO

**Status:** aceita
**Data:** 2026-09-03

## Contexto

A primeira versão do `MoedaController` devolvia a entidade `MoedaAtual` diretamente. O
JSON resultante expunha dois problemas.

O primeiro, visível: o campo `id`, que é a chave primária da **linha** na tabela — não da
moeda. Como o modelo é snapshot append-only, o Bitcoin recebe um `id` novo a cada coleta
horária. Um cliente que o guardasse como identificador da moeda quebraria em 60 minutos.

O segundo, estrutural: uma única classe cumpria dois papéis com donos diferentes — dizer ao
Hibernate como ler a tabela, e definir o JSON que terceiros consomem. Sob esse arranjo,
renomear uma coluna no PostgreSQL renomeia um campo do contrato público, sem que nada no
código sinalize que aquilo era público.

## Decisão

Um `record` `MoedaResposta` define o contrato. A conversão vive num método estático
`de(MoedaAtual)` dentro do próprio record, para que a dependência aponte numa direção só:
o contrato conhece a entidade; a entidade não conhece o contrato.

O `id` ficou de fora. `moedaId` — o identificador estável da CoinGecko — ficou dentro, e é
ele que o cliente deve usar para referenciar uma moeda.

## Alternativas consideradas

### Devolver a entidade com `@JsonIgnore` no `id`

**Vantagem:** uma anotação resolve o problema visível. Zero classes novas, zero conversão.

**Desvantagem:** trata o sintoma. A classe continuaria com dois donos, e o acoplamento
entre schema e contrato permaneceria intacto — o dia do refactor de banco continuaria
sendo o dia de quebrar clientes.

### MapStruct

**Vantagem:** gera o código de conversão em tempo de compilação a partir de uma interface
anotada. Com dezenas de DTOs, elimina muito código repetitivo, e erros de mapeamento
aparecem no build.

**Desvantagem:** adiciona um processador de anotações ao build para economizar onze linhas.
Passa a valer quando o número de DTOs justificar; hoje, não justifica.

### ModelMapper ou outro mapeador por reflexão

**Vantagem:** conversão automática sem escrever nada, baseada em coincidência de nomes.

**Desvantagem:** um campo renomeado deixa de ser copiado **em silêncio**, e o defeito
aparece como `null` no JSON, em produção. O método escrito à mão quebra a compilação no
mesmo instante. Descartada com mais convicção que as outras.

## Consequências

**Mais fácil:** o contrato tem um lugar só, e a assinatura do método do controller
documenta o que sai. Mudanças de schema não vazam para os clientes.

**Mais fácil:** a ordem dos campos no JSON passou a ser a de declaração do record, e não
alfabética como acontecia com a entidade. Foi possível ordenar por leitura humana — quem é
a moeda, quanto vale, os agregados, o carimbo de tempo.

**Mais difícil:** toda coluna nova que se queira expor exige alteração em dois lugares.

**Mais difícil:** a conversão é código manual, e código manual pode esquecer um campo. O
compilador ajuda — o construtor canônico do record exige todos os argumentos — mas não
impede trocar dois campos de mesmo tipo entre si.

## Nota técnica

`record` não pode ser entidade JPA: o Hibernate exige construtor sem argumentos e campos
alteráveis, porque instancia o objeto vazio e o preenche depois. Records são imutáveis por
definição. Essa incompatibilidade é conveniente — ela força a separação em vez de deixá-la
a critério da disciplina.

## Referências

- Martin Fowler, *Data Transfer Object*, em *Patterns of Enterprise Application
  Architecture* — <https://martinfowler.com/eaaCatalog/dataTransferObject.html>
- JEP 395: Records — <https://openjdk.org/jeps/395>

# 0009 — Adiar a criação de índices para os padrões de acesso da API

**Status:** aceita
**Data:** 2026-09-08

## Contexto

Ao implementar o endpoint de histórico, os planos de execução foram medidos no banco real
(1.120 linhas, 20 moedas, 56 coletas). Duas consultas da API **não são servidas pelos índices
existentes**.

### Consulta 1 — histórico por símbolo

```sql
SELECT ... FROM precos_cripto WHERE simbolo = 'BTC' ORDER BY coletado_em DESC LIMIT 10
```

```
Seq Scan on precos_cripto
  Filter: (simbolo = 'BTC')
  Rows Removed by Filter: 1045
```

O índice `idx_cripto_moeda_data` é sobre `(moeda_id, coletado_em DESC)` — indexado por
`moeda_id` (`"bitcoin"`), não por `simbolo` (`"BTC"`). A mesma consulta por `moeda_id` usa
`Index Scan`, lê 10 linhas e não ordena nada.

### Consulta 2 — a listagem, que já está no ar

```sql
SELECT ... FROM vw_cripto_atual
```

```
Unique
  → Sort  (rows=1120)                                    ← ordena a tabela inteira
      Sort Key: moeda_id, moeda_fiat, coletado_em DESC
      → Seq Scan (rows=1120)
```

O `DISTINCT ON` da view precisa das linhas ordenadas por
`(moeda_id, moeda_fiat, coletado_em DESC)`. O índice `uq_moeda_coleta` cobre as mesmas
colunas, mas com `coletado_em` **crescente** — a ordem não corresponde, e o planner ordena à
mão. Custo medido: **4,87 ms**, contra 0,68 ms da consulta de histórico.

Esta é a consulta mais cara da API, e é a do endpoint principal.

### A restrição

O [ADR 0002](0002-api-nao-e-dona-do-schema.md) estabelece que a API não altera o schema de
`precos_cripto`. A correção, portanto, não pode partir daqui.

## Decisão

**Não criar índices agora.** Registrar a medição, o limiar de reavaliação e a correção
pronta, para que a decisão possa ser executada sem repetir a análise.

A alternativa de contornar mudando o contrato da API — usar `moeda_id` no caminho em vez de
`simbolo` — também foi descartada.

## Alternativas consideradas

### Criar os índices no `cripto-pipeline`

```sql
CREATE INDEX idx_cripto_simbolo_data ON precos_cripto (simbolo, coletado_em DESC);
CREATE INDEX idx_cripto_ultima ON precos_cripto (moeda_id, moeda_fiat, coletado_em DESC);
```

**Vantagem:** resolve as duas consultas na raiz, respeitando a fronteira de propriedade — a
mudança acontece no repositório dono do schema. O primeiro índice troca `Seq Scan` por
`Index Scan`; o segundo elimina a ordenação completa da tabela a cada listagem.

**Desvantagem:** cada índice tem custo de escrita e de espaço. O pipeline insere 20 linhas por
hora; dois índices adicionais tornam cada inserção um pouco mais cara e o banco maior. Mais
relevante: exige mexer num repositório considerado concluído, o que reabre um projeto que já
tinha sido entregue.

### Mudar o contrato da API para usar `moeda_id`

`/api/v1/moedas/bitcoin/historico` em vez de `/api/v1/moedas/BTC/historico`.

**Vantagem:** usa o índice existente sem tocar em nada, e `moeda_id` é de fato o
identificador estável — `simbolo` pode colidir entre moedas diferentes.

**Desvantagem:** `BTC` é o identificador que as pessoas conhecem e que aparece em qualquer
interface. E não resolve nada da consulta 2, que é a mais cara.

### Resolver `simbolo → moeda_id` antes da consulta principal

Buscar o `moeda_id` na view e depois consultar o histórico pelo campo indexado.

**Vantagem:** manteria o caminho amigável e usaria o índice na consulta que cresce.

**Desvantagem:** **medida e descartada por número.** A resolução pela view custa 4,87 ms —
mais caro que o `Seq Scan` de 0,68 ms que se queria evitar. A view não tem índice que a
sirva, então consultá-la ordena a tabela inteira. A otimização custaria sete vezes mais que
o problema.

## Consequências

**Mais fácil:** nada muda. O `cripto-pipeline` permanece concluído, e a API não carrega
complexidade que não precisa.

**Mais fácil:** a decisão está registrada com os números, não com impressões. Quando for hora
de executar, não é preciso refazer a análise — o SQL está acima.

**Mais difícil:** as duas consultas degradam linearmente com o histórico. Hoje 4,87 ms com
1.120 linhas; a projeção linear dá ~760 ms com 175 mil linhas, ao fim de um ano de coleta
contínua. Índice não é otimização prematura quando a medição já existe — é dívida assumida.

## Quando revisitar

Executar a alternativa 1 quando **qualquer** destes for verdade:

- `precos_cripto` ultrapassar **50 mil linhas**;
- `GET /api/v1/moedas` passar de **100 ms**;
- a API receber tráfego de mais de um cliente simultâneo.

Verificação, a qualquer momento:

```sql
EXPLAIN (ANALYZE) SELECT * FROM vw_cripto_atual;
-- procurar por: Sort (rows=N) com N proximo ao total da tabela
```

## Nota honesta

A regra "não otimize sem problema medido" foi respeitada pela metade. **A medição existe** —
o que ainda não existe é a *dor*. A diferença entre adiar e ignorar é este registro: o
problema tem número, limiar e correção escrita. Sem isso, seria omissão.

## Referências

- Índices no PostgreSQL — <https://www.postgresql.org/docs/17/indexes.html>
- Ordenação em índices multicoluna —
  <https://www.postgresql.org/docs/17/indexes-ordering.html>
- Relacionado: [ADR 0002](0002-api-nao-e-dona-do-schema.md),
  [ADR 0006](0006-consulta-na-view-e-nao-em-java.md),
  [ADR 0008](0008-paginacao-por-offset.md)

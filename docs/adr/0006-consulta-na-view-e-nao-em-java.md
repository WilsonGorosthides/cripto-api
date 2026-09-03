# 0006 — Resolver "última coleta" no banco, através da view

**Status:** aceita
**Data:** 2026-09-03

## Contexto

A tabela `precos_cripto` é append-only: cada coleta horária acrescenta uma linha por moeda,
e nada é sobrescrito. No momento desta decisão eram 560 linhas de 28 coletas, crescendo
cerca de 480 linhas por dia. Em um mês, aproximadamente 14 mil.

O endpoint de listagem precisa da **última linha de cada moeda** — 20 linhas de um conjunto
que só cresce. O `cripto-pipeline` já expõe a view `vw_cripto_atual`, que resolve isso com
`DISTINCT ON (moeda_id, moeda_fiat) ... ORDER BY moeda_id, moeda_fiat, coletado_em DESC`, e
existe o índice composto `idx_cripto_moeda_data (moeda_id, coletado_em DESC)`.

## Decisão

A entidade `MoedaAtual` mapeia a **view**, não a tabela. O filtro acontece no PostgreSQL,
apoiado no índice existente. A API recebe 20 linhas prontas.

## Alternativas consideradas

### Trazer tudo e filtrar em Java (stream, `Collectors.groupingBy`)

**Vantagem:** nenhuma dependência de recurso específico do banco. O código seria portável
para qualquer SGBD, e a lógica ficaria visível em Java, testável sem banco.

**Desvantagem:** transfere a tabela inteira pela rede e a materializa na memória da JVM a
cada requisição, para descartar 99% dela. O custo cresce linearmente com o histórico, que é
justamente o que o projeto foi feito para acumular. Em um ano, seriam ~175 mil linhas por
chamada.

### Consulta com `@Query` e `ROW_NUMBER()` na própria entidade da tabela

**Vantagem:** portável entre bancos — `ROW_NUMBER() OVER (PARTITION BY ...)` é SQL padrão,
ao contrário do `DISTINCT ON`, que é exclusivo do PostgreSQL. Mantém a lógica visível no
código Java, sem depender de um objeto de banco definido em outro repositório.

**Desvantagem:** duplica no Java uma regra que já existe no `schema.sql`. Duas definições
da mesma coisa divergem com o tempo — é o problema que o ADR 0002 procura evitar. E o
`DISTINCT ON` costuma usar o índice de forma mais direta.

Esta é a alternativa que voltaria à mesa se o projeto precisasse rodar em outro SGBD.

### Tabela materializada de "estado atual", atualizada pelo pipeline

**Vantagem:** leitura ainda mais barata, sem nenhum processamento na consulta.

**Desvantagem:** cria uma segunda fonte de verdade que pode divergir da tabela de fatos, e
transfere para o pipeline a responsabilidade de mantê-la em sincronia. Otimização sem
problema medido que a justifique.

## Consequências

**Mais fácil:** o tempo de resposta da listagem não cresce com o histórico. O trabalho é
feito onde o índice está.

**Mais fácil:** a definição de "coleta mais recente" existe num lugar só — o `schema.sql`
do pipeline. Mudá-la muda o comportamento das duas pontas de uma vez.

**Mais difícil:** o projeto passa a depender de um objeto de banco definido em outro
repositório. Se a view for removida ou alterada lá, a API quebra em tempo de execução.

**Mais difícil:** o `DISTINCT ON` é específico do PostgreSQL. Migrar de SGBD exigiria
reescrever a view — não a aplicação, mas ainda assim trabalho.

## Referências

- `DISTINCT ON` no PostgreSQL —
  <https://www.postgresql.org/docs/17/sql-select.html#SQL-DISTINCT>
- Definição da view: `schema.sql` em
  <https://github.com/WilsonGorosthides/cripto-pipeline>
- Relacionado: [ADR 0002](0002-api-nao-e-dona-do-schema.md)

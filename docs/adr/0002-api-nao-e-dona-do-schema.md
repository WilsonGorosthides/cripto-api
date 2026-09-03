# 0002 — A API não é dona do schema de `precos_cripto`

**Status:** aceita
**Data:** 2026-09-03

## Contexto

A tabela `precos_cripto` é criada e mantida pelo `cripto-pipeline`, através de um
`CREATE TABLE IF NOT EXISTS` executado a cada coleta. No momento desta decisão ela
acumulava 560 linhas de série histórica real, insubstituíveis: representam 28 coletas
horárias que não podem ser recriadas.

O Hibernate, por padrão de configuração em quase todo tutorial, recebe autoridade para
ajustar o schema ao que as classes Java declaram (`ddl-auto=update`). Duas ferramentas
com autoridade sobre a mesma tabela é uma configuração instável.

## Decisão

A API é consumidora, não dona. Isso está declarado em dois lugares independentes:

- `spring.jpa.hibernate.ddl-auto=none` — o Hibernate não emite nenhum DDL ao subir;
- `@Immutable` na entidade `MoedaAtual` — o Hibernate não gera `INSERT`, `UPDATE` nem
  `DELETE` para ela, e pula a verificação de alterações.

As tabelas que a própria API criar no futuro (`usuarios`, `alertas`) serão versionadas por
Flyway, sob propriedade exclusiva dela. Uma fonte de verdade por tabela.

## Alternativas consideradas

### `ddl-auto=update`

**Vantagem:** o schema acompanha as classes Java automaticamente. Menos trabalho manual
ao evoluir o modelo, e é o caminho de menor atrito ao começar.

**Desvantagem:** dá ao Hibernate autoridade unilateral. Um tipo declarado com pequena
diferença — `Double` onde o banco tem `NUMERIC(24,8)` — faria o Hibernate emitir um
`ALTER TABLE` para "corrigir" a coluna. O `INSERT` do pipeline quebraria na hora seguinte,
e a depuração aconteceria no Python, procurando um defeito originado no Java. O modo
falha em silêncio, que é pior do que falhar alto.

### `ddl-auto=validate`

**Vantagem:** não altera nada e ainda detecta divergência entre classe e tabela,
derrubando a aplicação na inicialização em vez de produzir erro em tempo de execução.

**Desvantagem:** exige que a entidade descreva a tabela *inteira*, incluindo colunas que a
API não usa. Qualquer coluna nova adicionada pelo pipeline passaria a impedir a API de
subir — um acoplamento mais forte do que o necessário para uma leitora.

Continua sendo o próximo passo natural se a divergência silenciosa virar um problema real.

### Banco separado, com replicação

**Vantagem:** isolamento completo. Nenhuma disputa possível pelo schema.

**Desvantagem:** infraestrutura de replicação, latência entre escrita e leitura, e uma
segunda instância de PostgreSQL para manter. Custo desproporcional a um projeto com duas
peças.

## Consequências

**Mais fácil:** o pipeline evolui sem coordenação com a API. Uma coluna nova não quebra
nada aqui.

**Mais fácil:** nenhum caminho de código da API pode destruir dado histórico. A garantia é
estrutural, não disciplinar.

**Mais difícil:** se o pipeline renomear ou remover uma coluna que a entidade mapeia, a API
falha em tempo de execução, não na inicialização. É o preço de recusar `validate`.

**Mais difícil:** a entidade precisa ser atualizada à mão quando quiser expor uma coluna
nova.

## Referências

- `ddl-auto` e os valores possíveis:
  <https://docs.spring.io/spring-boot/reference/data/sql.html>
- Relacionado: [ADR 0001](0001-dois-repositorios-separados.md)

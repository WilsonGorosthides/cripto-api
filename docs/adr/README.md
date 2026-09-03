# Registros de Decisão de Arquitetura

Cada arquivo aqui documenta **uma decisão** tomada neste projeto: o contexto que a exigiu,
o que foi decidido, o que foi descartado e o que se perdeu ao decidir assim.

O formato é o ADR (*Architecture Decision Record*), proposto por Michael Nygard em 2011.

## Índice

| # | decisão | status |
|---|---|---|
| [0001](0001-dois-repositorios-separados.md) | Dois repositórios separados, não um monorepo | aceita |
| [0002](0002-api-nao-e-dona-do-schema.md) | A API não é dona do schema de `precos_cripto` | aceita |
| [0003](0003-bigdecimal-para-valor-monetario.md) | `BigDecimal` para valor monetário, nunca `double` | aceita |
| [0004](0004-dto-separado-da-entidade.md) | DTO separando contrato HTTP da entidade | aceita |
| [0005](0005-segredo-por-variavel-de-ambiente.md) | Segredo por variável de ambiente, não em arquivo | aceita |
| [0006](0006-consulta-na-view-e-nao-em-java.md) | Filtrar a última coleta na view, não em Java | aceita |
| [0007](0007-maven-em-vez-de-gradle.md) | Maven em vez de Gradle | aceita |

## Sobre a origem destes registros

Os ADRs **0001 a 0007 foram escritos retroativamente**, em 2026-09-03, para estabelecer a
linha de base. As decisões que eles descrevem foram tomadas entre 01 e 03 de setembro de
2026, durante a construção inicial do projeto; a data em cada arquivo é a do registro, não
a da decisão.

A partir do **0008**, cada registro é escrito no momento em que a decisão é tomada, antes
da implementação avançar.

Escrever os primeiros em lote é o modo usual de adotar a prática no meio de um projeto.
Datá-los como se tivessem sido escritos ao longo do caminho seria mais bonito e menos
verdadeiro — e o `git log` desmentiria em dez segundos.

## Regras deste diretório

**ADR não se edita.** Se uma decisão for revista, escreve-se um novo ADR e marca-se o
anterior como `substituída por NNNN`. O valor do registro está em provar o que se pensava
naquele momento — reescrever apaga exatamente isso.

**Nem toda escolha vira ADR.** O critério é decisão *arquiteturalmente significativa*: a
que restringe trabalho futuro, ou a que alguém plausivelmente contestaria. "Usei Spring
Boot" não é decisão, é o óbvio do contexto. Uma coleção inflada de ADRs comunica menos
que uma pequena e afiada.

## Modelo

```markdown
# NNNN — Título no imperativo

**Status:** aceita | substituída por NNNN | revogada
**Data:** AAAA-MM-DD

## Contexto
O que na situação obrigou a decidir. Fatos, não opiniões.

## Decisão
O que foi decidido, na voz ativa.

## Alternativas consideradas
Cada uma com vantagem e desvantagem reais. Se uma alternativa não tem vantagem
nenhuma, ela não era alternativa — era espantalho.

## Consequências
O que passou a ser mais fácil, e o que passou a ser mais difícil. Sempre há as duas.

## Referências
```

## Referência do método

- Michael Nygard, *Documenting Architecture Decisions* (2011) —
  <https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions>
- Coletânea de modelos e ferramentas — <https://adr.github.io/>

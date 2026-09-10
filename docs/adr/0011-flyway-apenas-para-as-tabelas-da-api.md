# 0011 — Flyway gerencia apenas as tabelas da API

**Status:** aceita
**Data:** 2026-09-09

## Contexto

A API vai passar a **escrever**: usuários, alertas e o registro de cada disparo. São três
tabelas novas, e alguém precisa criá-las — em desenvolvimento, no container de teste e em
qualquer banco onde a aplicação suba.

Até aqui a API só lia, e o [ADR 0002](0002-api-nao-e-dona-do-schema.md) resolvia tudo com
`ddl-auto=none`: o Hibernate não toca em nada, o pipeline Python cria o que é dele. Isso
continua valendo para `precos_cripto`. Mas `none` também proíbe o Hibernate de criar as
tabelas novas, e o único DDL que a API tem hoje é o espelho de teste do
[ADR 0010](0010-espelho-do-schema-do-pipeline-em-teste.md), que por definição não pode
conter nada além do que o pipeline possui.

O banco de desenvolvimento **já tem** `precos_cripto` no schema `public`. Qualquer
ferramenta de migração vai encontrar um schema não vazio sem histórico próprio, e precisa
saber o que fazer com isso.

## Decisão

Adotar o **Flyway**, rodando no boot da aplicação, com migrations SQL versionadas em
`src/main/resources/db/migration/`. Ele cria e evolui **somente** `usuarios`, `alertas` e
`alerta_disparos`.

Regras que fazem parte da decisão:

1. **Nenhuma migration menciona `precos_cripto` nem `vw_cripto_atual`.** Nem `ALTER`, nem
   `CREATE INDEX`, nem `COMMENT`. A fronteira do ADR 0002 não se mexe; o ADR 0009 já
   registrou que os índices que a API gostaria de ter são criados no repositório do pipeline.
2. **Sem chave estrangeira** de `alertas` para `precos_cripto`. Um FK faria o schema da API
   depender fisicamente de uma tabela que ela não controla — um `DROP` no pipeline
   quebraria a API no banco, não no código. A ligação é lógica, por `simbolo`.
3. `spring.flyway.baseline-on-migrate=true` com `spring.flyway.baseline-version=0`. Sem o
   primeiro, o Flyway recusa migrar um schema não vazio; com o primeiro e **sem o segundo**,
   ele marca o banco como já estando na versão 1 e **pula a `V1__`** — a primeira migration
   nunca roda, silenciosamente. Este é o erro mais comum ao adotar Flyway sobre banco
   existente, e a configuração está aqui para não ser descoberta em produção.
4. `ddl-auto` permanece `none` em execução e `validate` em teste. O Flyway cria; o Hibernate
   confere; ninguém mais escreve DDL.

## Alternativas consideradas

### Hibernate criar as tabelas novas (`ddl-auto=update`)

**Vantagem:** zero ferramenta nova. Anota a entidade, sobe, a tabela aparece.

**Desvantagem:** `update` não distingue tabela da API de tabela do pipeline. Ele compararia
`MoedaAtual` e `Preco` com o banco e "corrigiria" o que achasse diferente — o cenário exato
que o ADR 0002 proíbe. Além disso, `update` só adiciona; nunca remove coluna nem cria
constraint corretamente, e não deixa histórico do que foi aplicado. Serve para protótipo,
não para banco que outra aplicação também usa.

### Liquibase

**Vantagem:** changelogs em XML/YAML são independentes de dialeto e permitem *rollback*
declarado por mudança — o Flyway gratuito não faz `undo`.

**Desvantagem:** o projeto tem um único banco, PostgreSQL, e não vai trocar. A abstração de
dialeto custa uma sintaxe própria para descrever DDL que já se sabe escrever em SQL. O
Flyway lê `.sql` puro: o que está na migration é exatamente o que o banco executa, sem
tradução para conferir.

### Migrations num schema separado (`api`), não em `public`

`spring.flyway.schemas=api`, com `@Table(schema = "api")` nas entidades da API.

**Vantagem:** isolamento físico. O schema `api` nasce vazio, o problema do *baseline*
desaparece, e um `\dt api.*` mostra exatamente o que a API possui. É a forma mais forte de
impor a fronteira do ADR 0002.

**Desvantagem:** cada entidade, cada consulta nativa e cada `search_path` precisa saber do
schema. O job de alertas cruza as duas fronteiras numa consulta só, e passaria a fazê-lo
com qualificação explícita. É a alternativa mais correta e a mais cara; foi descartada
pelo custo, não pelo mérito. Se o projeto crescesse, seria a primeira a revisitar.

### Migrar fora da aplicação (CLI do Flyway no deploy)

**Vantagem:** a aplicação sobe sem privilégio de DDL — princípio do menor privilégio no
banco.

**Desvantagem:** exige um passo de deploy separado, com credencial própria, num projeto
cujo deploy é `docker compose up`. A separação faz sentido quando há pipeline de entrega
com etapas; aqui, criaria a etapa só para justificar a separação.

## Consequências

**Mais fácil:** o container de teste e o banco de desenvolvimento ganham as mesmas tabelas
pelo mesmo caminho. O teste sobe, o Flyway migra, o Hibernate valida — se uma entidade
divergir da migration, o build quebra antes de qualquer teste rodar.

**Mais fácil:** a tabela `flyway_schema_history` registra o que foi aplicado e quando. Isso
responde "este banco está em que versão?" sem adivinhar.

**Mais difícil:** o schema `public` passa a ter tabelas de dois donos lado a lado, e a
única coisa que separa uma da outra é convenção: o nome do arquivo de migration e a regra 1
acima. Nada no banco impede uma migration futura de tocar `precos_cripto`. É a mesma
fragilidade que o ADR 0010 assume — controle humano, e controle humano falha.

**Mais difícil:** migration aplicada não se edita. Um erro numa `V1__` que já rodou em
algum banco exige uma `V2__` que o corrija. O Flyway confere o *checksum* de cada arquivo
aplicado e recusa subir se ele mudou — proteção deliberada, que no começo parece obstáculo.

## Referências

- Flyway, conceitos de migration e baseline —
  <https://documentation.red-gate.com/flyway/flyway-concepts/migrations>
- `baselineOnMigrate` e `baselineVersion` —
  <https://documentation.red-gate.com/flyway/reference/configuration/flyway-namespace/flyway-baseline-on-migrate-setting>
- Integração com Spring Boot —
  <https://docs.spring.io/spring-boot/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway>
- Relacionado: [ADR 0002](0002-api-nao-e-dona-do-schema.md),
  [ADR 0009](0009-adiar-indices-para-os-acessos-da-api.md),
  [ADR 0010](0010-espelho-do-schema-do-pipeline-em-teste.md)

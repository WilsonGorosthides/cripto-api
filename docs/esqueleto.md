# Esqueleto — o que levar deste projeto para o próximo

Este documento existe para um único fim: começar o próximo projeto Spring **sem
redescobrir** o que custou horas aqui. Não é tutorial; é lista do que copiar, do que trocar
e do que não repetir.

A forma escolhida é **documento + arquivos neste repositório**, não um repositório-modelo
separado. Alternativa descartada: um repo `esqueleto-spring` com o código genérico. Vantagem
real — `gh repo create --template` em um comando. Desvantagem que decidiu — o modelo
envelhece sozinho, longe do projeto que o validou, e ninguém atualiza modelo. Aqui, o
esqueleto é o projeto que passou em 45 testes hoje. Se um dia valer a pena, marcar este
repositório como *Template repository* nas configurações do GitHub dá o mesmo comando sem
criar nada.

## Copiar como está

| arquivo | papel | o que muda |
|---|---|---|
| `mvnw`, `mvnw.cmd`, `.mvn/` | Maven sem instalar Maven | nada. Conferir `100755` no índice (`git ls-files -s mvnw`) |
| `.gitattributes` | `mvnw` sempre LF, `.cmd` sempre CRLF | nada |
| `.gitignore` | bloco de segredos: `.env`, `*.env`, `application-*-local.properties` | nada |
| `.dockerignore` | `target/`, `.git/`, docs, segredos fora do contexto de build | nada |
| `Dockerfile` | dois estágios, JRE alpine, usuário sem privilégio, `MaxRAMPercentage` | o nome do jar em `cp target/...-*.jar` |
| `.github/workflows/ci.yml` | suíte a cada push/PR; imagem no GHCR em `main` | o nome da imagem; remover o passo do espelho se não houver schema externo |
| `docs/adr/README.md` | índice, regras e modelo de ADR | zerar o índice; manter regras e modelo |
| `src/test/java/.../infra/ContainerDeTeste.java` | PostgreSQL real por `@Bean @ServiceConnection` | remover `withInitScript` se não houver schema externo |
| `src/test/java/.../infra/DockerDisponivelTest.java` | prova que Docker responde a partir do build | nada |
| `src/test/resources/application.properties` | perfil de teste: `ddl-auto=validate`, segredo fixo, agendamento desligado | os nomes das propriedades do domínio |
| `src/main/java/.../comum/TratadorDeErros.java` | `problem+json` para tudo, `erros` na validação | nada |
| `src/main/java/.../comum/PaginaResposta.java` | envelope de página estável | nada |
| `src/main/java/.../seguranca/` | `SegurancaConfig`, `PropriedadesJwt`, `TokenService` | as rotas públicas em `authorizeHttpRequests` |
| `src/main/java/.../usuario/` | registro, login, `/eu`, BCrypt, exceções | quase nada; é o mesmo em qualquer API com usuário |

## Trocar sempre

| onde | o quê |
|---|---|
| `pom.xml` | `groupId`, `artifactId`, `name`; a lista de starters |
| pacote raiz | `br.com.wilson.criptoapi` → o do projeto; os pacotes de domínio (`moeda/`, `alerta/`) somem |
| `application.properties` | `spring.application.name`, URL do banco, propriedades `cripto.*` |
| `docker-compose.yml` | nome do serviço, porta publicada do banco, os `initdb.d/` |
| `.env.exemplo` | as variáveis que o compose exige |
| `V1__*.sql` | o schema do novo domínio. **Sem** `baseline-on-migrate` se o banco nasce vazio |

## Não levar — é deste projeto

- `src/test/resources/schema-pipeline.sql` e `docker/dados-exemplo.sql` — espelho e dados de
  um banco que outro sistema alimenta. Só faz sentido quando há um dono externo do schema.
- `moeda/` e `alerta/` inteiros.
- ADRs 0001–0015. As **decisões** se repetem (BigDecimal, DTO separado, segredo por
  ambiente, ProblemDetail); os **registros** não — cada projeto escreve os seus, com o seu
  contexto.

## Ordem que funcionou

1. `start.spring.io` → conferir a versão do parent no **Maven Central**, não no metadado do
   Initializr (`4.1.1`, não `4.1.1.RELEASE`).
2. `ContainerDeTeste` + `application.properties` de teste + `CriptoApiApplicationTests`
   rodando verde **antes de qualquer código de domínio**. É o que impede o `contextLoads`
   de ficar quebrado por uma semana sem ninguém ver.
3. `docs/adr/` com o índice vazio. A partir daqui, decisão vira ADR antes de virar código.
4. Flyway `V1` + `MigracoesTest`.
5. Security + JWT + `AutenticacaoTest`.
6. Domínio, um pacote por assunto, teste vermelho antes.
7. `TratadorDeErros` + `TratamentoDeErroTest`.
8. `Dockerfile` → `docker compose up --wait` → `ci.yml`. Verificar cada um rodando, não lendo.
9. README que só afirma o que o passo 8 provou.

## Armadilhas que já custaram tempo — não pagar de novo

| armadilha | sintoma | correção |
|---|---|---|
| `spring-boot-starter-parent` `4.1.1.RELEASE` | `Non-resolvable parent POM` | `4.1.1` — checar `maven-metadata.xml` no Central |
| `spring-boot:run` mata o Maven, não a JVM filha | `Port 8080 was already in use` | `netstat -ano \| findstr :8080` + `Stop-Process` |
| Testcontainers 2.x | `postgresql:2.0.5` dá 404; `PostgreSQLContainer<?>` não compila | módulos `testcontainers-postgresql`, `-junit-jupiter`; classe sem generic |
| `application.properties` de teste **substitui** o de produção | propriedade "sumiu" só em teste | tudo o que o teste precisa vai no arquivo de teste |
| Flyway sobre banco não vazio | recusa migrar; ou, com `baseline-on-migrate` só, **pula a V1** | `baseline-on-migrate=true` **e** `baseline-version=0` |
| `NimbusJwtEncoder` com segredo HMAC | falha ao assinar procurando chave RSA | `JwsHeader.with(MacAlgorithm.HS256)` explícito |
| `UserDetailsService` padrão gera senha no log | ruído + risco | some sozinho quando existe um `JwtDecoder` bean |
| `/actuator/health/liveness` 404 fora do Kubernetes | healthcheck do compose falha | `management.endpoint.health.probes.enabled=true` |
| `mvnw` `100644` no índice | `permission denied` no Linux/CI | `git update-index --chmod=+x mvnw`; `chmod +x` também no Dockerfile |
| `docker.exe` por caminho absoluto no Git Bash | `docker-credential-desktop not found` | pôr `resources/bin` do Docker no `PATH` |
| `@Scheduled` ligado em teste | resultado muda entre execuções | `@ConditionalOnProperty` na config do agendamento; `false` no perfil de teste |
| `@ResponseStatus` em exceção | corpo de erro do `/error`, não `problem+json` | estender `ErrorResponseException` |
| `Page` do Spring serializado direto | contrato muda com a versão do framework | `PaginaResposta<T>` próprio |
| JSON impresso por script intermediário | `399034.0` onde o banco tem `399034.00000000` | mostrar o corpo cru quando o que se prova é precisão |

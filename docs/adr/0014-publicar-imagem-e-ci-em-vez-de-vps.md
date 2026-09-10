# 0014 — Publicar imagem no GHCR com CI, em vez de deploy em VPS

**Status:** aceita
**Data:** 2026-09-09

## Contexto

O escopo original fechava o ciclo com "deploy público em HTTPS numa VPS, com Caddy
terminando TLS". Duas coisas mudaram entre escrever isso e chegar aqui:

1. **O projeto vai ser congelado** ao fim deste ciclo. O domínio cripto deixou de me
   interessar e o próximo projeto já está definido. Não haverá manutenção.
2. **O banco é local.** A API lê o PostgreSQL que o pipeline Python alimenta na minha
   máquina. Uma API na VPS precisaria ou de um banco gerenciado (custo e migração do pipeline),
   ou de um túnel para a máquina local (frágil), ou de um segundo banco vazio (API pública
   sem dado).

O que se quer provar com "publicar" é: *a aplicação sai da minha máquina e roda em
qualquer outra, do mesmo jeito*. Uma URL clicável é **uma** prova disso. Não é a única.

## Decisão

Substituir o item "VPS + Caddy + HTTPS" por três entregas que não apodrecem nem custam:

| entrega | o que prova |
|---|---|
| **Dockerfile multi-stage** | a aplicação é construída e executada sem JDK, Maven ou código-fonte na imagem final |
| **`docker-compose.yml`** com API + PostgreSQL + schema do pipeline aplicado | `docker compose up` numa máquina limpa sobe o sistema inteiro, com a view que a API lê |
| **GitHub Actions** rodando a suíte a cada push e publicando a imagem no **GHCR** a cada merge em `main` | a suíte passa fora da minha máquina; a imagem existe num registro público, com `docker run` a um comando de distância |

O CI também passa a executar a **mitigação 2 do [ADR 0010](0010-espelho-do-schema-do-pipeline-em-teste.md)**:
baixar o `schema.sql` do `cripto-pipeline` e falhar se o espelho divergir.

O `docker-compose.yml` aplica o espelho do schema do pipeline ao PostgreSQL na primeira
subida (`/docker-entrypoint-initdb.d/`) e, opcionalmente, um arquivo de dados reais
exportado do meu banco, datado e identificado como exportação — não são números
inventados.

## Alternativas consideradas

### Manter a VPS com Caddy, como no escopo original

**Vantagem:** URL pública em HTTPS é o que um recrutador clica. Caddy resolve TLS com duas
linhas de configuração, e a experiência de operar uma VPS de verdade tem valor próprio.

**Desvantagem que decidiu:** projeto congelado com VPS ativa é **link que vai morrer** — a
assinatura vence, o certificado expira, e o recrutador que clicar vê erro. Um link morto
deprecia o repositório inteiro, inclusive o que funciona. Some-se o custo mensal
recorrente para algo que não será tocado e a decisão sobre onde fica o banco, que esta
alternativa obriga a tomar e as outras não.

### PaaS com camada gratuita (Render, Fly.io, Koyeb)

**Vantagem:** URL pública sem custo, em menos de uma hora.

**Desvantagem:** camada gratuita muda de regra sem aviso — Heroku encerrou a sua em 2022,
Railway em 2023. Instâncias gratuitas hibernam e a primeira requisição leva dezenas de
segundos. E o problema do banco continua: ou se paga um gerenciado, ou a API pública lê um
banco vazio. Continua sendo opção de uma hora **se** um dia fizer falta; não é item de
escopo.

### Kubernetes local (kind) com manifests versionados

**Vantagem:** era o plano original, e Kubernetes está no título das vagas que motivaram
o projeto.

**Desvantagem:** eu o retirei do escopo por decisão estratégica — orçamento de tempo
finito, e o ciclo a fechar era outro. Manifests para um cluster que só existe na minha
máquina provam menos que um compose que qualquer um sobe. Kubernetes entra num projeto
que tenha mais de um serviço para orquestrar.

### Docker Hub em vez de GHCR

**Vantagem:** é o registro que todo mundo conhece; `docker pull cripto-api` sem prefixo.

**Desvantagem:** exige conta e token separados, guardados como *secret* no repositório.
O GHCR autentica com o `GITHUB_TOKEN` que o Actions já tem, a imagem aparece na página do
próprio repositório, e não há segundo serviço para manter.

## Consequências

**Mais fácil:** nada expira. Imagem no GHCR, workflow verde e compose funcionam daqui a
dois anos do mesmo jeito que hoje — o que importa para um repositório que vai ficar parado.

**Mais fácil:** a mitigação 2 do ADR 0010 sai do papel. O risco "espelho envelhece em
silêncio" passa a ter um verificador que não esquece.

**Mais difícil:** não há URL para clicar. Quem quiser ver a API rodando precisa ter Docker
e rodar um comando. É uma barreira real para um recrutador não técnico; para um técnico,
`docker compose up` é a prova mais forte que existe.

**Mais difícil:** TLS e proxy reverso ficam de fora do que este projeto ensina. Entram no
próximo, que vai precisar deles de verdade.

**Perdido, sem atenuante:** a experiência de operar algo em produção — logs em máquina
remota, disco cheio, processo que morre às 3h. Isso não se simula com compose.

## Referências

- Multi-stage builds — <https://docs.docker.com/build/building/multi-stage/>
- GitHub Container Registry —
  <https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry>
- Testcontainers em GitHub Actions —
  <https://java.testcontainers.org/supported_docker_environment/continuous_integration/github_actions/>
- Relacionado: [ADR 0010](0010-espelho-do-schema-do-pipeline-em-teste.md),
  [ADR 0011](0011-flyway-apenas-para-as-tabelas-da-api.md)

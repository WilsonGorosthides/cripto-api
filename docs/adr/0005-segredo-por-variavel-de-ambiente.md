# 0005 — Manter a senha do banco fora do código, em variável de ambiente

**Status:** aceita
**Data:** 2026-09-03

## Contexto

A aplicação precisa de usuário e senha para conectar ao PostgreSQL. O
`application.properties` é o lugar natural para configuração — e é um arquivo versionado,
que vai para um repositório público.

Segredo commitado permanece no histórico do Git mesmo depois de removido: `git rm` apaga o
arquivo do estado atual, não das revisões anteriores. A única correção real é trocar a
credencial e reescrever o histórico.

## Decisão

Nenhuma linha `spring.datasource.password` existe no arquivo. O valor vem da variável de
ambiente `SPRING_DATASOURCE_PASSWORD`.

O Spring faz a conversão sozinho, por *relaxed binding*: maiúscula vira minúscula,
sublinhado vira ponto. Não há biblioteca, anotação nem código envolvido. A mesma regra vale
para qualquer propriedade do arquivo, o que significa que todas podem ser sobrescritas por
ambiente sem alteração de código.

O `.gitignore` também cobre `.env`, `*.env` e `application-*-local.properties` — defesa
contra o dia em que alguém criar um desses.

## Alternativas consideradas

### Senha no `application.properties`, com o arquivo no `.gitignore`

**Vantagem:** toda a configuração num lugar só, sem passo extra para rodar localmente.

**Desvantagem:** esse arquivo é a documentação viva da configuração — quem clona o projeto
precisa ver quais propriedades existem. Removê-lo do Git remove junto a única lista de
o-que-configurar. E cria a categoria "arquivo que existe na minha máquina e ninguém mais
tem", que é onde divergências de ambiente nascem.

### Perfil `local` com `application-local.properties` não versionado

**Vantagem:** mantém o arquivo principal versionado e isola o segredo num arquivo separado.
Padrão comum em times.

**Desvantagem:** exige ativar o perfil ao rodar, e o arquivo não versionado tem o mesmo
problema de divergência. Continua sendo segredo em disco, em texto puro — só que num
arquivo diferente.

Permanece como opção se o número de propriedades locais crescer a ponto de tornar a
exportação de variáveis incômoda.

### Cofre de segredos (Vault, AWS Secrets Manager)

**Vantagem:** rotação automática, auditoria de acesso, segredo nunca em disco.

**Desvantagem:** infraestrutura adicional para um projeto de duas peças. É o caminho certo
em produção corporativa e desproporcional aqui.

## Consequências

**Mais fácil:** o repositório pode ser público sem revisão de segredo a cada commit.

**Mais fácil:** a migração para banco gerenciado na nuvem, prevista para a última etapa do
projeto, não exigirá alteração de código — apenas variáveis diferentes no ambiente de
destino. É o mesmo mecanismo que já permitiu ao `cripto-pipeline` alternar entre SQLite e
PostgreSQL sem tocar no código.

**Mais difícil:** rodar localmente exige exportar a variável antes. Esquecer produz uma
falha de inicialização com mensagem clara (`Failed to determine a suitable driver class`
quando falta a URL; erro de autenticação quando falta a senha), mas ainda assim é um passo
a mais.

**Mais difícil:** a senha existe em texto puro no ambiente do processo, visível a quem
inspecionar o processo na máquina. Aceitável neste contexto; não seria em produção
multiusuário.

## Referências

- *The Twelve-Factor App*, fator III — Config — <https://12factor.net/config>
- *Relaxed binding* no Spring Boot —
  <https://docs.spring.io/spring-boot/reference/features/external-config.html>

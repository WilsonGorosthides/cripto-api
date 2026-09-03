# 0007 — Adotar Maven, com wrapper versionado

**Status:** aceita
**Data:** 2026-09-03

## Contexto

O projeto foi gerado pelo Spring Initializr, cujo padrão é Gradle — foi preciso pedir
`type=maven-project` explicitamente. A escolha da ferramenta de build determina como as
dependências são declaradas, como o CI é configurado e como o container é construído, e
trocá-la depois custa reescrever tudo isso.

Contexto adicional: este projeto tem também função de aprendizado da plataforma Java, e as
vagas visadas são de ambiente corporativo.

## Decisão

Maven, com o wrapper (`mvnw` / `mvnw.cmd` / `.mvn/wrapper/`) versionado no repositório.
Maven não é instalado na máquina — o wrapper baixa a versão exata declarada no projeto.

## Alternativas consideradas

### Gradle

**Vantagem:** builds incrementais e cache tornam a compilação sensivelmente mais rápida em
projetos grandes. O script é muito menos verboso que XML, e o build é programável: dá para
expressar lógica condicional sem plugin.

**Desvantagem:** essa mesma programabilidade é o custo. Um `build.gradle` é código Groovy
ou Kotlin executável — quando quebra, depura-se um programa, com estado e ordem de
avaliação. Um `pom.xml` quebrado é um XML errado, e o erro aponta para a linha. Para quem
está aprendendo a plataforma, o modelo declarativo remove uma fonte inteira de confusão.

Além disso, o ecossistema corporativo Java é majoritariamente Maven, e a familiaridade com
ele tem valor prático imediato.

### Maven instalado na máquina, sem wrapper

**Vantagem:** um arquivo a menos no repositório, e comando mais curto (`mvn` em vez de
`./mvnw`).

**Desvantagem:** a versão do build passa a ser propriedade da máquina, não do projeto.
Máquinas diferentes com versões diferentes produzem builds diferentes a partir do mesmo
código — exatamente a classe de problema que o "funciona na minha máquina" nomeia. O
wrapper elimina isso e ainda dispensa qualquer instalação prévia por parte de quem clona.

## Consequências

**Mais fácil:** quem clona o repositório roda `./mvnw test` sem instalar nada além do JDK.
O mesmo vale para o CI e para a etapa de build dentro do Dockerfile.

**Mais fácil:** o `spring-boot-starter-parent` centraliza o gerenciamento de versões, e
nenhuma dependência precisa declarar a sua. Builds são reprodutíveis ao longo do tempo.

**Mais difícil:** builds serão mais lentos que o equivalente em Gradle, e a diferença
cresce com o tamanho do projeto.

**Mais difícil:** expressar lógica condicional de build exige plugin ou perfil, em vez de
um `if`.

## Referências

- Maven Wrapper —
  <https://maven.apache.org/tools/wrapper/>
- `spring-boot-starter-parent` —
  <https://docs.spring.io/spring-boot/maven-plugin/using.html>

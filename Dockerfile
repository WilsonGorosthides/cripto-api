# syntax=docker/dockerfile:1

# Dois estagios (docs/adr/0014): o primeiro tem JDK, Maven e codigo-fonte e
# produz o jar; o segundo tem so JRE e o jar. Nada do que serve para
# construir vai para a imagem que roda.

# ---------------------------------------------------------------- 1. build
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Primeiro so o que define as dependencias. Enquanto o pom.xml nao mudar,
# esta camada fica em cache e o download de dependencias nao se repete a
# cada alteracao de codigo.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# chmod porque o contexto pode vir de Windows, onde nao existe bit de execucao.
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Agora o codigo. Os testes NAO rodam aqui: eles precisam do Docker
# (Testcontainers), que nao existe dentro de um build. Rodam no CI, antes
# de a imagem ser publicada - ver .github/workflows/ci.yml.
COPY src/ src/
RUN ./mvnw -B -q -DskipTests package \
 && cp target/cripto-api-*.jar app.jar

# -------------------------------------------------------------- 2. runtime
FROM eclipse-temurin:17-jre-alpine

# Usuario sem privilegio. Se a aplicacao for comprometida, quem a controla
# nao e root dentro do container.
RUN addgroup -S api && adduser -S api -G api
USER api
WORKDIR /app

COPY --from=build --chown=api:api /app/app.jar app.jar

# A JVM le o limite de memoria do container; 75% dele vai para o heap.
# Sem isto, ela dimensiona pelo host e pode ser morta por OOM pelo Docker.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

# Alternativa descartada: extrair o jar em camadas (dependencies, loader,
# application) para que so a camada da aplicacao mude a cada build. Menor
# tempo de push para um projeto que muda toda hora; irrelevante para um que
# vai ser congelado. A simplicidade de um jar unico venceu.

FROM ghcr.io/graalvm/native-image-community:25i3-25.0.4.1-ol8-20260825 AS build
WORKDIR /build

ARG MAVEN_VERSION=3.9.16
RUN microdnf install -y tar gzip && \
    curl -fsSL "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" -o /tmp/maven.tar.gz && \
    tar -xzf /tmp/maven.tar.gz -C /opt && \
    ln -s "/opt/apache-maven-${MAVEN_VERSION}" /opt/maven && \
    rm /tmp/maven.tar.gz
ENV PATH="/opt/maven/bin:${PATH}"

# Projeto de módulo único: copia o pom e o código-fonte
COPY pom.xml .
COPY src src

# Gera o executável nativo em target/estoque (nome = artifactId)
RUN mvn -Pnative native:compile -DskipTests

FROM debian:bookworm-slim
WORKDIR /app
EXPOSE 8082
COPY --from=build /build/target/estoque /app/app
ENTRYPOINT ["/app/app"]

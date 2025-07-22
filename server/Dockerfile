FROM openjdk:21-jdk-slim

WORKDIR /app

RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

COPY . /app

RUN ./mvnw package -DskipTests || mvn package -DskipTests

EXPOSE 8887

CMD ["java", "-cp", "target/classes", "org.br.PokemonServer"]

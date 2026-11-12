# 1era etapa: Construccion
FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /app

# Copiamos el wrapper de maven y el archivo de configuracion
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Damos permisos de ejecucion al wrapper y descargamos dependencias
RUN chmod +x ./mvnw
COPY src src
RUN ./mvnw clean package -DskipTests


# Etapa 2: Imagen de Produccion
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiamos el .jar generado
COPY --from=build /app/target/Eventia-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

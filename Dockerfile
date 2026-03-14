FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Копируем jar файл
COPY target/training-app-1.0.0.jar app.jar

EXPOSE 8080

# Запускаем с prod профилем
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]

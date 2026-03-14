FROM openjdk:17-jdk-slim

WORKDIR /app

# Копируем jar файл
COPY target/training-app-1.0.0.jar app.jar

# Копируем все properties файлы (на всякий случай)
COPY src/main/resources/application*.properties ./

# Указываем порт
EXPOSE 8080

# Запускаем с prod профилем
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]

# Build stage - Sử dụng Maven để build ứng dụng
FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app
COPY . .
RUN mvn -B clean package -DskipTests

# Runtime stage - Sử dụng image JRE nhỏ gọn
FROM eclipse-temurin:11-jre

# Cài đặt các phụ thuộc hệ thống
RUN apt-get update \
    && apt-get install -y --no-install-recommends netcat-openbsd locales \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean \
    && locale-gen C.UTF-8

# Thiết lập biến môi trường
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

# Thiết lập thư mục làm việc và copy file JAR đã build
WORKDIR /electro-server
COPY --from=build /app/target/electro-*.jar app.jar

# Mở cổng và chạy ứng dụng
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
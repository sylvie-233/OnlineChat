# ==================== 构建阶段 ====================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# 先复制 pom，利用 Docker 缓存下载依赖
COPY pom.xml .
COPY onlinechat-common/pom.xml    onlinechat-common/
COPY onlinechat-repository/pom.xml onlinechat-repository/
COPY onlinechat-service/pom.xml   onlinechat-service/
COPY onlinechat-connect/pom.xml   onlinechat-connect/
COPY onlinechat-task/pom.xml      onlinechat-task/
COPY onlinechat-server/pom.xml    onlinechat-server/

RUN mvn dependency:go-offline -pl onlinechat-server -am -B

# 复制源码并构建
COPY . .
RUN mvn clean package -pl onlinechat-server -am -DskipTests -B

# ==================== 运行阶段 ====================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S onlinechat && adduser -S onlinechat -G onlinechat
USER onlinechat

COPY --from=builder /build/onlinechat-server/target/onlinechat-server-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

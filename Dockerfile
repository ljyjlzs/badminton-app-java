# 构建阶段
FROM maven:3.8-openjdk-8 AS builder

WORKDIR /app

# 复制 pom.xml 并下载依赖（利用 Docker 缓存层）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn package -DskipTests -B

# 运行阶段
FROM openjdk:8-jre-slim

LABEL maintainer="badminton-app"
LABEL description="羽毛球报名小程序后端"

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建非 root 用户
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 创建日志目录
RUN mkdir -p /var/log/badminton && chown -R appuser:appuser /var/log/badminton

WORKDIR /app

# 从构建阶段复制 jar 包
COPY --from=builder /app/target/*.jar app.jar

# 切换到非 root 用户
USER appuser

# 暴露端口
EXPOSE 8049

# JVM 参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8049/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]

# 使用官方的 OpenJDK 11 镜像作为构建环境
FROM openjdk:11-jdk-slim as builder

# 设置工作目录
WORKDIR /app

# 将项目文件复制到容器中
COPY . .

# 构建项目，跳过测试
RUN ./mvnw package -DskipTests


# 使用更小的 JRE 镜像作为最终的运行环境
FROM openjdk:11-jre-slim

# 安装字体配置
RUN apt-get update && apt-get install -y fontconfig

# 设置工作目录
WORKDIR /app

# 从构建阶段复制 JAR 文件
COPY --from=builder /app/target/*.jar app.jar

# 暴露应用程序运行的端口
EXPOSE 8080

# 运行应用程序
ENTRYPOINT ["java","-jar","app.jar"]

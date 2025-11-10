# 使用本地仓库的 OpenJDK 11 JDK 镜像作为运行环境
FROM localhost:5000/library/openjdk:11-jdk

# 设置工作目录
WORKDIR /app

# 复制 JAR 文件
COPY app.jar app.jar

# 暴露应用程序运行的端口
EXPOSE 6324

# 运行应用程序
ENTRYPOINT ["java","-Djava.awt.headless=true","-jar","app.jar"]

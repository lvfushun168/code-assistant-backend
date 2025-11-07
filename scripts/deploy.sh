#!/bin/bash

# 如果任何命令执行失败，立即退出脚本
set -e

# --- 配置 ---
# 应用程序的名称
APP_NAME="code-assistant-backend"
# 映射到主机的端口
HOST_PORT=8080
# 容器内应用程序运行的端口
CONTAINER_PORT=8080
# 服务器上传 JAR 文件的目录
PROJECT_DIR="/lfs/project"
# Docker 镜像的名称
IMAGE_NAME="lfs/${APP_NAME}"
# Docker 镜像的标签
IMAGE_TAG="latest"

# --- 部署 ---

echo "开始部署 ${APP_NAME}..."

# 检查是否提供了 JAR 文件路径
if [ -z "$1" ]; then
  echo "用法: $0 <your-app.jar 的路径>"
  exit 1
fi

JAR_PATH=$1

# 检查 JAR 文件是否存在
if [ ! -f "${JAR_PATH}" ]; then
    echo "错误: 在 ${JAR_PATH} 未找到 JAR 文件"
    exit 1
fi

# 为构建上下文创建一个临时目录
BUILD_CONTEXT_DIR=$(mktemp -d)

# 将 JAR 文件和 Dockerfile 复制到构建上下文目录
cp "${JAR_PATH}" "${BUILD_CONTEXT_DIR}/app.jar"

# 在构建上下文目录中创建一个简单的 Dockerfile
cat <<EOF > "${BUILD_CONTEXT_DIR}/Dockerfile"
FROM openjdk:11-jre-slim
WORKDIR /app
COPY app.jar app.jar
EXPOSE ${CONTAINER_PORT}
ENTRYPOINT ["java","-jar","app.jar"]
EOF


# 进入构建上下文目录
cd "${BUILD_CONTEXT_DIR}"

# 如果容器正在运行，则停止并删除现有容器
if [ $(docker ps -q -f name=^/${APP_NAME}$) ]; then
    echo "正在停止并删除现有容器..."
    docker stop "${APP_NAME}"
    docker rm "${APP_NAME}"
fi

# 删除旧的镜像
if [ $(docker images -q "${IMAGE_NAME}:${IMAGE_TAG}") ]; then
    echo "正在删除旧的镜像..."
    docker rmi "${IMAGE_NAME}:${IMAGE_TAG}"
fi

# 构建 Docker 镜像
echo "正在构建 Docker 镜像 ${IMAGE_NAME}:${IMAGE_TAG}..."
docker build -t "${IMAGE_NAME}:${IMAGE_TAG}" .

# 运行 Docker 容器
    echo "正在运行 Docker 容器..."
    # 创建宿主机日志目录（如果不存在）
    mkdir -p "${PROJECT_DIR}/logs"
    docker run -d --name "${APP_NAME}" -p "${HOST_PORT}:${CONTAINER_PORT}" -v "${PROJECT_DIR}/logs:/app/logs" "${IMAGE_NAME}:${IMAGE_TAG}"

# 清理构建上下文目录rm -rf "${BUILD_CONTEXT_DIR}"

echo "部署成功！"
echo "应用程序现在可以通过 http://8.148.146.195:${HOST_PORT} 访问"

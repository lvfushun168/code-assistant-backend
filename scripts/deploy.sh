#!/bin/bash

# 如果任何命令执行失败，立即退出脚本
set -e

# --- 配置 ---
# 应用程序的名称
APP_NAME="code-assistant-backend"
# 映射到主机的端口
HOST_PORT=8080
# 容器内应用程序运行的端口
CONTAINER_PORT=6324
# 服务器上传 JAR 文件的目录
PROJECT_DIR="/lfs/project"
# 本地 Docker 仓库的地址
REGISTRY_HOST="localhost:5000"
# Docker 镜像的名称
IMAGE_NAME="${REGISTRY_HOST}/lfs/${APP_NAME}"
# Docker 镜像的标签
IMAGE_TAG="latest"

# 宿主机存储路径配置
HOST_DATA_DIR="/lfs/data/files"
HOST_LOG_DIR="/lfs/logs"

# 容器内部路径配置
CONTAINER_DATA_DIR="/app/data/files"
CONTAINER_LOG_DIR="/app/logs"


# --- 部署 ---

echo "开始部署 ${APP_NAME}..."

# 启动本地 Docker 仓库（如果尚未运行）
if ! docker ps -f name=^/registry$ --format '{{.Names}}' | grep -q registry; then
    echo "正在启动本地 Docker 仓库..."
    docker run -d -p 5000:5000 --restart=always --name registry registry:2
    sleep 5 # 等待仓库启动
fi

# --- 准备基础镜像 ---
BASE_IMAGE_NAME="openjdk:11-jdk"
LOCAL_BASE_IMAGE_NAME="${REGISTRY_HOST}/library/${BASE_IMAGE_NAME}"

# 检查基础镜像是否已在本地仓库
if ! docker manifest inspect "${LOCAL_BASE_IMAGE_NAME}" > /dev/null 2>&1; then
    echo "本地仓库中未找到基础镜像 ${LOCAL_BASE_IMAGE_NAME}。"
    echo "正在从 Docker Hub 拉取 ${BASE_IMAGE_NAME}..."
    docker pull "${BASE_IMAGE_NAME}"

    echo "正在为基础镜像打上本地仓库标签..."
    docker tag "${BASE_IMAGE_NAME}" "${LOCAL_BASE_IMAGE_NAME}"

    echo "正在将基础镜像推送到本地仓库..."
    docker push "${LOCAL_BASE_IMAGE_NAME}"
    echo "基础镜像准备完成。"
else
    echo "基础镜像 ${LOCAL_BASE_IMAGE_NAME} 已存在于本地仓库。"
fi

# 检查是否提供了 JAR 文件路径
if [ -z "$1" ]; then
  echo "用法: $0 <app.jar 的路径>"
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
cp "Dockerfile" "${BUILD_CONTEXT_DIR}/Dockerfile"


# 进入构建上下文目录
cd "${BUILD_CONTEXT_DIR}"

# 如果容器正在运行，则停止并删除现有容器
if [ $(docker ps -a -q -f name=^/${APP_NAME}$) ]; then
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

# 推送镜像到本地仓库
echo "正在推送镜像到本地仓库..."
docker push "${IMAGE_NAME}:${IMAGE_TAG}"

# 运行 Docker 容器
echo "正在运行 Docker 容器..."
docker run -d \
    --name "${APP_NAME}" \
    --restart=always \
    -p "${HOST_PORT}:${CONTAINER_PORT}" \
    -v "${HOST_DATA_DIR}:${CONTAINER_DATA_DIR}" \
    -v "${HOST_LOG_DIR}:${CONTAINER_LOG_DIR}" \
    -e MYSQL_PASSWORD="YOUR_REAL_PASSWORD" \
    -e MYSQL_HOST="172.17.0.1" \
    -e SPRING_PROFILES_ACTIVE="prod" \
    "${IMAGE_NAME}:${IMAGE_TAG}"

# 清理构建上下文目录
rm -rf "${BUILD_CONTEXT_DIR}"

echo "部署成功！"
echo "应用程序现在可以通过 http://8.148.146.195:${HOST_PORT} 访问"

#!/bin/bash

# 获取脚本所在的真实目录
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
# 切换到项目根目录 (脚本所在目录的上一级)
cd "$SCRIPT_DIR/.."

# 如果任何命令失败，立即退出。
set -e

# 定义目标JAR文件路径
JAR_PATH="target/code-assistant-backend-0.0.1-SNAPSHOT.jar"

# 检查JAR文件是否存在
if [ ! -f "$JAR_PATH" ]; then
    echo "错误：未找到JAR文件：$JAR_PATH"
    echo "请先使用 'mvn clean package' 构建项目。"
    exit 1
fi

# 定义远程服务器详细信息
REMOTE_USER="root"
REMOTE_HOST="8.148.146.195"
REMOTE_DIR="/lfs/project"
PASSWORD_FILE="/Users/lvfushun/Downloads/备份/password.txt"

# 检查密码文件是否存在
if [ ! -f "$PASSWORD_FILE" ]; then
    echo "错误：未找到密码文件：$PASSWORD_FILE"
    exit 1
fi

# 从文件中读取密码
PASSWORD=$(cat "$PASSWORD_FILE")

# 使用 sshpass 安全地将JAR文件复制到远程服务器
echo "正在上传JAR文件到 $REMOTE_HOST..."
SSHPASS="$PASSWORD" sshpass -e scp -o StrictHostKeyChecking=no "$JAR_PATH" "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}/"

echo "上传完成。"

# 使用 sshpass 在远程服务器上执行部署脚本
echo "正在 $REMOTE_HOST 上执行部署脚本..."
SSHPASS="$PASSWORD" sshpass -e ssh -o StrictHostKeyChecking=no "${REMOTE_USER}@${REMOTE_HOST}" "sh ${REMOTE_DIR}/deploy.sh ${REMOTE_DIR}/code-assistant-backend-0.0.1-SNAPSHOT.jar"

echo "脚本执行完毕。"

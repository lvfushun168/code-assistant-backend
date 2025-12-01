# code-assistant-backend

本仓库是 `code-assistant` 项目的后端服务，基于 Spring Boot 开发，为文档编辑器提供核心的 API 支持。

## 技术栈

- **后端**: Spring Boot 2.6.13
- **语言**: Java 11
- **数据库**: MySQL 8.0
- **ORM**: MyBatis-Plus
- **认证**: Spring Security + JWT
- **构建工具**: Maven
- **容器化**: Docker

---

## 准备工作

在本地运行或部署此项目之前，请确保您的环境中已安装以下软件：

1.  **JDK 11**: [Oracle JDK](https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html) 或 OpenJDK
2.  **Maven 3.6+**: [Apache Maven](https://maven.apache.org/download.cgi)
3.  **MySQL 8.0+**: [MySQL Community Server](https://dev.mysql.com/downloads/mysql/)
4.  **Docker**: [Docker Desktop](https://www.docker.com/products/docker-desktop) (用于容器化部署)

---

## 本地开发环境运行

按照以下步骤在您的本地机器上启动后端服务。

### 1. 克隆项目

```bash
git clone <your-repository-url>
cd code-assistant-backend
```

### 2. 配置并启动 MySQL

您需要一个正在运行的 MySQL 8.0+ 实例。

-   **创建数据库**:
    请在您的 MySQL 中创建一个名为 `lfs` 的数据库。
    ```sql
    CREATE DATABASE lfs CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    ```
-   **执行初始化 SQL**:
    在 `lfs` 数据库中执行以下 SQL 脚本以创建所需的表结构。

    ```sql
    CREATE TABLE `user` (
      `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
      `username` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
      `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
      `nickname` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
      `phone_num` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标志 (0: 未删除, 1: 已删除)',
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_username` (`username`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

    CREATE TABLE `dir` (
      `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
      `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目录名',
      `parent_id` bigint(20) DEFAULT NULL COMMENT '上级目录ID',
      `user_id` bigint(20) NOT NULL COMMENT '用户ID',
      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标志 (0: 未删除, 1: 已删除)',
      PRIMARY KEY (`id`),
      KEY `idx_user_id` (`user_id`),
      KEY `idx_parent_id` (`parent_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='目录表';

    CREATE TABLE `content` (
      `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
      `dir_id` bigint(20) NOT NULL COMMENT '目录ID',
      `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
      `file_path` varchar(1024) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件存储路径',
      `creator` bigint(20) NOT NULL COMMENT '创建者用户ID',
      `encrypted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否加密 (0: 否, 1: 是)',
      `content_hash` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '内容摘要或哈希',
      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标志 (0: 未删除, 1: 已删除)',
      PRIMARY KEY (`id`),
      KEY `idx_dir_id` (`dir_id`),
      KEY `idx_creator` (`creator`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容表';
    ```


### 3. 配置环境变量

后端服务通过环境变量读取 MySQL 的密码。

-   **macOS / Linux**:
    ```bash
    export MYSQL_PASSWORD="your_mysql_password"
    ```
-   **Windows (PowerShell)**:
    ```powershell
    $env:MYSQL_PASSWORD="your_mysql_password"
    ```
您也可以在 IDE (如 IntelliJ IDEA) 的运行配置中直接设置此环境变量。

### 4. 运行应用程序

完成以上步骤后，使用 Maven 启动 Spring Boot 应用。

```bash
mvn spring-boot:run
```

服务启动后，您可以通过 `http://localhost:6324/lfs-code-assistant` 访问 API。

---

## Docker 部署

您也可以将此应用程序容器化部署。

### 1. 打包应用程序

首先，使用 Maven 将项目打包成一个可执行的 JAR 文件。

```bash
mvn clean package -DskipTests
```

此命令会在 `target/` 目录下生成 `code-assistant-backend-0.0.1-SNAPSHOT.jar` 文件。

### 2. 准备 Dockerfile

项目根目录下的 `Dockerfile` 默认使用了私有镜像仓库 `localhost:5000`。为了方便在任何机器上构建，建议将其修改为使用官方的公共镜像。

**修改 `Dockerfile`:**

将 `FROM localhost:5000/library/openjdk:11-jdk` 修改为：

```dockerfile
FROM openjdk:11-jdk
```

然后，将打包好的 JAR 文件重命名为 `app.jar` 并放在 `Dockerfile` 同级目录下。

```bash
cp target/code-assistant-backend-0.0.1-SNAPSHOT.jar app.jar
```

### 3. 构建 Docker 镜像

在项目根目录下，执行以下命令构建镜像。

```bash
docker build -t code-assistant-backend:latest .
```

### 4. 运行 Docker 容器

使用以下命令启动容器。请将 `YOUR_MYSQL_PASSWORD` 替换为您的真实密码，并确保 `MYSQL_HOST` 指向您的 MySQL 服务器地址。

-   `172.17.0.1` 通常是 Docker 容器访问宿主机的 IP 地址。如果您的 MySQL 部署在其他服务器上，请替换为相应的 IP。
-   命令会创建并挂载 `/your/local/path/data` 和 `/your/local/path/logs` 目录用于持久化存储文件和日志。请替换为您希望的本地路径。

```bash
docker run -d \
  --name code-assistant-backend \
  --restart=always \
  -p 6324:6324 \
  -v /your/local/path/data:/app/data/files \
  -v /your/local/path/logs:/app/logs \
  -e MYSQL_PASSWORD="YOUR_MYSQL_PASSWORD" \
  -e MYSQL_HOST="172.17.0.1" \
  code-assistant-backend:latest
```

容器启动后，服务将在 `http://<your-host-ip>:6324/lfs-code-assistant` 上可用。

**注意**: `deploy.sh` 脚本提供了更复杂的自动化部署流程，包括管理本地 Docker 仓库，但对于大多数标准部署场景，以上手动步骤已足够。

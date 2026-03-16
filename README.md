# code-assistant-backend

本仓库是 `code-assistant` 项目的后端服务，基于 Spring Boot 开发，为文档编辑器提供核心的 API 支持。

## 项目简介

code-assistant-backend 是一个功能完整的云端文档管理系统，支持用户账户管理、目录树结构、文档存储、文件加密以及云端文件同步等功能。系统采用 JWT 认证机制，支持跨设备访问，并提供 RESTful API 供前端调用。

## 核心功能

### 1. 用户认证与授权
- 用户注册与登录
- 密码修改
- JWT Token 认证与续期
- 图形验证码生成
- 用户密钥包管理（用于加密文件的跨设备同步）

### 2. 目录管理
- 获取用户目录树结构
- 创建、更新、删除目录
- 支持多级目录嵌套

### 3. 内容管理
- 创建、更新、删除文档
- 列出指定目录下的所有文档
- 文档下载
- 支持文档元数据管理
- 文件加密存储（可选）

### 4. 云端文件同步
- 检查云端文件是否存在
- 单文件上传与下载
- ZIP 批量上传（保留目录结构）
- 目录打包下载（ZIP 格式）
- 支持通过目录路径或目录 ID 操作
- 覆盖控制机制

## 技术栈

- **后端框架**: Spring Boot 2.6.13
- **开发语言**: Java 11
- **数据库**: MySQL 8.0
- **ORM 框架**: MyBatis-Plus 3.5.2
- **认证授权**: Spring Security + JWT (jjwt 0.11.5)
- **连接池**: HikariCP 5.0.1
- **加密库**: Bouncy Castle 1.70 (AES、Argon2)
- **工具库**: Hutool 5.8.2、Guava 31.1、Apache Commons Lang3
- **验证框架**: Hibernate Validator 6.2.0
- **构建工具**: Maven
- **容器化**: Docker

## API 接口说明

### 基础路径
```
http://localhost:6324/lfs-code-assistant
```

### 账户管理 (`/account`)
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/account/register` | 用户注册 |
| POST | `/account/login` | 用户登录 |
| POST | `/account/change-password` | 修改密码 |
| POST | `/account/renew-token` | Token 续期 |
| GET | `/account/key-package` | 获取用户密钥包 |

### 验证码 (`/captcha`)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/captcha/generate` | 生成图形验证码 |

### 目录管理 (`/dir`)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/dir/tree` | 获取目录树 |
| POST | `/dir` | 创建目录 |
| PUT | `/dir` | 更新目录 |
| DELETE | `/dir/{id}` | 删除目录 |

### 内容管理 (`/content`)
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/content` | 创建文档 |
| PUT | `/content` | 更新文档 |
| DELETE | `/content/{id}` | 删除文档 |
| GET | `/content/{dirId}` | 列出目录文档 |
| GET | `/content/download/{id}` | 下载文档 |

### 云端文件同步 (`/api/fs`)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/fs/check` | 检查文件是否存在 |
| POST | `/api/fs/upload` | 上传单个文件 |
| GET | `/api/fs/download` | 下载单个文件 |
| POST | `/api/fs/upload-zip` | 上传 ZIP 文件 |
| GET | `/api/fs/download-zip` | 下载目录为 ZIP |
| POST | `/api/fs/upload-zip-by-id` | 通过目录 ID 上传 ZIP |
| GET | `/api/fs/download-zip-by-id` | 通过目录 ID 下载 ZIP |

---

## 准备工作

在本地运行或部署此项目之前，请确保你的环境中已安装以下软件：

1.  **JDK 11**: [Oracle JDK](https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html) 或 OpenJDK
2.  **Maven 3.6+**: [Apache Maven](https://maven.apache.org/download.cgi)
3.  **MySQL 8.0+**: [MySQL Community Server](https://dev.mysql.com/downloads/mysql/)
4.  **Docker**: [Docker Desktop](https://www.docker.com/products/docker-desktop) (用于容器化部署)

---

## 本地开发环境运行

按照以下步骤在你的本地机器上启动后端服务。

### 1. 克隆项目

```bash
git clone <your-repository-url>
cd code-assistant-backend
```

### 2. 配置并启动 MySQL

你需要一个正在运行的 MySQL 8.0+ 实例。

-   **创建数据库**:
    请在你的 MySQL 中创建一个名为 `lfs` 的数据库。
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
你也可以在 IDE (如 IntelliJ IDEA) 的运行配置中直接设置此环境变量。

### 4. 运行应用程序

完成以上步骤后，使用 Maven 启动 Spring Boot 应用。

```bash
mvn spring-boot:run
```

服务启动后，你可以通过 `http://localhost:6324/lfs-code-assistant` 访问 API。

---

## Docker 部署

你也可以将此应用程序容器化部署。

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

使用以下命令启动容器。请将 `YOUR_MYSQL_PASSWORD` 替换为你的真实密码，并确保 `MYSQL_HOST` 指向你的 MySQL 服务器地址。

-   `172.17.0.1` 通常是 Docker 容器访问宿主机的 IP 地址。如果你的 MySQL 部署在其他服务器上，请替换为相应的 IP。
-   命令会创建并挂载 `/your/local/path/data` 和 `/your/local/path/logs` 目录用于持久化存储文件和日志。请替换为你希望的本地路径。

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

### 5. 发布
在把deploy放到服务器/lfs/project目录后，根据你本地环境执行下面脚本进行部署。
* win：先安装[PuTTY](https://www.chiark.greenend.org.uk/~sgtatham/putty/latest.html)，再执行plink root@8.148.146.195将主机密钥保存到本地（无需输入密码），然后执行publish-win.bat
* mac：直接执行publish-mac.sh

---

## 待实现功能

### 功能增强
- [ ] **版本控制**: 为文档添加版本历史记录，支持查看历史版本和版本回退
- [ ] **文档分享**: 生成分享链接，支持公开分享和密码保护
- [ ] **文档协作**: 支持多人实时协作编辑文档
- [ ] **全文搜索**: 实现文档内容的全文检索功能
- [ ] **标签系统**: 为文档添加标签，支持分类和筛选
- [ ] **收藏功能**: 支持用户收藏常用文档和目录

### 安全与权限
- [ ] **细粒度权限控制**: 实现基于角色的访问控制（RBAC）
- [ ] **文件访问日志**: 记录文件访问和操作日志
- [ ] **二次验证**: 敏感操作（如删除、修改密码）支持二次验证
- [ ] **IP 白名单**: 支持配置访问 IP 白名单
- [ ] **审计日志**: 记录系统重要操作和异常事件

### 性能优化
- [ ] **缓存优化**: 引入 Redis 缓存热点数据和目录树
- [ ] **异步处理**: 文件上传、ZIP 压缩等耗时操作改为异步处理
- [ ] **分页优化**: 大数据量列表查询优化分页性能
- [ ] **CDN 加速**: 静态资源和文件下载接入 CDN
- [ ] **数据库优化**: 添加合适的索引，优化慢查询

### 用户体验
- [ ] **文件预览**: 支持在线预览常见文件格式（图片、PDF、代码等）
- [ ] **拖拽上传**: 支持拖拽文件批量上传
- [ ] **最近访问**: 显示用户最近访问的文档
- [ ] **回收站**: 删除的文件进入回收站，支持恢复
- [ ] **快捷键**: 支持常用操作的快捷键

### 监控与运维
- [ ] **健康检查**: 添加 Spring Boot Actuator 健康检查端点
- [ ] **监控告警**: 接入监控系统（如 Prometheus + Grafana）
- [ ] **日志聚合**: 统一日志收集和分析（如 ELK Stack）
- [ ] **性能监控**: 接入 APM 工具监控应用性能
- [ ] **自动化测试**: 添加单元测试和集成测试

### 多端支持
- [ ] **移动端 API**: 优化 API 以支持移动端应用
- [ ] **第三方集成**: 支持第三方存储（如 OSS、S3）
- [ ] **Webhook**: 支持 Webhook 通知机制
- [ ] **开放 API**: 提供开放 API 供第三方调用

### 国际化
- [ ] **多语言支持**: 支持中英文等多语言切换
- [ ] **时区处理**: 完善时区处理机制

---
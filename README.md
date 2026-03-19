# AI Hub - AI 学习项目

基于 Spring Boot 3 + LangChain4j 的 AI 学习项目，展示多种主流 AI 集成模式。

## 项目简介

这是一个演示项目，展示了如何在 Spring Boot 应用中集成各类 AI 能力：

- **RAG 检索增强生成** - 基于文档的问答助手，支持持久化向量存储
- **多 Agent 顺序工作流** - 创意写作 -> 受众适配 -> 风格编辑的三阶段工作流
- **MCP 模型上下文协议** - 让 AI 安全访问本地文件系统
- **AI 健身计划生成器** - 完整全栈 AI 应用，根据用户偏好生成个性化周训练计划
- **JWT 认证授权** - 完整的用户认证体系

## 技术栈

**后端：**
- Java 21
- Spring Boot 3.5.11
- LangChain4j 1.11.0-beta19
- MyBatis-Plus 3.5.5
- MySQL + Druid 连接池
- JJWT 0.12.6 (JWT)

**AI 模型：**
- 聊天模型：ByteDance Ark (豆包) - OpenAI 兼容接口
- 嵌入模型：Alibaba Dashscope text-embedding-v4

**前端：**
- React 18 + Vite
- 现代前端工程化

## 项目结构

```
ai-hub/
├── src/main/java/com/mordan/aihub/
│   ├── AiStudyApplication.java      # 启动入口
│   ├── studydemo/                   # AI 演示模块
│   │   ├── assistant/               # AI 助手服务
│   │   ├── agent/                   # 多 Agent (CreativeWriter, AudienceEditor, StyleEditor)
│   │   ├── config/                  # AI 服务配置
│   │   ├── rag/                     # RAG 配置
│   │   ├── mcp/                     # MCP 文件系统集成
│   │   └── tool/                    # 自定义工具 (CalculateTool)
│   ├── fitness/                     # AI 健身计划生成器 (全栈应用)
│   │   ├── controller/              # REST 接口
│   │   ├── service/                 # 业务服务 (同步+异步生成)
│   │   ├── domain/                  # 实体/VO/AI 结果类
│   │   ├── mapper/                  # MyBatis-Plus Mapper
│   │   └── config/                  # 配置类
│   ├── auth/                        # JWT 认证模块
│   └── common/                      # 公共组件 (统一响应、异常处理、CORS)
├── src/main/resources/
│   ├── application.yaml             # 配置模板 (占位符)
│   ├── application-local.yaml       # 本地配置 (填写你的 API Key)
│   ├── docs/                        # RAG 文档
│   └── prompts/                     # 提示词模板
├── ai-study-fronent/fitness-app/    # React 前端项目
└── pom.xml
```

## 环境准备

1. JDK 21+
2. Maven 3.8+
3. MySQL 8.0+
4. Node.js 18+ (前端)
5. npx (用于 MCP 文件系统服务器，随 Node.js 自带)

## 配置

1. 复制 `application-local.yaml` 并填写你的配置：

```yaml
# 修改为你的 MySQL 连接
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/你的数据库名?useUnicode=true&...
    username: 用户名
    password: 密码

# 修改为你的 ByteDance Ark API
langchain4j:
  open-ai:
    chat-model:
      api-key: 你的 Ark API Key

# 修改为你的 Dashscope API Key
  community:
    dashscope:
      embedding-model:
        api-key: 你的 Dashscope API Key

# 修改为你的 MCP 允许访问目录
mcp:
  filesystem:
    root-dirs:
      - "你的本地目录路径"

# 修改为你的 JWT 密钥
jwt:
  secret: 你的 JWT 密钥
```

2. 创建数据库（可参考实体类自动建表，MyBatis-Plus 可开自增建表）

## 后端启动

```bash
# 编译
mvnw.cmd clean compile

# 运行
mvnw.cmd spring-boot:run -Dspring.profiles.active=local

# 或打包后运行
mvnw.cmd clean package -DskipTests
java -jar target/ai-hub-xxx.jar -Dspring.profiles.active=local
```

后端服务启动在 `http://localhost:9090`

## 前端启动

```bash
# 进入前端目录
cd ai-study-fronent/fitness-app

# 安装依赖
npm install

# 开发模式启动
npm run dev
```

前端默认启动在 `http://localhost:5173`

## 功能说明

### 1. RAG 问答助手

- 启动时自动加载 `src/main/resources/docs/` 下的文档
- 分割文档并创建嵌入，持久化到 `embedding-store.json`
- 基于用户问题检索相关文档片段，给出回答
- 支持 SSE 流式响应

**接口示例：**
```
GET /demo/assistant/chat?memoryId=test&message=请介绍一下文档内容
```

### 2. 多 Agent 故事生成工作流

1. **CreativeWriter** - 根据主题生成故事初稿
2. **AudienceEditor** - 根据受众适配故事内容
3. **StyleEditor** - 根据风格要求改写故事

**接口示例：**
```
GET /demo/assistant/story/work/workFlow?topic=一只猫&audience=儿童&style=幽默
```

### 3. AI 健身计划生成器

用户填写训练偏好，AI 生成个性化周计划：
- 支持不同训练水平（新手/中级/进阶）
- 支持不同健身目标（增肌/减脂/保持健康）
- 可指定重点训练部位
- 可指定可用器械
- 异步生成，前端可轮询状态
- 自动生成每个动作的 B 站视频链接

所有计划保存到数据库，支持重新生成。

### 4. MCP 文件系统集成

通过 [Model Context Protocol](https://modelcontextprotocol.io/) 让 AI 安全访问你指定的本地目录，AI 可以读取文件内容帮助你分析代码和文档。

## 主要 API 端点

| 模块 | 方法 | 端点 | 说明 |
|------|------|------|------|
| RAG | GET | `/demo/assistant/chat/direct` | 非流式问答 |
| RAG | GET | `/demo/assistant/chat` | SSE 流式问答 |
| Agent | GET | `/demo/assistant/story/work/workFlow` | 多 Agent 故事生成 |
| 健身 | POST | `/api/v1/fitness/preferences` | 提交偏好，开始生成 |
| 健身 | GET | `/api/v1/fitness/plans/{planId}` | 获取计划详情 |
| 健身 | GET | `/api/v1/fitness/plans/latest` | 获取最新计划 |
| 认证 | POST | `/api/v1/user/register` | 用户注册 |
| 认证 | POST | `/api/v1/user/login` | 用户登录，获取 Token |

## 开发者约定

详见 [CLAUDE.md](./CLAUDE.md)

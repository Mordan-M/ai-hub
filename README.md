# AI Hub - 企业级 AI 集成实践平台

基于 Spring Boot 3 + LangChain4j 构建的企业级 AI 应用开发框架，展示生产环境中主流 AI 能力的集成模式与最佳实践。

## 项目简介

这是一个完整的 Java 生态 AI 应用开发示范项目，展示如何在企业级 Spring Boot 应用中系统化集成各类先进 AI 能力。项目包含从基础 RAG 问答到高级多 Agent 协作的多种 AI 开发模式，同时提供完整的用户认证、数据持久化、前后端分离的全栈应用示例：

- **RAG 检索增强生成** - 基于私有文档的问答助手，支持磁盘持久化向量存储，启动时免重新 ingestion
- **多 Agent 协作工作流** - 基于专业化分工的多 Agent 顺序处理工作流，展示模块化 AI 能力编排
- **MCP 模型上下文协议** - 遵循 Model Context Protocol 让 AI 安全访问本地文件系统，支持代码分析与文档处理
- **AI 个性化健身计划生成器** - 完整全栈 AI 应用，根据用户训练偏好智能生成个性化周训练计划
- **AI 低代码生成平台** - AI 根据自然语言描述自动生成完整可运行的 Vue 3 前端应用，支持实时预览与一键部署
- **JWT 无状态认证授权** - 基于 Spring Security 6.x 的完整用户认证与权限控制体系

## 技术栈

**后端：**
- Java 21 (虚拟线程)
- Spring Boot 3.5.11
- LangChain4j 1.11.0-beta19
- langgraph4j 2.0.1 (工作流编排)
- MyBatis-Plus 3.5.13
- MySQL 8.0 + Druid 连接池
- JJWT 0.12.6 (JWT 无状态认证)

**AI 模型：**
- 聊天模型：ByteDance Ark (豆包) - OpenAI 兼容接口
- 嵌入模型：Alibaba Dashscope text-embedding-v4

**前端：**
- React 18 + Vite (健身计划应用)
- Vue 3 + Bootstrap (低代码平台前端)

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
│   ├── lowcode/                     # AI 低代码生成平台
│   │   ├── controller/              # REST 接口 (应用管理、生成、预览、部署)
│   │   ├── service/                 # 业务服务 (应用、任务、对话、生成记录)
│   │   ├── domain/                  # 实体/VO/枚举 (应用、任务、对话消息)
│   │   ├── mapper/                  # MyBatis-Plus Mapper
│   │   ├── workflow/                # langgraph4j 工作流节点
│   │   ├── ai/                      # AI 服务 (意图检查、解析、代码生成、校验)
│   │   ├── tools/                   # AI 工具 (文件读写、修改、删除)
│   │   ├── config/                  # 配置类
│   │   └── infrastructure/          # 基础设施 (SSE 推送)
│   ├── auth/                        # JWT 认证模块
│   └── common/                      # 公共组件 (统一响应、异常处理、CORS)
├── src/main/resources/
│   ├── application.yaml             # 配置模板 (占位符)
│   ├── application-local.yaml       # 本地配置 (填写你的 API Key)
│   ├── docs/                        # RAG 文档
│   └── prompts/                     # 提示词模板
├── ai-study-fronent/fitness-app/    # React 前端项目 (健身计划)
├── ai-hub-fronent/lowcode-platfrom/ # Vue 3 前端 (低代码生成平台)
└── pom.xml
```

## 环境准备

1. JDK 21+
2. Maven 3.8+
3. MySQL 8.0+
4. Node.js 18+ (前端 + 低代码构建)
5. npm (用于构建生成的前端项目，随 Node.js 自带)
6. npx (用于 MCP 文件系统服务器，随 Node.js 自带)

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

### 健身计划应用 (React + Vite)
```bash
# 进入前端目录
cd ai-study-fronent/fitness-app

# 安装依赖
npm install

# 开发模式启动
npm run dev
```

前端默认启动在 `http://localhost:5173`

### 低代码生成平台 (Vue 3 + Bootstrap)
```bash
# 进入前端目录
cd ai-hub-fronent/lowcode-platfrom

# 安装依赖
npm install

# 开发模式启动
npm run dev
```

前端默认启动在 `http://localhost:5174` (或其他可用端口)

## 功能特性

### 1. RAG 检索增强问答助手

企业级 RAG 实现，基于私有文档构建问答系统：
- 应用启动时自动加载 `src/main/resources/docs/` 目录下的自定义文档
- 智能文档分割（150 字符最大分段，15 字符重叠）
- 嵌入向量磁盘持久化存储，重启后免重新 ingestion
- 基于相似度检索相关文档片段，结合 LLM 生成准确回答
- 支持 SSE 流式响应，实现打字机效果

**接口示例：**
```
GET /demo/assistant/chat?memoryId=test&message=请介绍一下文档内容
```

### 2. 多 Agent 专业化协作工作流

展示模块化 AI 能力编排，通过专业化分工提升输出质量：

1. **CreativeWriter** - 根据主题生成内容初稿
2. **AudienceEditor** - 根据受众群体适配内容难度与风格
3. **StyleEditor** - 根据指定风格要求进行最终润色改写

**接口示例：**
```
GET /demo/assistant/story/work/workFlow?topic=人工智能&audience=产品经理&style=专业严谨
```

### 3. AI 个性化健身计划生成器

完整的全栈 AI 应用范例，包含用户系统、数据持久化、异步处理：

用户提交训练偏好后，AI 生成科学的个性化周训练计划：
- 支持三级训练水平（新手/中级/进阶）
- 支持三种健身目标（增肌/减脂/保持健康）
- 可指定重点训练部位与可用器械
- 异步 AI 生成，前端可轮询生成状态
- 自动为每个动作生成 B 站教学视频链接
- 所有计划持久化存储，支持随时查看与重新生成

### 4. MCP 文件系统集成

遵循 [Model Context Protocol](https://modelcontextprotocol.io/) 标准，授权 AI 安全访问本地文件系统：
- AI 可以读取指定目录内的代码与文档
- 支持 AI 辅助代码分析、文档总结、问题排查
- 可配置允许访问的目录、超时参数
- 跨平台支持，自动适配 Windows/Linux/macOS

### 5. AI 低代码生成平台

基于 langgraph4j 工作流的 AI 自动生成完整前端应用：
- 用户用自然语言描述需求，AI 自动生成可运行的 Vue 3 + Bootstrap 代码
- 六阶段流水线工作流：意图检查 → 需求解析 → 代码生成 → 质量校验 → 项目构建 → 结果保存
- AI 配备文件工具，可以直接读写修改项目文件
- **SSE 实时进度推送**：前端实时查看每个阶段进度
- **权限分离**：预览需要认证（仅创建者可见），部署后公开访问
- 持久化对话记忆，支持多轮对话迭代改进代码
- 一键重新部署，支持下载源码

**工作流节点：**
1. **IntentCheckNode** - 检查请求是否为合法的代码生成需求
2. **ParseIntentNode** - 将自然语言需求解析为结构化的应用信息
3. **GenerateCodeNode** - AI 生成完整的项目代码
4. **ValidateCodeNode** - AI 进行代码质量检查与问题修复
5. **BuildNode** - 使用 npm 安装依赖并构建生产版本
6. **SaveGenerateRecordNode** - 保存生成结果，推送完成事件

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
| 低代码 | GET | `/api/v1/lowcode/apps` | 获取应用列表 |
| 低代码 | POST | `/api/v1/lowcode/apps` | 创建应用 |
| 低代码 | POST | `/api/v1/lowcode/apps/{appId}/generate` | 提交生成请求 |
| 低代码 | GET | `/api/v1/lowcode/apps/{appId}/tasks/{taskId}/stream` | SSE 监听生成进度 |
| 低代码 | POST | `/api/v1/lowcode/apps/deploy/{appId}` | 一键部署应用 |
| 低代码 | GET | `/api/v1/lowcode/apps/{appId}/generated-info` | 获取预览/部署地址 |

## 开发者约定

详见 [CLAUDE.md](./CLAUDE.md)

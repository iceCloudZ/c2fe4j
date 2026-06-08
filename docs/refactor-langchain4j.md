# c2fe4j 重构设计：基于 LangChain4j 的 C²FE Agent 模式库

> 作者：iceCloudZ
> 日期：2026-06-09
> 状态：规划中

---

## 1. 为什么要重构

### 1.1 现状

c2fe4j 当前是一个"全栈 AI 工具包"，包含 6 个模块、约 36 个源文件、1600+ 行自维护代码：

```
c2fe4j
├── c2fe4j-core            # 基础抽象：ChatMessage, LlmClient, Tool, AgentLoop...
├── c2fe4j-openai          # OpenAI 兼容客户端：重试、流式、限流、TTFT
├── c2fe4j-agent           # Agent 模式：DomainAgent, RouterAgent, ReAct, SemanticRouter
├── c2fe4j-observability   # 可观测性：Trace, Span, TokenUsage
├── c2fe4j-spring-boot-starter  # Spring Boot 自动装配
└── pom.xml
```

使用方：LifeOps api-java、baoxian。

### 1.2 核心问题：大量代码在做别人已经做好的事

| c2fe4j 模块 | 代码行数 | LangChain4j 对应能力 | 重复度 |
|---|---|---|---|
| ChatMessage / ChatResponse / LlmClient | ~150 行 | `ChatMessage` / `AiMessage` / `ChatLanguageModel` | 95% |
| ToolDefinition / ToolCall / ToolResult | ~80 行 | `ToolSpecification` / `ToolExecutionRequest` | 90% |
| ToolRegistry / ToolGateway | ~130 行 | `@Tool` 注解 + 自动注册 | 80% |
| OpenAiLlmClient（重试/流式/限流/TTFT） | ~400 行 | `OpenAiChatModel` / `OpenAiStreamingChatModel` | 95% |
| StreamingChatClient | ~90 行 | `OpenAiStreamingChatModel` | 95% |
| CostCalculator | ~80 行 | LangChain4j 内置 token 追踪 | 70% |
| ReActAgentLoop | ~120 行 | LangChain4j 有 ReAct 实现 | 60% |

**合计约 1050 行是重复造轮子**。这些代码需要持续维护——新模型适配、API 变更、流式协议更新、重试策略调优……而我们真正关心的不是管道工活，是 Agent 模式。

### 1.3 c2fe4j 真正独特的价值

以下组件是 LangChain4j **没有的**，体现了 C²FE 哲学：

| 组件 | 代码行数 | 为什么独特 |
|---|---|---|
| **DomainAgent** 接口 | ~10 行 | 每个领域 Agent 自带 `retrieveContext()`，是 C²FE P2 双网关设计 |
| **RouterAgent** 多领域路由 | ~100 行 | LLM 分析问题 → 选择领域 Agent → 聚合上下文回答 |
| **SemanticRouter** | ~100 行 | 基于嵌入的余弦相似度路由，带置信度阈值和歧义检测 |
| **MemoryGateway** 三层记忆 | ~50 行 | 语义/情景/程序性分层，不是简单的窗口裁剪 |
| **OrchestrationStrategy** | ~40 行 | 策略接口，ReAct vs PlanExecute 动态选择 |
| **DecisionGate** 认知门控 | ~30 行 | C²FE P1 认知带宽，判断是否需要检索/升级模型 |
| **Observability** 体系 | ~200 行 | Trace/Span/TokenUsage 完整链路追踪，LangChain4j 这块较弱 |

**这些才是 c2fe4j 的知识产权，约 530 行。**

### 1.4 结论

> c2fe4j 应该从"全栈 AI 工具包"重构为 **"C²FE Agent 模式库"**。
>
> 底层管道（LLM 调用、工具注册、对话管理）委托给 LangChain4j；
> 上层模式（领域路由、三层记忆、认知门控、可观测性）保留在 c2fe4j。

这样做的好处：

- **维护负担减半**：不再需要维护 LLM 客户端、流式解析、重试策略
- **Provider 自动扩展**：LangChain4j 支持 30+ Provider，不限于 OpenAI 兼容
- **DX 提升**：用 LangChain4j 的 `@Tool` 注解替代手写 Tool 接口
- **专注核心竞争力**：把精力花在让 Agent 更聪明的上层模式上

---

## 2. 重构后的架构

```
┌─────────────────────────────────────────────┐
│  LifeOps / baoxian 业务代码                   │
├─────────────────────────────────────────────┤
│  c2fe4j-agent                               │
│  ├── DomainAgent        领域 Agent 接口       │
│  ├── RouterAgent        多领域路由 Agent      │
│  ├── SemanticRouter     嵌入式语义路由         │
│  ├── MemoryGateway      三层记忆接口          │
│  ├── OrchestrationStrategy  编排策略接口       │
│  └── DecisionGate       认知门控              │
├─────────────────────────────────────────────┤
│  c2fe4j-observability                        │
│  └── Trace / Span / TokenUsage / TraceReporter │
├─────────────────────────────────────────────┤
│  c2fe4j-spring-boot-starter                  │
│  └── 自动装配 LangChain4j + c2fe4j beans     │
├─────────────────────────────────────────────┤
│  LangChain4j (依赖，不修改)                    │
│  ├── langchain4j-core       基础类型          │
│  └── langchain4j-open-ai    OpenAI 兼容客户端  │
└─────────────────────────────────────────────┘
```

### 模块变化对照

| 重构前 | 重构后 | 说明 |
|---|---|---|
| c2fe4j-core (16 文件) | **删除** | 用 langchain4j-core 替代 |
| c2fe4j-openai (3 文件) | **删除** | 用 langchain4j-open-ai 替代 |
| c2fe4j-agent (10 文件) | **保留并重构** | 内部类型改为 LangChain4j |
| c2fe4j-observability (5 文件) | **保留** | 简单适配 LangChain4j TokenUsage |
| c2fe4j-spring-boot-starter (2 文件) | **保留并重构** | 装配 LangChain4j + c2fe4j |

---

## 3. 类型映射

| c2fe4j 类型 | LangChain4j 类型 | 迁移方式 |
|---|---|---|
| `ChatMessage` (record, Role enum) | `dev.langchain4j.data.message.*` | `SystemMessage` / `UserMessage` / `AiMessage` |
| `ChatResponse` (record) | `Response<AiMessage>` | 直接替换 |
| `ChatResponse.TokenUsage` | `dev.langchain4j.model.output.TokenUsage` | 直接替换 |
| `LlmClient` (interface) | `ChatLanguageModel` | 直接替换 |
| `ToolDefinition` | `ToolSpecification` | 直接替换 |
| `ToolCall` | `ToolExecutionRequest` | 直接替换 |
| `ToolResult` | `ToolExecutionResultMessage` | 直接替换 |
| `Tool` (interface) | LangChain4j `@Tool` | 保留 c2fe4j Tool 接口做适配 |
| `ToolRegistry` | 内置工具注册 | 保留，含 session tracking |
| `AgentContext` | 无对应 | 保留 c2fe4j 实现，改内部类型 |
| `AgentLoop` / `ReActAgentLoop` | 有 ReAct 实现 | 保留接口，实现改用 ChatLanguageModel |
| `DecisionGate` | 无对应 | 保留 |
| `RouterAgent` | 无对应 | 保留 |
| `SemanticRouter` | 无对应 | 保留 |
| `MemoryGateway` | `ChatMemory` 是窗口裁剪 | 保留，语义不同 |

---

## 4. 保留组件详细设计

### 4.1 DomainAgent

接口签名微调，`retrieveContext` 返回 LangChain4j 消息类型：

```java
public interface DomainAgent {
    String domain();
    String systemPrompt();
    // 返回 LangChain4j ChatMessage（上下文注入）
    List<dev.langchain4j.data.message.ChatMessage> retrieveContext(String query);
}
```

### 4.2 RouterAgent

内部从 `LlmClient` 改为 `ChatLanguageModel`：

```java
public class RouterAgent {
    private final ChatLanguageModel chatModel;  // 替代 LlmClient
    private final List<DomainAgent> domainAgents;
    private final Observability observability;
    // ...
}
```

### 4.3 SemanticRouter

不依赖 c2fe4j-core，**完全不变**。

### 4.4 ToolRegistry

保留 `Tool` 接口作为 c2fe4j 适配层（LangChain4j 的 `@Tool` 注解不支持 session call tracking）：

```java
public class ToolRegistry {
    // 新增：转换为 LangChain4j ToolSpecification
    public List<ToolSpecification> toLc4jToolSpecifications() { ... }
    // 保留：session call tracking 是 c2fe4j 独有功能
}
```

### 4.5 Observability

`obs.TokenUsage` 改为包装 LangChain4j 的 `TokenUsage`，Trace/Span/TraceReporter 不变。

---

## 5. 实施步骤

### Step 1: 修改父 POM
- 移除 `c2fe4j-core`、`c2fe4j-openai` 模块
- 添加 LangChain4j BOM 依赖管理
- 剩余 3 个模块：`c2fe4j-agent`、`c2fe4j-observability`、`c2fe4j-spring-boot-starter`

### Step 2: 重构 c2fe4j-observability
- `obs.TokenUsage` 适配 LangChain4j 的 `TokenUsage`
- 确保不依赖已删除模块的类型

### Step 3: 重构 c2fe4j-agent
- 所有 `c2fe4j.core.ChatMessage` → LangChain4j 消息类型
- `LlmClient` → `ChatLanguageModel`
- `ToolDefinition/ToolCall/ToolResult` → LangChain4j 对应类型
- `ReActAgentLoop`、`RouterAgent` 改用 `ChatLanguageModel`
- 保留：DomainAgent, SemanticRouter, MemoryGateway, OrchestrationStrategy, DecisionGate
- 删除：`OpenAiToolCallParser`（LangChain4j 有原生 tool call 解析）

### Step 4: 重构 c2fe4j-spring-boot-starter
- 自动装配 LangChain4j `OpenAiChatModel`
- 保留 c2fe4j beans：RouterAgent, ToolRegistry, Observability
- Properties 透传 LangChain4j 配置

### Step 5: 更新 LifeOps api-java
- 删除 `AppConfig` 中手动创建 `OpenAiLlmClient` 的代码
- DomainAgent 实现改用 LangChain4j 类型
- 依赖 starter 自动装配

### Step 6: 更新 baoxian
- 更新 c2fe4j 依赖版本
- 确认 coexist 模式正常

---

## 6. 重构前后对比

| 指标 | 重构前 | 重构后 |
|---|---|---|
| 模块数 | 6 | 3 |
| 源文件数 | ~36 | ~18 |
| 自维护代码行数 | ~1600 | ~550 |
| LLM Provider 支持 | 仅 OpenAI 兼容 | LangChain4j 支持的 30+ Provider |
| 维护负担 | 高 | 低 |
| 核心价值密度 | ~33%（530/1600） | ~100%（全部是独特模式） |

---

## 7. 风险与缓解

| 风险 | 缓解 |
|---|---|
| LangChain4j 版本不稳定 | 用最新稳定版 + BOM 管理版本 |
| 类型不兼容连锁改动 | 逐模块重构，每步编译验证 |
| 不支持某些中国模型 | DeepSeek/通义/智谱都是 OpenAI 兼容，用 `baseUrl` 配置 |
| ToolRegistry 功能丢失 | 保留在 c2fe4j-agent 中 |
| 重构期间项目不可用 | 在分支上重构，完成后一次性切换 |

---

## 8. 验证清单

- [ ] `mvnw clean compile` 所有模块编译通过
- [ ] `mvnw install -DskipTests` 安装到本地仓库
- [ ] LifeOps api-java 启动成功
- [ ] `curl /api/members` 返回正确数据
- [ ] AI Chat 对话正常（RouterAgent 通过 LangChain4j ChatModel 调用 LLM）
- [ ] baoxian 编译通过（c2fe4j 与现有代码共存）

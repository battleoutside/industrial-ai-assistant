# Industrial AI Assistant

本项目包含智能辅助报价、工程设计指导和行业信息咨询三个功能模块。

## 首次运行前：构建工程指导知识库

工程设计指导模块使用本地持久化向量文件：

```text
data/design-guidance-embedding-store.json
```

该文件不会随源码自动生成。首次拉取项目，或
`src/main/resources/knowledge/design-guidance/` 中的知识文档发生更新时，
需要执行一次知识入库。

### 1. 配置模型密钥

复制根目录的 `.env.example` 为 `.env`，并填写模型和 MCP 配置：

```properties
DASHSCOPE_API_KEY=your_dashscope_api_key
BIGMODEL_API_KEY=your_bigmodel_api_key
```

### 2. 执行一次性入库测试

在项目根目录执行：

```bash
mvn -Pintegration-tests -Dtest=DesignGuidanceKnowledgeBaseIngestorTest test
```

该命令会读取以下目录中的知识文档：

```text
src/main/resources/knowledge/design-guidance/
```

随后调用 `text-embedding-v2` 生成向量，并写入：

```text
data/design-guidance-embedding-store.json
```

### 3. 启动前检查

确认向量文件已经生成后再启动后端。若文件不存在，应用会创建空向量库，
工程设计指导模块将因检索不到有效证据而返回无相关证据。

### 4. 更新知识库

一次性入库测试带有防重复生成校验。需要重建时，请先备份并移走现有向量文件，
再重新执行上述命令。重建过程会调用 Embedding 模型并产生 Token 消耗。

package com.cetus.industrialai.designguidance.knowledge;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 定位：“进货员”，负责把新货运进来并摆上货架（序列化到文件）
 * <p>
 * 本地持久化向量库的“离线构建器”，负责“把外部知识灌进数据库”这个动作。
 * <p>
 * 这是一个离线脚本/管理接口，负责请求LLM构建离线向量库（调用 text-embedding-v[x]）。
 * 一般只在知识库内容更新时跑一次，生成好向量文件后，应用重启加载即可，不参与线上实时请求。
 **/
@Component
public class GuidanceKnowledgeIngestor {

    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    @Resource
    private InMemoryEmbeddingStore<TextSegment> inMemoryEmbeddingStore;

    private static final Path EMBEDDING_STORE_PATH =
            Path.of("data", "design-guidance-embedding-store.json");

    /**
     * 单独建立工程指导知识库目录+文件。
     */
    private static final String KNOWLEDGE_BASE_PATH =
            "src/main/resources/knowledge/design-guidance";

    // ------ RAG ------
    public void ingest() {
        System.out.println("开始构建知识库...");
        // 1. 加载文档
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(KNOWLEDGE_BASE_PATH);
        System.out.println("文档数量:" + documents.size());
        // 2. 文档切割：将每个文档按每段进行分割，最大 1000 字符，每次重叠最多 200 个字符
        DocumentByParagraphSplitter paragraphSplitter =
                new DocumentByParagraphSplitter(1000, 200);
        // 3. 自定义文档加载器
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(paragraphSplitter)
                // 为了提高搜索质量，为每个 TextSegment 添加文档名称
                .textSegmentTransformer(textSegment -> TextSegment.from(
                        textSegment.metadata().getString("file_name") + "\n" + textSegment.text(),
                        textSegment.metadata()
                ))
                // 使用指定的向量模型
                .embeddingModel(qwenEmbeddingModel)
                .embeddingStore(inMemoryEmbeddingStore)
                .build();
        // 调用 Embedding 模型，将文档写入内存向量库
        ingestor.ingest(documents);
        // 创建 data 目录，并将向量库保存到本地文件
        try {
            Files.createDirectories(EMBEDDING_STORE_PATH.getParent());
            inMemoryEmbeddingStore.serializeToFile(EMBEDDING_STORE_PATH);
            System.out.println("向量库已保存：" + EMBEDDING_STORE_PATH);
        } catch (IOException e) {
            throw new IllegalStateException("创建向量库目录失败", e);
        }
    }

}

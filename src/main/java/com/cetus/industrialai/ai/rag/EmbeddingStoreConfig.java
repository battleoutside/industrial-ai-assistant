package com.cetus.industrialai.ai.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * description: 创建并加载本地持久化的向量库
 */
@Configuration
public class EmbeddingStoreConfig {

    private static final Path EMBEDDING_STORE_PATH =
            Path.of("data", "embedding-store.json");

    @Bean
    public InMemoryEmbeddingStore<TextSegment> embeddingStore() {

        if (Files.exists(EMBEDDING_STORE_PATH)) {
            System.out.println("加载已有向量库：" + EMBEDDING_STORE_PATH);
            return InMemoryEmbeddingStore.fromFile(EMBEDDING_STORE_PATH);
        }

        System.out.println("未找到向量库文件，创建空向量库");
        return new InMemoryEmbeddingStore<>();
    }
}
package com.cetus.industrialai.designguidance.knowledge;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 定位：相当于一个“仓库管理员”，负责把仓库门打开（加载文件），并把货架摆好（填充内存）
 * <p>
 * 本地持久化向量库的“启动加载器”，负责“在 Spring 启动时，把硬盘上的向量库文件搬进内存，准备好工作台”
 * <p>
 * 保证后续检索时内存里有数据可用，且离线持久化，
 * 避免每次重启都要重新调用Token开销大的 Embedding 接口。
 */
@Configuration
public class GuidanceKnowledgeStoreConfig {

    private static final Path EMBEDDING_STORE_PATH =
            Path.of("data", "design-guidance-embedding-store.json");

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

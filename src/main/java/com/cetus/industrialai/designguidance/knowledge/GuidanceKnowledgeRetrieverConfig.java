package com.cetus.industrialai.designguidance.knowledge;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 定位：“导购员”，依赖货架（内存向量库）上的货，快速帮用户找出最匹配的商品
 * <p>
 * 本地持久化向量库的“检索过滤器”，负责“根据用户的问题，从已经建好的向量库中快速捞出最相关的知识片段”
 * <p>
 * 复用千问Embedding模型查询本地工程知识向量库，只保留相似度达到阈值的前5个片段。
 * 这是真正线上高频率调用的组件，只做纯向量计算（不读写硬盘），保证响应速度。
 */
@Configuration
public class GuidanceKnowledgeRetrieverConfig {

    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Bean
    public ContentRetriever contentRetriever() {
        // 在线请求只执行查询Embedding，不会重复构建整个知识库。
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(qwenEmbeddingModel)
                .maxResults(5) // 最多 5 个检索结果
                .minScore(0.75) // 过滤掉分数小于 0.75 的结果
                .build();
    }

}
